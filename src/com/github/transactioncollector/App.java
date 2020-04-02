package com.github.transactioncollector;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class App extends Application {

    private String title = "Collector";

    public static void main(String[] args) {
        App.launch(args);
    }

    @Override
    public void start(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Supported types", SupportedTypes.getPatterns());
        fileChooser.getExtensionFilters().add(filter);
        List<File> files = fileChooser.showOpenMultipleDialog(stage);
        if (Objects.nonNull(files)) {
            VBox root = new VBox();
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(10));
            root.setSpacing(10);
            ProgressBar progressBar = new ProgressBar();
            progressBar.setPrefSize(200, 30);
            Label label = new Label("Your data is being processed");
            root.getChildren().addAll(label, progressBar);
            Scene scene = new Scene(root);
            stage.setOnCloseRequest(event -> System.exit(0));
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
            launchCollector(stage, files);
        } else Platform.exit();
    }

    private void launchCollector(Stage stage, List<File> files) {
        Task<Collector.CollectorEvent> task = new Task<Collector.CollectorEvent>() {
            @Override
            public Collector.CollectorEvent call() {
                return new Collector().processFiles(files);
            }
        };
        task.setOnSucceeded(event -> {
            stage.close();
            Collector.CollectorEvent collectorEvent = task.getValue();
            Alert alert = new Alert(collectorEvent.getType());
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(collectorEvent.getMessage());
            alert.showAndWait();
        });
        new Thread(task).start();
    }
}