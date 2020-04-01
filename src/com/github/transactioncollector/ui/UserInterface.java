package com.github.transactioncollector.ui;

import java.io.File;

public interface UserInterface {

    File[] fileChooser();

    void showBar();

    void closeBar(String message);
}