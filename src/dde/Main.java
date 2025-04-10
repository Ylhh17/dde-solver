package dde;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Main extends Application {

    private double[] ts;
    private double[] ys;

    @Override
    public void start(Stage stage) {
        // Поля ввода параметров
        TextField aField = new TextField("1");
        TextField tauField = new TextField("1");
        TextField hField = new TextField("0.1");
        TextField tMaxField = new TextField("10");

        // Сетка с подписями
        GridPane inputGrid = new GridPane();
        inputGrid.setVgap(10);
        inputGrid.setHgap(10);
        inputGrid.setPadding(new Insets(10));
        inputGrid.add(new Label("a:"), 0, 0);
        inputGrid.add(aField, 1, 0);
        inputGrid.add(new Label("τ (tau):"), 0, 1);
        inputGrid.add(tauField, 1, 1);
        inputGrid.add(new Label("h (шаг):"), 0, 2);
        inputGrid.add(hField, 1, 2);
        inputGrid.add(new Label("t max:"), 0, 3);
        inputGrid.add(tMaxField, 1, 3);

        Button solveButton = new Button("Рассчитать");
        Button saveButton = new Button("Сохранить в CSV");

        // График
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("t");
        yAxis.setLabel("y(t)");
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Решение DDE");

        // Обработка кнопки "Рассчитать"
        solveButton.setOnAction(e -> {
            double a = Double.parseDouble(aField.getText());
            double tau = Double.parseDouble(tauField.getText());
            double h = Double.parseDouble(hField.getText());
            double tMax = Double.parseDouble(tMaxField.getText());

            ts = new double[(int)(tMax / h) + 1];
            ys = new double[ts.length];

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName("y(t)");

            for (int i = 0; i < ts.length; i++) {
                ts[i] = i * h;
                if (ts[i] <= 0) {
                    ys[i] = 1.0; // Функция истории φ(t) = 1
                } else {
                    double t_minus_tau = ts[i] - tau;
                    int index = (int)(t_minus_tau / h);
                    if (index >= 0) {
                        ys[i] = ys[i - 1] + h * (-a * ys[index]);
                    } else {
                        ys[i] = ys[i - 1] + h * (-a * 1.0); // φ(t - τ) = 1
                    }
                }
                series.getData().add(new XYChart.Data<>(ts[i], ys[i]));
            }

            lineChart.getData().clear();
            lineChart.getData().add(series);
        });

        // Обработка кнопки "Сохранить"
        saveButton.setOnAction(e -> {
            if (ts != null && ys != null) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Сохранить как CSV");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
                File file = fileChooser.showSaveDialog(stage);

                if (file != null) {
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write("t,y\n");
                        for (int i = 0; i < ts.length; i++) {
                            writer.write(ts[i] + "," + ys[i] + "\n");
                        }
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Готово");
                        alert.setHeaderText(null);
                        alert.setContentText("Файл успешно сохранён!");
                        alert.showAndWait();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Ошибка");
                        alert.setHeaderText(null);
                        alert.setContentText("Не удалось сохранить файл.");
                        alert.showAndWait();
                    }
                }
            }
        });

        VBox root = new VBox(10, inputGrid, solveButton, saveButton, lineChart);
        root.setPadding(new Insets(10));

        stage.setScene(new Scene(root, 700, 550));
        stage.setTitle("DDE Solver");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
