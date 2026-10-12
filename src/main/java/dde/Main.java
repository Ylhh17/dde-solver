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
import javafx.geometry.Pos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Главный класс
public class Main extends Application {

    private final ComboBox<String> methodCombo = new ComboBox<>(); // Комбобокс для выбора метода
    private final Map<String, XYChart.Series<Number, Number>> methodSeries = new HashMap<>(); // Словарь для хранения графиков разных методов (если понадобится)
    private double[] ts; // Массивы для хранения рассчитанных данных (используются для экспорта и таблицы)
    private double[] ys;

    @Override
    public void start(Stage stage) {

        System.out.println("JavaFX стартует!");

        // === Элементы ввода параметров DDE ===
        Label aLabel = new Label("a:");
        TextField aField = new TextField("");
        Label bLabel = new Label("b:");
        TextField bField = new TextField("");
        Label tauLabel = new Label("τ (tau):");
        TextField tauField = new TextField("");
        Label tau2Label = new Label("τ₂ (доп. задержка):");
        TextField tau2Field = new TextField("");
        Label hLabel = new Label("h (шаг):");
        TextField hField = new TextField("");
        Label tMaxLabel = new Label("t max:");
        TextField tMaxField = new TextField("");
        Label phiLabel = new Label("φ(t ≤ 0):");
        TextField phiField = new TextField(""); // начальные условия

        // === Поле для отображения текущего уравнения ===
        Label equationLabel = new Label();
        equationLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 0 10 0;");

        // === Поле для пояснения режима (меняется при выборе примера) ===
        Label descriptionLabel = new Label();
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 5 0;");

        // === Примеры уравнений (режимы) ===
        ComboBox<String> exampleCombo = new ComboBox<>();
        exampleCombo.getItems().addAll(
                "Классика: ẏ(t) = a·y(t-τ)",
                "Состояние: ẏ(t) = a·y(t-τ) + b·y(t)",
                "Две задержки: ẏ(t) = a·y(t-τ₁) + b·y(t-τ₂)"
        );
        exampleCombo.setValue("Классика: ẏ(t) = a·y(t-τ)"); // по умолчанию

        // === Размещение параметров в GridPane ===
        GridPane inputGrid = new GridPane();
        inputGrid.setVgap(10);
        inputGrid.setHgap(10);
        inputGrid.setPadding(new Insets(10));
        inputGrid.add(aLabel, 0, 0);
        inputGrid.add(aField, 1, 0);
        inputGrid.add(bLabel, 0, 1);
        inputGrid.add(bField, 1, 1);
        inputGrid.add(tauLabel, 0, 2);
        inputGrid.add(tauField, 1, 2);
        inputGrid.add(tau2Label, 0, 3);
        inputGrid.add(tau2Field, 1, 3);
        inputGrid.add(hLabel, 0, 4);
        inputGrid.add(hField, 1, 4);
        inputGrid.add(tMaxLabel, 0, 5);
        inputGrid.add(tMaxField, 1, 5);
        inputGrid.add(phiLabel, 0, 6);
        inputGrid.add(phiField, 1, 6);
        inputGrid.add(new Label("Пример:"), 2, 6);
        inputGrid.add(exampleCombo, 3, 6);

        // === Скрытие b/tau2 для классики ===
        bLabel.setVisible(false);
        bLabel.setManaged(false);
        bField.setVisible(false);
        bField.setManaged(false);
        tau2Label.setVisible(false);
        tau2Label.setManaged(false);
        tau2Field.setVisible(false);
        tau2Field.setManaged(false);

        // === Лямбды для обновления уравнения и пояснения ===
        Runnable updateEquation = () -> equationLabel.setText(
                "Уравнение: " + getEquationText(
                        exampleCombo.getValue(),
                        aField.getText(),
                        bField.getText(),
                        tauField.getText(),
                        tau2Field.getText()
                )
        );

        Runnable updateDescription = () -> {
            switch (exampleCombo.getValue()) {
                case "Классика: ẏ(t) = a·y(t-τ)" ->
                        descriptionLabel.setText("Классическая DDE: производная в текущий момент зависит только от значения функции в прошлом (на время задержки).");
                case "Состояние: ẏ(t) = a·y(t-τ) + b·y(t)" ->
                        descriptionLabel.setText("Производная зависит как от значения функции в прошлом, так и от её текущего значения. Это позволяет моделировать процессы с внутренней обратной связью.");
                case "Две задержки: ẏ(t) = a·y(t-τ₁) + b·y(t-τ₂)" ->
                        descriptionLabel.setText("В уравнении две задержки — производная зависит от функции в двух разных прошлых моментах времени. Это отражает влияние нескольких предыдущих состояний на эволюцию системы.");
                default -> descriptionLabel.setText("");
            }
        };

        // === Логика переключения режимов уравнения ===
        exampleCombo.setOnAction(e -> {
            switch (exampleCombo.getValue()) {
                case "Классика: ẏ(t) = a·y(t-τ)" -> {
                    aLabel.setVisible(true);
                    aLabel.setManaged(true);
                    aField.setVisible(true);
                    aField.setManaged(true);

                    bLabel.setVisible(false);
                    bLabel.setManaged(false);
                    bField.setVisible(false);
                    bField.setManaged(false);

                    tauLabel.setVisible(true);
                    tauLabel.setManaged(true);
                    tauField.setVisible(true);
                    tauField.setManaged(true);

                    tau2Label.setVisible(false);
                    tau2Label.setManaged(false);
                    tau2Field.setVisible(false);
                    tau2Field.setManaged(false);
                }
                case "Состояние: ẏ(t) = a·y(t-τ) + b·y(t)" -> {
                    aLabel.setVisible(true);
                    aLabel.setManaged(true);
                    aField.setVisible(true);
                    aField.setManaged(true);

                    bLabel.setVisible(true);
                    bLabel.setManaged(true);
                    bField.setVisible(true);
                    bField.setManaged(true);

                    tauLabel.setVisible(true);
                    tauLabel.setManaged(true);
                    tauField.setVisible(true);
                    tauField.setManaged(true);

                    tau2Label.setVisible(false);
                    tau2Label.setManaged(false);
                    tau2Field.setVisible(false);
                    tau2Field.setManaged(false);
                }
                case "Две задержки: ẏ(t) = a·y(t-τ₁) + b·y(t-τ₂)" -> {
                    aLabel.setVisible(true);
                    aLabel.setManaged(true);
                    aField.setVisible(true);
                    aField.setManaged(true);

                    bLabel.setVisible(true);
                    bLabel.setManaged(true);
                    bField.setVisible(true);
                    bField.setManaged(true);

                    tauLabel.setVisible(true);
                    tauLabel.setManaged(true);
                    tauField.setVisible(true);
                    tauField.setManaged(true);

                    tau2Label.setVisible(true);
                    tau2Label.setManaged(true);
                    tau2Field.setVisible(true);
                    tau2Field.setManaged(true);
                }
            }
            updateEquation.run();
            updateDescription.run();
        });
        updateEquation.run();

        // Автообновление уравнения при изменении параметров
        aField.textProperty().addListener((obs, oldV, newV) -> updateEquation.run());
        bField.textProperty().addListener((obs, oldV, newV) -> updateEquation.run());
        tauField.textProperty().addListener((obs, oldV, newV) -> updateEquation.run());
        tau2Field.textProperty().addListener((obs, oldV, newV) -> updateEquation.run());


        // === Выбор численного метода ===
        methodCombo.getItems().addAll("Метод Эйлера", "Метод Рунге-Кутты 4", "Метод Адамса-Бэшфорта");
        methodCombo.setValue("Метод Эйлера");
        inputGrid.add(new Label("Метод:"), 0, 7);
        inputGrid.add(methodCombo, 1, 7);
        // === Кнопки управления ===
        Button solveButton = new Button("Рассчитать");
        Button saveButton = new Button("Сохранить в CSV");

        // === График ===
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("t");
        yAxis.setLabel("y(t)");
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Решение DDE");

        // === Кнопка "Рассчитать" === // Обработчик
        solveButton.setOnAction(e -> {
            double a = Double.parseDouble(aField.getText()); // Чтение параметров
            double b = bField.isVisible() ? Double.parseDouble(bField.getText()) : 0;
            double tau = Double.parseDouble(tauField.getText());
            double tau2 = tau2Field.isVisible() ? Double.parseDouble(tau2Field.getText()) : 0;
            double h = Double.parseDouble(hField.getText());
            double tMax = Double.parseDouble(tMaxField.getText());
            // Выбор выражения для solver
            String expr = switch (exampleCombo.getValue()) {
                case "Классика: ẏ(t) = a·y(t-τ)" -> "a * y_tau1";
                case "Состояние: ẏ(t) = a·y(t-τ) + b·y(t)" -> "a * y_tau1 + b * y";
                case "Две задержки: ẏ(t) = a·y(t-τ₁) + b·y(t-τ₂)" -> "a * y_tau1 + b * y_tau2";
                default -> "a * y_tau1";
            };

            // Подставляем b в формулу, если он используется
            String exprReady = expr.replace("b", Double.toString(b));
            // Инициализация solver
            DDESolver solver = new DDESolver();
            solver.setTau2(tau2);
            solver.setInitialFunctionExpression(phiField.getText());
            solver.setExpression(exprReady);

            DDESolver.Method method = switch (methodCombo.getValue()) {
                case "Метод Рунге-Кутты 4" -> DDESolver.Method.RUNGE_KUTTA_4;
                case "Метод Адамса-Бэшфорта" -> DDESolver.Method.ADAMS_BASHFORTH_2;
                default -> DDESolver.Method.EULER;
            };

            // Решение
            solver.solve(method, a, tau, h, tMax);

            List<Double> tList = solver.getTList();
            List<Double> yList = solver.getYList();

            // Сохраняем значения для экспорта/таблицы
            ts = tList.stream().mapToDouble(Double::doubleValue).toArray();
            ys = yList.stream().mapToDouble(Double::doubleValue).toArray();

            // Обновляем график
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(methodCombo.getValue());
            for (int i = 0; i < tList.size(); i++) {
                series.getData().add(new XYChart.Data<>(tList.get(i), yList.get(i)));
            }

            lineChart.getData().clear();
            lineChart.getData().add(series);

            System.out.println("Выбран метод: " + method);
        });

        // === Кнопка справки ===
        Button helpButton = new Button("ℹ️ Справка о методах");
        helpButton.setTooltip(new Tooltip("Объяснение численных методов"));

        helpButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Справка о численных методах и режимах");
            alert.setHeaderText("Описание режимов и методов:");
            alert.setContentText("""
                    Режимы уравнения:
                    
                    Классика:
                    Классическая DDE: производная в текущий момент зависит только от значения функции в прошлом (на время задержки).
                    
                    Состояние:
                    Производная зависит как от значения функции в прошлом, так и от её текущего значения. Это позволяет моделировать процессы с внутренней обратной связью.
                    
                    Две задержки:
                    В уравнении две задержки — производная зависит от функции в двух разных прошлых моментах времени. Это отражает влияние нескольких предыдущих состояний на эволюцию системы.
                    
                    ----------------------------------------------------
                    Численные методы:
                    
                    Метод Эйлера: простой и быстрый, но может быть неточным.
                    Метод Рунге–Кутты 4 порядка: самый точный среди реализованных.
                    Метод Адамса–Бэшфорта 2 порядка: использует предсказание на основе предыдущих шагов.
                    
                    Вы можете сравнить их точность, используя кнопку 'Сравнить методы' или 'График ошибки'.
                    """);
            alert.showAndWait();
        });

        // === Кнопка "Сравнить методы" ===
        Button compareButton = new Button("Сравнить методы");

        compareButton.setOnAction(e -> { // Аналогично solveButton, только строит все методы сразу
            double a = Double.parseDouble(aField.getText());
            double b = bField.isVisible() ? Double.parseDouble(bField.getText()) : 0;
            double tau = Double.parseDouble(tauField.getText());
            double tau2 = tau2Field.isVisible() ? Double.parseDouble(tau2Field.getText()) : 0;
            double h = Double.parseDouble(hField.getText());
            double tMax = Double.parseDouble(tMaxField.getText());
            String phiExpr = phiField.getText();

            String expr = switch (exampleCombo.getValue()) {
                case "Классика: ẏ(t) = a·y(t-τ)" -> "a * y_tau1";
                case "Состояние: ẏ(t) = a·y(t-τ) + b·y(t)" -> "a * y_tau1 + b * y";
                case "Две задержки: ẏ(t) = a·y(t-τ₁) + b·y(t-τ₂)" -> "a * y_tau1 + b * y_tau2";
                default -> "a * y_tau1";
            };
            String exprReady = expr.replace("b", Double.toString(b));

            lineChart.getData().clear();

            for (DDESolver.Method method : DDESolver.Method.values()) {
                DDESolver solver = new DDESolver();
                solver.setTau2(tau2);
                solver.setInitialFunctionExpression(phiExpr);
                solver.setExpression(exprReady);
                solver.solve(method, a, tau, h, tMax);

                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName(switch (method) {
                    case EULER -> "Эйлер";
                    case RUNGE_KUTTA_4 -> "Рунге-Кутта 4";
                    case ADAMS_BASHFORTH_2 -> "Адамс-Бэшфорта";
                });

                List<Double> tList = solver.getTList();
                List<Double> yList = solver.getYList();

                for (int i = 0; i < tList.size(); i++) {
                    series.getData().add(new XYChart.Data<>(tList.get(i), yList.get(i)));
                }

                lineChart.getData().add(series);
            }

            lineChart.setTitle("Сравнение методов: a=" + a + ", τ=" + tau + ", h=" + h);
        });

        // === Кнопка "Экспорт всех методов" ===
        Button exportAllButton = new Button("Экспорт всех методов");

        exportAllButton.setOnAction(e -> {
            double a = Double.parseDouble(aField.getText());
            double b = bField.isVisible() ? Double.parseDouble(bField.getText()) : 0;
            double tau = Double.parseDouble(tauField.getText());
            double tau2 = tau2Field.isVisible() ? Double.parseDouble(tau2Field.getText()) : 0;
            double h = Double.parseDouble(hField.getText());
            double tMax = Double.parseDouble(tMaxField.getText());

            String expr = switch (exampleCombo.getValue()) {
                case "Классика: ẏ(t) = a·y(t-τ)" -> "a * y_tau1";
                case "Состояние: ẏ(t) = a·y(t-τ) + b·y(t)" -> "a * y_tau1 + b * y";
                case "Две задержки: ẏ(t) = a·y(t-τ₁) + b·y(t-τ₂)" -> "a * y_tau1 + b * y_tau2";
                default -> "a * y_tau1";
            };
            String exprReady = expr.replace("b", Double.toString(b));

            int steps = (int) (tMax / h);
            double[] tsLocal = new double[steps + 1];
            double[][] ysLocal = new double[3][steps + 1];

            int methodIndex = 0;
            for (DDESolver.Method method : DDESolver.Method.values()) {
                DDESolver solver = new DDESolver();
                solver.setTau2(tau2);
                solver.setInitialFunctionExpression(phiField.getText());
                solver.setExpression(exprReady);
                solver.solve(method, a, tau, h, tMax);

                List<Double> tList = solver.getTList();
                List<Double> yList = solver.getYList();

                for (int i = 0; i < tList.size(); i++) {
                    tsLocal[i] = tList.get(i);
                    ysLocal[methodIndex][i] = yList.get(i);
                }
                methodIndex++;
            }

            // Сохраняем
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Сохранить все решения");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы", "*.csv"));
            File file = fileChooser.showSaveDialog(new Stage());

            if (file != null) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("t,Euler,RK4,Adams\n");
                    for (int i = 0; i < tsLocal.length; i++) {
                        writer.write(tsLocal[i] + "," + ysLocal[0][i] + "," + ysLocal[1][i] + "," + ysLocal[2][i] + "\n");
                    }
                    System.out.println("Файл успешно экспортирован!");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Обработка кнопки "Сохранить" // === Кнопка "Сохранить текущее решение" ===
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
        // === Кнопка "Показать таблицу" ===
        Button tableButton = new Button("Показать таблицу");

        tableButton.setOnAction(e -> {
            if (ts == null || ys == null || ts.length == 0 || ys.length == 0) {
                showAlert("Нет данных", "Сначала рассчитайте решение.");
                return;
            }
            if (tauField.getText().isEmpty() || hField.getText().isEmpty()) {
                showAlert("Ошибка", "Пожалуйста, введите значения τ и h.");
                return;
            }

            double tau = Double.parseDouble(tauField.getText());
            double tau2 = tau2Field.getText().isEmpty() ? 0 : Double.parseDouble(tau2Field.getText());

            TableView<TableRowData> table = new TableView<>();

            TableColumn<TableRowData, Double> colT = new TableColumn<>("t");
            colT.setCellValueFactory(data -> data.getValue().tProperty().asObject());

            TableColumn<TableRowData, Double> colY = new TableColumn<>("y(t)");
            colY.setCellValueFactory(data -> data.getValue().yProperty().asObject());

            TableColumn<TableRowData, Double> colTau1 = new TableColumn<>("y(t - τ₁)");
            colTau1.setCellValueFactory(data -> data.getValue().yTau1Property().asObject());

            TableColumn<TableRowData, Double> colTau2 = new TableColumn<>("y(t - τ₂)");
            colTau2.setCellValueFactory(data -> data.getValue().yTau2Property().asObject());

            table.getColumns().addAll(colT, colY, colTau1);
            if (tau2 > 0) table.getColumns().add(colTau2);

            for (int i = 0; i < ts.length; i++) {
                double t = ts[i];
                double y = ys[i];
                double yTau1 = getInterpolatedY(t - tau, ts, ys);
                double yTau2 = getInterpolatedY(t - tau2, ts, ys);
                table.getItems().add(new TableRowData(t, y, yTau1, yTau2));
            }

            Stage tableStage = new Stage();
            tableStage.setTitle("Таблица значений");
            tableStage.setScene(new Scene(table, 600, 400));
            tableStage.show();
        });

        //кнопка проверки ошибки двух методов  // === Кнопка "График ошибки" ===
        Button errorButton = new Button("График ошибки");

        errorButton.setOnAction(e -> {
            double a = Double.parseDouble(aField.getText());
            double b = bField.isVisible() ? Double.parseDouble(bField.getText()) : 0;
            double tau = Double.parseDouble(tauField.getText());
            double tau2 = tau2Field.isVisible() ? Double.parseDouble(tau2Field.getText()) : 0;
            double h = Double.parseDouble(hField.getText());
            double tMax = Double.parseDouble(tMaxField.getText());
            String phiExpr = phiField.getText();

            String expr = switch (exampleCombo.getValue()) {
                case "Классика: ẏ(t) = a·y(t-τ)" -> "a * y_tau1";
                case "Состояние: ẏ(t) = a·y(t-τ) + b·y(t)" -> "a * y_tau1 + b * y";
                case "Две задержки: ẏ(t) = a·y(t-τ₁) + b·y(t-τ₂)" -> "a * y_tau1 + b * y_tau2";
                default -> "a * y_tau1";
            };
            String exprReady = expr.replace("b", Double.toString(b));

            DDESolver solver1 = new DDESolver();
            solver1.setTau2(tau2);
            solver1.setInitialFunctionExpression(phiExpr);
            solver1.setExpression(exprReady);
            solver1.solve(DDESolver.Method.EULER, a, tau, h, tMax);

            DDESolver solver2 = new DDESolver();
            solver2.setTau2(tau2);
            solver2.setInitialFunctionExpression(phiExpr);
            solver2.setExpression(exprReady);
            solver2.solve(DDESolver.Method.RUNGE_KUTTA_4, a, tau, h, tMax);

            List<Double> tList1 = solver1.getTList();
            List<Double> yList1 = solver1.getYList();
            List<Double> yList2 = solver2.getYList();

            XYChart.Series<Number, Number> errorSeries = new XYChart.Series<>();
            errorSeries.setName("Ошибка |Эйлер - РК4|");

            for (int i = 0; i < tList1.size(); i++) {
                double error = Math.abs(yList1.get(i) - yList2.get(i));
                errorSeries.getData().add(new XYChart.Data<>(tList1.get(i), error));
            }

            lineChart.getData().clear();
            lineChart.getData().add(errorSeries);
            lineChart.setTitle("График ошибки между методами: Эйлер и РК4");
        });
        // === Кнопка "Система DDE" (сейчас закомментирована) ===
        Button systemButton = new Button("Система DDE");

        systemButton.setOnAction(e -> {
            double a = Double.parseDouble(aField.getText());
            double tau = Double.parseDouble(tauField.getText());
            double h = Double.parseDouble(hField.getText());
            double tMax = Double.parseDouble(tMaxField.getText());

            int steps = (int) (tMax / h);
            double[] ts = new double[steps + 1];
            double[] xs = new double[steps + 1];
            double[] ys = new double[steps + 1];

            for (int i = 0; i <= steps; i++) {
                ts[i] = i * h;
            }

            // φ(t) = 1 по умолчанию для x и y при t <= 0
            for (int i = 0; i <= steps; i++) {
                if (ts[i] <= 0) {
                    xs[i] = 1.0;
                    ys[i] = 1.0;
                } else {
                    double t = ts[i];
                    double t_tau = t - tau;
                    double xTau = getSystemYAt(t_tau, ts, xs);
                    double yTau = getSystemYAt(t_tau, ts, ys);
                    xs[i] = xs[i - 1] + h * (a * yTau);
                    ys[i] = ys[i - 1] + h * (xTau);
                }
            }

            XYChart.Series<Number, Number> xSeries = new XYChart.Series<>();
            xSeries.setName("x(t)");
            XYChart.Series<Number, Number> ySeries = new XYChart.Series<>();
            ySeries.setName("y(t)");

            for (int i = 0; i <= steps; i++) {
                xSeries.getData().add(new XYChart.Data<>(ts[i], xs[i]));
                ySeries.getData().add(new XYChart.Data<>(ts[i], ys[i]));
            }

            lineChart.getData().clear();
            lineChart.getData().addAll(xSeries, ySeries);
            lineChart.setTitle("Система DDE: x'(t) = a y(t-τ), y'(t) = x(t-τ)");
        });
        // === Формирование колонок для HBox ===
        // ==== Первый столбик: Параметры ====
        VBox column1 = new VBox(10,
                aLabel, aField,
                bLabel, bField,
                tauLabel, tauField,
                tau2Label, tau2Field,
                hLabel, hField,
                tMaxLabel, tMaxField
        );
        column1.setPadding(new Insets(10));
        column1.setPrefWidth(180);

        // ==== Второй столбик: Выражения ====
        VBox column2 = new VBox(10,
                phiLabel, phiField,
                new Label("Пример:"), exampleCombo,
                new Label("Метод:"), methodCombo
        );
        column2.setPadding(new Insets(10));
        column2.setPrefWidth(260);

        // ==== Третий столбик: Кнопки ====
        VBox column3 = new VBox(10,
                new Label("Действия:"),
                solveButton,
                compareButton,
                errorButton,
                //systemButton,
                tableButton,
                saveButton,
                exportAllButton,
                helpButton
        );
        column3.setPadding(new Insets(10));
        column3.setPrefWidth(200);

        // Можно добавить descriptionLabel под equationLabel, если захочешь!
        VBox column4 = new VBox(equationLabel);
        column4.setPadding(new Insets(10));
        column4.setPrefWidth(500);
        column4.setAlignment(Pos.CENTER);
        equationLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");


        // ==== Горизонтальный ряд из 3 колонок ==== // === Основной ряд (четыре колонки) ===
        HBox topRow = new HBox(30, column1, column2, column3, column4); // добавь column3 если нужно

        // ==== Главный контейнер: три столбца сверху + график ====
        VBox root = new VBox(20, topRow, lineChart);
        root.setPadding(new Insets(10));
        lineChart.setPrefHeight(500); // жирный график
        stage.setScene(new Scene(root, 1200, 800));
        stage.setTitle("DDE Solver");
        stage.show(); // <-- ОЧЕНЬ ВАЖНО

        // Tooltips для кнопок и combo //всплюывают
        solveButton.setTooltip(new Tooltip("Рассчитать решение с выбранным методом"));
        compareButton.setTooltip(new Tooltip("Построить графики всех трёх методов"));
        errorButton.setTooltip(new Tooltip("Показать разницу между методами"));
        systemButton.setTooltip(new Tooltip("Решение системы с задержкой"));
        tableButton.setTooltip(new Tooltip("Открыть таблицу значений"));
        saveButton.setTooltip(new Tooltip("Сохранить текущее решение в файл CSV"));
        exportAllButton.setTooltip(new Tooltip("Сохранить все три метода в один CSV-файл"));
        exampleCombo.setTooltip(new Tooltip("Выберите тип уравнения. Для описания режимов нажмите ℹ️ Справка."));
    }
    // ==== Вспомогательные методы ====

    // Для ошибок
    private void showErrorDialog(String ошибкаВычислений, String message) {}
    // Для обновления графика (не используется напрямую)
    private void updateChart(DDESolver solver, String methodName, double a, double tau) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(String.format("%s (a=%.2f, τ=%.2f)", methodName, a, tau));

        List<Double> tList = solver.getTList();
        List<Double> yList = solver.getYList();

        if (tList.size() != yList.size()) {
            showErrorDialog("Ошибка данных", "Несовпадение размеров массивов");
            return;
        }
    }
    // Для отображения ошибок
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    // Интерполяция для системы (x и y)
    private double getSystemYAt(double t, double[] ts, double[] ys) {
        if (t <= ts[0]) return 1.0;
        if (t >= ts[ts.length - 1]) return ys[ys.length - 1];

        for (int i = 0; i < ts.length - 1; i++) {
            if (ts[i] <= t && t < ts[i + 1]) {
                double t1 = ts[i];
                double t2 = ts[i + 1];
                double y1 = ys[i];
                double y2 = ys[i + 1];
                return y1 + (y2 - y1) * (t - t1) / (t2 - t1);
            }
        }
        return ys[0];
    }
    // Интерполяция для вычисления y(t-τ)
    private double getInterpolatedY(double t, double[] ts, double[] ys) {
        if (t <= ts[0]) return ys[0];
        if (t >= ts[ts.length - 1]) return ys[ys.length - 1];

        for (int i = 0; i < ts.length - 1; i++) {
            if (ts[i] <= t && t < ts[i + 1]) {
                double t1 = ts[i];
                double t2 = ts[i + 1];
                double y1 = ys[i];
                double y2 = ys[i + 1];
                return y1 + (y2 - y1) * (t - t1) / (t2 - t1);
            }
        }
        return ys[0];
    }

    // Генерация текстового вида уравнения (для label)
    private String getEquationText(String example, String a, String b, String tau, String tau2) {
        String aShow = (a == null || a.isEmpty()) ? "a" : a;
        String bShow = (b == null || b.isEmpty()) ? "b" : b;
        String tauShow = (tau == null || tau.isEmpty()) ? "τ" : tau;
        String tau2Show = (tau2 == null || tau2.isEmpty()) ? "τ₂" : tau2;
        switch (example) {
            case "Классика: ẏ(t) = a·y(t-τ)":
                return "ẏ(t) = " + aShow + "·y(t-" + tauShow + ")";
            case "Состояние: ẏ(t) = a·y(t-τ) + b·y(t)":
                return "ẏ(t) = " + aShow + "·y(t-" + tauShow + ") + " + bShow + "·y(t)";
            case "Две задержки: ẏ(t) = a·y(t-τ₁) + b·y(t-τ₂)":
                return "ẏ(t) = " + aShow + "·y(t-" + tauShow + ") + " + bShow + "·y(t-" + tau2Show + ")";
            default:
                return "";
        }
    }

    // === откуда  ===
    public static void main(String[] args) {
        launch();
    }
}