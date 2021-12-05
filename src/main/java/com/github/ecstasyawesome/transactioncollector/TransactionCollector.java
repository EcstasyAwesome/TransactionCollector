package com.github.ecstasyawesome.transactioncollector;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TransactionCollector extends Application {

  private final ResourceBundle languageBundle = ResourceContainer.LANGUAGE_BUNDLE;
  private final String appTitle = languageBundle.getString("app.title");
  private final String fileTypes = languageBundle.getString("app.types");
  private final String processLabel = languageBundle.getString("app.process");
  private final String dateTitle = languageBundle.getString("app.dateTitle");
  private final String dateLabel = languageBundle.getString("app.dateLabel");
  private final String transactionTitle = languageBundle.getString("app.sumTitle");
  private final String transactionLabel = languageBundle.getString("app.sumLabel");

  @Override
  public void start(Stage stage) {
    var files = showFileChooser(stage);
    var dateColumnName = showDialog(dateTitle, dateLabel);
    var sumColumnName = showDialog(transactionTitle, transactionLabel);
    showProgressBar(stage);
    launchCollector(stage, files, dateColumnName, sumColumnName);
  }

  private void launchCollector(Stage stage, List<File> files, String dateColumn, String sumColumn) {
    var collector = new Collector(files, dateColumn, sumColumn);
    collector.setOnSucceeded(event -> {
      stage.close();
      showAlert(collector.getValue());
    });

    new Thread(collector).start();
  }

  private List<File> showFileChooser(Stage stage) {
    var fileChooser = new FileChooser();
    var filter = new FileChooser.ExtensionFilter(fileTypes, SupportedTypes.getPatterns());
    fileChooser.getExtensionFilters().add(filter);
    var files = fileChooser.showOpenMultipleDialog(stage);
    if (files == null) {
      shutdown();
    }
    return files;
  }

  private String showDialog(String title, String label) {
    final var dialog = new TextInputDialog();
    dialog.setTitle(title);
    dialog.setHeaderText(null);
    dialog.setContentText(label);
    var result = dialog.showAndWait();
    if (result.isEmpty()) {
      shutdown();
    }
    return result.orElseThrow();
  }

  private void showProgressBar(Stage stage) {
    var root = new VBox();
    root.setAlignment(Pos.CENTER);
    root.setPadding(new Insets(10));
    root.setSpacing(10);

    var progressBar = new ProgressBar();
    progressBar.setPrefSize(200, 30);

    var label = new Label(processLabel);
    root.getChildren().addAll(label, progressBar);

    var scene = new Scene(root);

    stage.setOnCloseRequest(event -> shutdown());
    stage.setTitle(appTitle);
    stage.setScene(scene);
    stage.setResizable(false);
    stage.show();
  }

  private void showAlert(CollectorEvent collectorEvent) {
    var alert = new Alert(collectorEvent.getType());
    alert.setTitle(appTitle);
    alert.setHeaderText(null);
    alert.setContentText(collectorEvent.getMessage());
    alert.show();
  }

  private void shutdown() {
    Platform.exit();
    System.exit(0);
  }
}
