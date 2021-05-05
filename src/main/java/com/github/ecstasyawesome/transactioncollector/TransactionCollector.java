package com.github.ecstasyawesome.transactioncollector;

import java.io.File;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TransactionCollector extends Application {

  @Override
  public void start(Stage stage) {
    var fileChooser = new FileChooser();
    var filter = new FileChooser.ExtensionFilter(
        ResourceContainer.LANGUAGE_BUNDLE.getString("app.types"),
        SupportedTypes.getPatterns());
    fileChooser.getExtensionFilters().add(filter);
    var files = fileChooser.showOpenMultipleDialog(stage);
    if (files != null) {
      var root = new VBox();
      root.setAlignment(Pos.CENTER);
      root.setPadding(new Insets(10));
      root.setSpacing(10);

      var progressBar = new ProgressBar();
      progressBar.setPrefSize(200, 30);

      var label = new Label(ResourceContainer.LANGUAGE_BUNDLE.getString("app.process"));
      root.getChildren().addAll(label, progressBar);

      var scene = new Scene(root);

      stage.setOnCloseRequest(event -> Platform.exit());
      stage.setTitle(ResourceContainer.LANGUAGE_BUNDLE.getString("app.title"));
      stage.setScene(scene);
      stage.setResizable(false);
      stage.show();

      launchCollector(stage, files);
    } else {
      Platform.exit();
    }
  }

  private void launchCollector(Stage stage, List<File> files) {
    var collector = new Collector(files);
    collector.setOnSucceeded(event -> {
      stage.close();
      var collectorEvent = collector.getValue();
      var alert = new Alert(collectorEvent.getType());
      alert.setTitle(ResourceContainer.LANGUAGE_BUNDLE.getString("app.title"));
      alert.setHeaderText(null);
      alert.setContentText(collectorEvent.getMessage());
      alert.show();
    });

    new Thread(collector).start();
  }
}