package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.infrastructure.ports.VisualisationProvider;
import tools.jackson.databind.JsonNode;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SupersetAdapter implements VisualisationProvider {

    private static final Logger logger = LoggerFactory.getLogger(SupersetAdapter.class);
    private final JdbcTemplate jdbcTemplate;
    private RestClient restClient;
    private String accessToken;

    private String username;
    private String password;

    public SupersetAdapter(
        RestClient.Builder builder,
        JdbcTemplate jdbcTemplate,
        String baseUrl,
        String username,
        String password
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.username = username;
        this.password = password;

        logger.info("SupersetAdapter initialized on Virtual Threads with base URL: {}", baseUrl);
    }

    private String authenticate() {
        logger.debug("Authenticating Superset user: {}", username);
        
        JsonNode res = restClient.post()
                .uri("/api/v1/security/login")
                .body(Map.of("username", username, "password", password, "provider", "db"))
                .retrieve()
                .body(JsonNode.class);

        if (res == null || res.path("access_token").isMissingNode()) {
            throw new RuntimeException("Superset auth failed: No token in response");
        }
        
        this.accessToken = res.get("access_token").asString("");
        return this.accessToken;
    }

    private Boolean internalOverwriteTable(String tableName, List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            logger.warn("No data for table {}", tableName);
            return true;
        }

        jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName);
        
        Map<String, Object> firstRow = data.get(0);
        String columns = firstRow.keySet().stream()
                .map(k -> k + " TEXT")
                .collect(Collectors.joining(", "));
        
        jdbcTemplate.execute("CREATE TABLE " + tableName + " (" + columns + ")");

        String placeholders = firstRow.keySet().stream().map(k -> "?").collect(Collectors.joining(","));
        String sql = "INSERT INTO " + tableName + " VALUES (" + placeholders + ")";
        
        List<Object[]> batchArgs = data.stream()
                .map(row -> row.values().toArray())
                .collect(Collectors.toList());
        
        jdbcTemplate.batchUpdate(sql, batchArgs);
        return true;
    }

    private Boolean internalRefresh(Integer datasetId) {
        if (accessToken == null) authenticate();
        
        restClient.put()
                .uri("/api/v1/dataset/{id}/refresh", datasetId)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .toBodilessEntity(); // Pure synchronous wait
        
        return true;
    }

    private Integer internalCreateChart(Integer datasetId, String name, String type, String params) {
        if (accessToken == null) authenticate();
        
        JsonNode res = restClient.post()
                .uri("/api/v1/chart/")
                .headers(h -> h.setBearerAuth(accessToken))
                .body(Map.of(
                    "slice_name", name,
                    "viz_type", type,
                    "datasource_id", datasetId,
                    "datasource_type", "table",
                    "params", params
                ))
                .retrieve()
                .body(JsonNode.class);
        
        int chartId = (res != null) ? res.path("id").asInt(-1) : -1;
        if (chartId == -1) throw new RuntimeException("Chart creation failed");

        return chartId;
    }

    @Override
    public Mono<Try<Boolean>> overwritePhysicalTable(String tableName, List<Map<String, Object>> data) {
        return SafeRunner.futureSafe(() -> internalOverwriteTable(tableName, data));
    }

    @Override
    public Mono<Try<Boolean>> refreshDataset(Integer datasetId) {
        return SafeRunner.futureSafe(() -> internalRefresh(datasetId));
    }

    @Override
    public Mono<Try<Integer>> createChart(Integer datasetId, String chartName, String vizType, String paramsJson) {
        return SafeRunner.futureSafe(() -> internalCreateChart(datasetId, chartName, vizType, paramsJson));
    }
}
