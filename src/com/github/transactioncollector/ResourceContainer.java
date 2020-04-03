package com.github.transactioncollector;

import java.util.ResourceBundle;

public class ResourceContainer {

    public static final ResourceBundle LANGUAGE_BUNDLE = ResourceBundle.getBundle("locale", new CustomResourceBundleControl());

}
