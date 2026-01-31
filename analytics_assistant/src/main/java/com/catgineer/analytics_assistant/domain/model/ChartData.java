package com.catgineer.analytics_assistant.domain.model;

public class ChartData {
    private String label;
    private double value;

    public ChartData() {
    }

    public ChartData(String label, double value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ChartData{" +
               "label='" + label + "'" +
               ", value=" + value +
               '}';
    }
}
