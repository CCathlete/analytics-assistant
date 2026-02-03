package com.catgineer.analytics_assistant.infrastructure.adapters;

import com.catgineer.analytics_assistant.domain.SafeRunner;
import com.catgineer.analytics_assistant.infrastructure.ports.VisualisationProvider;
import tools.jackson.databind.JsonNode;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SupersetAdapter implements VisualisationProvider {

    private static final Logger logger = LoggerFactory.getLogger(SupersetAdapter.class);
    private final JdbcTemplate jdbcTemplate;

    private String targetDatasetId;
    private String targetTableName;

    private RestClient restClient;
    private String accessToken;

    private String username;
    private String password;

    public SupersetAdapter(
        RestClient.Builder builder,
        JdbcTemplate jdbcTemplate,
        String baseUrl,
        String username,
        String password,
        String targetTableName,
        String targetDatasetId
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.username = username;
        this.password = password;
        this.targetTableName = targetTableName;
        this.targetDatasetId = targetDatasetId;

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

    private Boolean internalOverwriteTable(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            logger.warn("No data for table {}", targetTableName);
            return true;
        }

        jdbcTemplate.execute("DROP TABLE IF EXISTS " + targetTableName);
        logger.info("Table {} dropped successfully", targetTableName);

        final Map<String, Object> firstRow = data.get(0);
        final List<String> stableKeys = List.copyOf(firstRow.keySet());

        String columnsDefinition = stableKeys.stream()
                .map(k -> k + " " + inferPostgresType(String.valueOf(firstRow.get(k))))
                .collect(Collectors.joining(", "));
        logger.info("Columns definition for table creation: {}", columnsDefinition);

        jdbcTemplate.execute("CREATE TABLE " + targetTableName + " (" + columnsDefinition + ")");
        logger.info("Table {} created successfully", targetTableName);

        String columnNames = String.join(", ", stableKeys);
        logger.info("Column names for insertion: {}", columnNames);

        String placeholders = stableKeys.stream().map(k -> "?").collect(Collectors.joining(","));
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", targetTableName, columnNames, placeholders);
        logger.info("Insertion query: {}", sql);

        // Map values strictly following the stable keys to prevent alignment issues
        List<Object[]> batchArgs = data.stream()
                .map(row -> stableKeys.stream()
                        .map(row::get)
                        .toArray(Object[]::new))
                .toList();

        logger.info("Data for insertion: {}", batchArgs.stream().map(java.util.Arrays::toString).toList());

        jdbcTemplate.batchUpdate(sql, batchArgs);
        return true;
    }

    private String inferPostgresType(String value) {
        if (value == null || value.equalsIgnoreCase("null")) return "TEXT";
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) return "BOOLEAN";
        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) return "DATE";
        if (value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")) return "TIMESTAMP";
        if (value.matches("-?\\d+(\\.\\d+)?")) return "NUMERIC";
        return "TEXT";
    }

    private Boolean internalRefresh() {
        if (accessToken == null) authenticate();
        restClient.put()
                .uri("/api/v1/dataset/{id}/refresh", targetDatasetId)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                    (req, res) ->
                        logger.error("Metadata sync failed for dataset {}", targetDatasetId)) 
                .toBodilessEntity();
        return true;
    }


    private Integer internalCreateChart(Integer datasetId, String name, String type, String paramsJson) {
        if (accessToken == null) authenticate();
        
        JsonNode res = restClient.post()
                .uri("/api/v1/chart/")
                .headers(h -> h.setBearerAuth(accessToken))
                .body(Map.of(
                    "slice_name", name,
                    "viz_type", type,
                    "datasource_id", datasetId,
                    "datasource_type", "table",
                    "params", paramsJson
                ))
                .retrieve()
                .body(JsonNode.class);
        
        int chartId = (res != null) ? res.path("id").asInt(-1) : -1;
        if (chartId == -1) throw new RuntimeException("Chart creation failed");

        return chartId;
    }

    @Override
    public Mono<Try<Boolean>> overwritePhysicalTable(String tableName, List<Map<String, Object>> data) {
        // TODO: Fix signature.
        // We now ignore the tableName parameter in favor of the injected targetTableName
        return SafeRunner.futureSafe(() -> internalOverwriteTable(data));
    }

    @Override
    public Mono<Try<Boolean>> refreshDataset(Integer datasetId) {
        // TODO: Fix signature.
        // We ignore the datasetId parameter in favor of the injected targetDatasetId
        return SafeRunner.futureSafe(this::internalRefresh);
    }

    @Override
    public Mono<Try<Integer>> createChart(Integer datasetId, String chartName, String vizType, String paramsJson) {
        return SafeRunner.futureSafe(() -> internalCreateChart(datasetId, chartName, vizType, paramsJson));
    }
}
