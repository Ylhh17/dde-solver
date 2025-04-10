package dde;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.List;

public class GraphWindow extends Application {

    public static List<Double> tData;
    public static List<Double> yData;

    @Override
    public void start(Stage stage) {
        stage.setTitle("График y(t)");

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("t");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("y(t)");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Решение DDE");

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("y(t)");

        for (int i = 0; i < tData.size(); i++) {
            series.getData().add(new XYChart.Data<>(tData.get(i), yData.get(i)));
        }

        lineChart.getData().add(series);

        StackPane root = new StackPane();
        root.getChildren().add(lineChart);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();
    }
}
