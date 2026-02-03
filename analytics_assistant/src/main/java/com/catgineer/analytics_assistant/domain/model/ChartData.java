package com.catgineer.analytics_assistant.domain.model;

import java.util.HashMap;
import java.util.Map;

public class ChartData {
    // Represents the actual data: e.g., {"week": "2026-02-03", "project": "Gemini", "commits": 10}
    private Map<String, Object> columns = new HashMap<>();
    
    // Metadata for Postgres types: e.g., {"week": "DATE", "commits": "NUMERIC"}
    private Map<String, String> columnTypes = new HashMap<>();

    public ChartData() {
    }

    public ChartData(Map<String, Object> columns, Map<String, String> columnTypes) {
        this.columns = columns != null ? columns : new HashMap<>();
        this.columnTypes = columnTypes != null ? columnTypes : new HashMap<>();
    }

    /**
     * Safe access to a value with a fallback default.
     */
    public Object getColumnValue(String columnName, Object defaultValue) {
        return columns.getOrDefault(columnName, defaultValue);
    }

    public Map<String, Object> getColumns() {
        return columns;
    }

    public void setColumns(Map<String, Object> columns) {
        this.columns = columns;
    }

    public Map<String, String> getColumnTypes() {
        return columnTypes;
    }

    public void setColumnTypes(Map<String, String> columnTypes) {
        this.columnTypes = columnTypes;
    }

    @Override
    public String toString() {
        return "ChartData{" +
                "columns=" + columns +
                ", columnTypes=" + columnTypes +
                '}';
    }
}
