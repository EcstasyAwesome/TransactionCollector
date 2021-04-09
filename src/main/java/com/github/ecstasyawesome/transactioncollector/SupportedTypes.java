package com.github.ecstasyawesome.transactioncollector;

import java.util.stream.Stream;

public enum SupportedTypes {
  XLSX("xlsx"),
  ZIP("zip");

  public final String pattern;
  public final String extension;

  SupportedTypes(String type) {
    pattern = String.format("*.%s", type);
    extension = String.format(".%s", type);
  }

  public static String[] getPatterns() {
    return Stream.of(SupportedTypes.values()).map(type -> type.pattern).toArray(String[]::new);
  }
}