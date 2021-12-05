package com.github.ecstasyawesome.transactioncollector;

import java.util.ResourceBundle;

public class ResourceContainer {

  public static final ResourceBundle LANGUAGE_BUNDLE;

  static {
    LANGUAGE_BUNDLE = ResourceBundle.getBundle("locale");
  }
}
