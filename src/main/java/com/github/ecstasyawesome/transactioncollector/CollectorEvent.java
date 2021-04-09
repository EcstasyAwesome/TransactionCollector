package com.github.ecstasyawesome.transactioncollector;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class CollectorEvent implements Event<AlertType> {

  private final Alert.AlertType type;
  private final String message;

  public CollectorEvent(Alert.AlertType type, String message) {
    this.type = type;
    this.message = message;
  }

  @Override
  public Alert.AlertType getType() {
    return type;
  }

  @Override
  public String getMessage() {
    return message;
  }
}