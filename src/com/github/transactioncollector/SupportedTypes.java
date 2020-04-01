package com.github.transactioncollector;

import java.util.stream.Stream;

public enum SupportedTypes {
    XLSX("xlsx"),
    ZIP("zip");

    private String type;

    SupportedTypes(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getSuffix() {
        return "." + type;
    }

    public static String[] getTypes() {
        return Stream.of(SupportedTypes.values()).map(SupportedTypes::getType).toArray(String[]::new);
    }
}