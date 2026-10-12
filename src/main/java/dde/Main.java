package dde;

import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main extends Application {

    private static final String CLASSIC = "Классика: ẏ(t) = a·y(t-τ)";
    private static final String STATE = "Состояние: ẏ(t) = a·y(t-τ) + b·y(t)";
    private static final String TWO_DELAYS = "Две задержки: ẏ(t) = a·y(t-τ₁) + b·y(t-τ₂)";

    private final TextField aField = new TextField("-1");
    private final TextField bField = new TextField("0.5");
    private final TextField tauField = new TextField("1");
    private final TextField tau2Field = new TextField("0.5");
    private final TextField hField = new TextField("0.05");
    private final TextField tMaxField = new TextField("10");
    private final TextField phiField = new TextField("1");
    private final TextField exactField = new TextField();

    private final ComboBox<String> exampleCombo = new ComboBox<>();
    private final ComboBox<String> methodCombo = new ComboBox<>();
    private final Label equationLabel = new Label();
    private final Label descriptionLabel = new Label();
    private final Label statusLabel = new Label("Введите параметры и нажмите «Рассчитать»");
    private final Button saveButton = new Button("Сохранить в CSV");
    private final Button tableButton = new Button("Показать таблицу");

    private LineChart<Number, Number> lineChart;
    private Stage owner;
    private double[] currentTimes;
    private double[] currentValues;
    private CalculationInput currentInput;
    private String currentMethodName;

    @Override
    public void start(Stage stage) {
        owner = stage;
        configureControls();

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("t");
        yAxis.setLabel("y(t)");
        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Решение дифференциального уравнения с запаздыванием");
        lineChart.setCreateSymbols(false);
        lineChart.setAnimated(false);
        lineChart.setMinHeight(480);

        Button solveButton = new Button("Рассчитать");
        Button compareButton = new Button("Сравнить методы");
        Button errorButton = new Button("График расхождения");
        Button exactButton = new Button("Сравнить с точным");
        Button exportAllButton = new Button("Экспорт всех методов");
        Button helpButton = new Button("Справка");

        solveButton.setMaxWidth(Double.MAX_VALUE);
        compareButton.setMaxWidth(Double.MAX_VALUE);
        errorButton.setMaxWidth(Double.MAX_VALUE);
        exactButton.setMaxWidth(Double.MAX_VALUE);
        tableButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setMaxWidth(Double.MAX_VALUE);
        exportAllButton.setMaxWidth(Double.MAX_VALUE);
        helpButton.setMaxWidth(Double.MAX_VALUE);

        solveButton.setTooltip(new Tooltip("Рассчитать решение выбранным методом"));
        compareButton.setTooltip(new Tooltip("Построить решения всеми тремя методами"));
        errorButton.setTooltip(new Tooltip("Показать модуль разности решений Эйлера и RK4"));
        exactButton.setTooltip(new Tooltip("Сравнить выбранный метод с заданным аналитическим решением"));
        tableButton.setTooltip(new Tooltip("Открыть таблицу последнего рассчитанного решения"));
        saveButton.setTooltip(new Tooltip("Сохранить последнее рассчитанное решение"));
        exportAllButton.setTooltip(new Tooltip("Рассчитать и сохранить три метода в один CSV"));

        solveButton.setOnAction(event -> runSafely(this::solveCurrent));
        compareButton.setOnAction(event -> runSafely(this::compareMethods));
        errorButton.setOnAction(event -> runSafely(this::showDifference));
        exactButton.setOnAction(event -> runSafely(this::compareWithExact));
        tableButton.setOnAction(event -> runSafely(this::showTable));
        saveButton.setOnAction(event -> runSafely(this::saveCurrent));
        exportAllButton.setOnAction(event -> runSafely(this::exportAll));
        helpButton.setOnAction(event -> showHelp());

        VBox parameters = section("Параметры",
                labelled("a", aField),
                labelled("b", bField),
                labelled("τ", tauField),
                labelled("τ₂", tau2Field),
                labelled("Шаг h", hField),
                labelled("Конец интервала", tMaxField));
        parameters.setPrefWidth(210);

        VBox equation = section("Уравнение",
                labelled("Начальная функция φ(t), t ≤ 0", phiField),
                labelled("Точное решение y(t), необязательно", exactField),
                labelled("Тип уравнения", exampleCombo),
                labelled("Численный метод", methodCombo));
        equation.setPrefWidth(390);

        setOptionalFieldsVisibility();

        VBox actions = section("Действия", solveButton, compareButton, errorButton, exactButton,
                tableButton, saveButton, exportAllButton, helpButton);
        actions.setPrefWidth(220);

        equationLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        equationLabel.setWrapText(true);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-text-fill: #555555;");
        VBox information = section("Текущая задача", equationLabel, descriptionLabel);
        HBox.setHgrow(information, Priority.ALWAYS);

        HBox top = new HBox(18, parameters, equation, actions, information);
        top.setAlignment(Pos.TOP_LEFT);
        top.setPadding(new Insets(12));

        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setStyle("-fx-padding: 8; -fx-background-color: #eef3f8; -fx-font-size: 13px;");

        VBox content = new VBox(12, top, statusLabel, lineChart);
        content.setPadding(new Insets(10));
        VBox.setVgrow(lineChart, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        stage.setScene(new Scene(scrollPane, 1280, 820));
        stage.setMinWidth(980);
        stage.setMinHeight(700);
        stage.setTitle("DDE Solver");
        stage.show();
    }

    private void configureControls() {
        exampleCombo.getItems().setAll(CLASSIC, STATE, TWO_DELAYS);
        exampleCombo.setValue(CLASSIC);
        exampleCombo.setMaxWidth(Double.MAX_VALUE);

        methodCombo.getItems().setAll("Метод Эйлера", "Метод Рунге-Кутты 4", "Метод Адамса-Бэшфорта 2");
        methodCombo.setValue("Метод Рунге-Кутты 4");
        methodCombo.setMaxWidth(Double.MAX_VALUE);

        setOptionalFieldsVisibility();
        updateEquationAndDescription();
        saveButton.setDisable(true);
        tableButton.setDisable(true);

        List<TextField> fields = List.of(aField, bField, tauField, tau2Field, hField, tMaxField, phiField, exactField);
        for (TextField field : fields) {
            field.setMaxWidth(Double.MAX_VALUE);
            field.textProperty().addListener(this::inputChanged);
        }
        exampleCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            setOptionalFieldsVisibility();
            updateEquationAndDescription();
            invalidateCurrentResult();
        });
        methodCombo.valueProperty().addListener((observable, oldValue, newValue) -> invalidateCurrentResult());
    }

    private void inputChanged(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        updateEquationAndDescription();
        invalidateCurrentResult();
    }

    private void solveCurrent() {
        CalculationInput input = readInput();
        DDESolver.Method method = selectedMethod();

        long startedAt = System.nanoTime();
        DDESolver solver = createSolver(input);
        solver.solve(method, input.a(), input.tau(), input.h(), input.tMax());
        long elapsedNanos = System.nanoTime() - startedAt;

        currentTimes = toArray(solver.getTList());
        currentValues = toArray(solver.getYList());
        currentInput = input;
        currentMethodName = methodName(method);
        saveButton.setDisable(false);
        tableButton.setDisable(false);

        NumericalSolution numerical = new NumericalSolution(method, solver.getTList(), solver.getYList());
        ExactSolution exact = optionalExactSolution(input);
        if (exact == null) {
            lineChart.getData().setAll(series(currentMethodName, numerical));
            setSuccess(String.format(Locale.ROOT, "Готово: %d точек, %.3f мс", currentTimes.length, elapsedNanos / 1_000_000.0));
        } else {
            ErrorMetrics metrics = ErrorMetrics.compare(numerical, exact);
            lineChart.getData().setAll(series(currentMethodName, numerical), exactSeries(exact, numerical.times()));
            setSuccess(metricsMessage(currentTimes.length, elapsedNanos, metrics));
        }
        lineChart.setTitle("Решение: " + currentMethodName);
    }

    private void compareMethods() {
        CalculationInput input = readInput();
        long startedAt = System.nanoTime();
        List<XYChart.Series<Number, Number>> series = new ArrayList<>();

        for (DDESolver.Method method : DDESolver.Method.values()) {
            DDESolver solver = solve(input, method);
            series.add(series(methodName(method), new NumericalSolution(method, solver.getTList(), solver.getYList())));
        }

        ExactSolution exact = optionalExactSolution(input);
        if (exact != null) {
            DDESolver referenceGrid = solve(input, DDESolver.Method.RUNGE_KUTTA_4);
            series.add(exactSeries(exact, referenceGrid.getTList()));
        }
        lineChart.getData().setAll(series);
        lineChart.setTitle(exact == null ? "Сравнение численных методов" : "Сравнение методов с точным решением");
        long elapsedNanos = System.nanoTime() - startedAt;
        setSuccess(String.format(Locale.ROOT, "Три метода рассчитаны за %.3f мс", elapsedNanos / 1_000_000.0));
    }

    private void showDifference() {
        CalculationInput input = readInput();
        DDESolver euler = solve(input, DDESolver.Method.EULER);
        DDESolver rk4 = solve(input, DDESolver.Method.RUNGE_KUTTA_4);
        ensureSameGrid(euler, rk4);

        XYChart.Series<Number, Number> difference = new XYChart.Series<>();
        difference.setName("|Эйлер − RK4|");
        for (int index = 0; index < euler.getTList().size(); index++) {
            difference.getData().add(new XYChart.Data<>(
                    euler.getTList().get(index),
                    Math.abs(euler.getYList().get(index) - rk4.getYList().get(index))
            ));
        }

        lineChart.getData().setAll(difference);
        lineChart.setTitle("Расхождение решений Эйлера и Рунге-Кутты 4");
        setSuccess("График расхождения построен. Это разность двух приближений, а не точная погрешность.");
    }

    private void compareWithExact() {
        CalculationInput input = readInput();
        ExactSolution exact = requiredExactSolution(input);
        DDESolver.Method method = selectedMethod();
        long startedAt = System.nanoTime();
        DDESolver solver = solve(input, method);
        NumericalSolution numerical = new NumericalSolution(method, solver.getTList(), solver.getYList());
        ErrorMetrics metrics = ErrorMetrics.compare(numerical, exact);
        long elapsedNanos = System.nanoTime() - startedAt;

        currentTimes = toArray(numerical.times());
        currentValues = toArray(numerical.values());
        currentInput = input;
        currentMethodName = methodName(method);
        saveButton.setDisable(false);
        tableButton.setDisable(false);

        lineChart.getData().setAll(series(currentMethodName, numerical), exactSeries(exact, numerical.times()));
        lineChart.setTitle("Численное и точное решения");
        setSuccess(metricsMessage(numerical.size(), elapsedNanos, metrics));
    }

    private void saveCurrent() {
        requireCurrentResult();
        File file = chooseCsvFile("Сохранить текущее решение", "dde-solution.csv");
        if (file == null) {
            return;
        }

        ExactSolution exact = optionalExactSolution(currentInput);
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(exact == null ? "t,y\n" : "t,y,exact,absoluteError,relativeError\n");
            for (int index = 0; index < currentTimes.length; index++) {
                writer.write(csv(currentTimes[index]) + "," + csv(currentValues[index]));
                if (exact != null) {
                    double exactValue = exact.valueAt(currentTimes[index]);
                    double absoluteError = Math.abs(currentValues[index] - exactValue);
                    writer.write("," + csv(exactValue) + "," + csv(absoluteError) + ","
                            + csv(relativeError(absoluteError, exactValue)));
                }
                writer.write("\n");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сохранить файл: " + exception.getMessage(), exception);
        }
        setSuccess("Текущее решение сохранено: " + file.getName());
    }

    private void exportAll() {
        CalculationInput input = readInput();
        List<DDESolver> solvers = new ArrayList<>();
        for (DDESolver.Method method : DDESolver.Method.values()) {
            solvers.add(solve(input, method));
        }
        ensureSameGrid(solvers.get(0), solvers.get(1));
        ensureSameGrid(solvers.get(0), solvers.get(2));

        File file = chooseCsvFile("Сохранить решения всех методов", "dde-all-methods.csv");
        if (file == null) {
            return;
        }

        ExactSolution exact = optionalExactSolution(input);
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(exact == null
                    ? "t,Euler,RK4,AdamsBashforth2\n"
                    : "t,Euler,RK4,AdamsBashforth2,exact,EulerAbsError,RK4AbsError,AdamsAbsError\n");
            int size = solvers.get(0).getTList().size();
            for (int index = 0; index < size; index++) {
                double time = solvers.get(0).getTList().get(index);
                double euler = solvers.get(0).getYList().get(index);
                double rk4 = solvers.get(1).getYList().get(index);
                double adams = solvers.get(2).getYList().get(index);
                writer.write(csv(time) + "," + csv(euler) + "," + csv(rk4) + "," + csv(adams));
                if (exact != null) {
                    double exactValue = exact.valueAt(time);
                    writer.write("," + csv(exactValue)
                            + "," + csv(Math.abs(euler - exactValue))
                            + "," + csv(Math.abs(rk4 - exactValue))
                            + "," + csv(Math.abs(adams - exactValue)));
                }
                writer.write("\n");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сохранить файл: " + exception.getMessage(), exception);
        }
        setSuccess("Решения трёх методов сохранены: " + file.getName());
    }

    private void showTable() {
        requireCurrentResult();
        Expression history = new ExpressionBuilder(currentInput.phi()).variable("t").build();
        TableView<TableRowData> table = new TableView<>();

        TableColumn<TableRowData, Double> tColumn = column("t", "t");
        TableColumn<TableRowData, Double> yColumn = column("y(t)", "y");
        TableColumn<TableRowData, Double> tau1Column = column("y(t − τ)", "tau1");
        table.getColumns().addAll(tColumn, yColumn, tau1Column);

        if (currentInput.usesSecondDelay()) {
            table.getColumns().add(column("y(t − τ₂)", "tau2"));
        }

        for (int index = 0; index < currentTimes.length; index++) {
            double t = currentTimes[index];
            double yTau1 = delayedValue(t - currentInput.tau(), history);
            double yTau2 = currentInput.usesSecondDelay()
                    ? delayedValue(t - currentInput.tau2(), history)
                    : Double.NaN;
            table.getItems().add(new TableRowData(t, currentValues[index], yTau1, yTau2));
        }

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Stage tableStage = new Stage();
        tableStage.initOwner(owner);
        tableStage.setTitle("Таблица значений — " + currentMethodName);
        tableStage.setScene(new Scene(table, 720, 520));
        tableStage.show();
    }

    private TableColumn<TableRowData, Double> column(String title, String property) {
        TableColumn<TableRowData, Double> column = new TableColumn<>(title);
        switch (property) {
            case "t" -> column.setCellValueFactory(data -> data.getValue().tProperty().asObject());
            case "y" -> column.setCellValueFactory(data -> data.getValue().yProperty().asObject());
            case "tau1" -> column.setCellValueFactory(data -> data.getValue().yTau1Property().asObject());
            case "tau2" -> column.setCellValueFactory(data -> data.getValue().yTau2Property().asObject());
            default -> throw new IllegalArgumentException("Неизвестный столбец: " + property);
        }
        return column;
    }

    private double delayedValue(double time, Expression history) {
        if (time <= 0.0) {
            return history.setVariable("t", time).evaluate();
        }
        if (time >= currentTimes[currentTimes.length - 1]) {
            return currentValues[currentValues.length - 1];
        }

        int left = 0;
        int right = currentTimes.length - 1;
        while (right - left > 1) {
            int middle = (left + right) / 2;
            if (currentTimes[middle] <= time) {
                left = middle;
            } else {
                right = middle;
            }
        }
        double ratio = (time - currentTimes[left]) / (currentTimes[right] - currentTimes[left]);
        return currentValues[left] + ratio * (currentValues[right] - currentValues[left]);
    }

    private CalculationInput readInput() {
        return parseInput(exampleCombo.getValue(), aField.getText(), bField.getText(), tauField.getText(),
                tau2Field.getText(), hField.getText(), tMaxField.getText(), phiField.getText(), exactField.getText());
    }

    static CalculationInput parseInput(String example, String aText, String bText, String tauText,
                                       String tau2Text, String hText, String tMaxText,
                                       String phiText, String exactText) {
        boolean usesB = !CLASSIC.equals(example);
        boolean usesSecondDelay = TWO_DELAYS.equals(example);

        double a = InputValidator.parseFinite(aText, "a");
        double b = usesB ? InputValidator.parseFinite(bText, "b") : 0.0;
        double tau = InputValidator.parsePositive(tauText, "τ");
        double tau2 = usesSecondDelay ? InputValidator.parsePositive(tau2Text, "τ₂") : 0.0;
        double h = InputValidator.parsePositive(hText, "h");
        double tMax = InputValidator.parseNonNegative(tMaxText, "t max");
        String phi = InputValidator.requireText(phiText, "Начальная функция");
        String exact = exactText == null ? "" : exactText.trim();
        InputValidator.validateStep(h, tau, tau2);

        String expression = switch (example) {
            case STATE -> "a * y_tau1 + (" + Double.toString(b) + ") * y";
            case TWO_DELAYS -> "a * y_tau1 + (" + Double.toString(b) + ") * y_tau2";
            default -> "a * y_tau1";
        };
        return new CalculationInput(a, b, tau, tau2, h, tMax, phi, exact, expression, usesSecondDelay);
    }

    private DDESolver solve(CalculationInput input, DDESolver.Method method) {
        DDESolver solver = createSolver(input);
        solver.solve(method, input.a(), input.tau(), input.h(), input.tMax());
        return solver;
    }

    private DDESolver createSolver(CalculationInput input) {
        DDESolver solver = new DDESolver();
        solver.setTau2(input.tau2());
        solver.setInitialFunctionExpression(input.phi());
        solver.setExpression(input.expression());
        return solver;
    }

    private DDESolver.Method selectedMethod() {
        return switch (methodCombo.getValue()) {
            case "Метод Эйлера" -> DDESolver.Method.EULER;
            case "Метод Адамса-Бэшфорта 2" -> DDESolver.Method.ADAMS_BASHFORTH_2;
            default -> DDESolver.Method.RUNGE_KUTTA_4;
        };
    }

    private String methodName(DDESolver.Method method) {
        return switch (method) {
            case EULER -> "Метод Эйлера";
            case RUNGE_KUTTA_4 -> "Метод Рунге-Кутты 4";
            case ADAMS_BASHFORTH_2 -> "Метод Адамса-Бэшфорта 2";
        };
    }

    private XYChart.Series<Number, Number> series(String name, NumericalSolution solution) {
        XYChart.Series<Number, Number> chartSeries = new XYChart.Series<>();
        chartSeries.setName(name);
        for (int index = 0; index < solution.size(); index++) {
            chartSeries.getData().add(new XYChart.Data<>(solution.times().get(index), solution.values().get(index)));
        }
        return chartSeries;
    }

    private XYChart.Series<Number, Number> exactSeries(ExactSolution exact, List<Double> times) {
        XYChart.Series<Number, Number> chartSeries = new XYChart.Series<>();
        chartSeries.setName("Точное решение");
        for (double time : times) {
            chartSeries.getData().add(new XYChart.Data<>(time, exact.valueAt(time)));
        }
        return chartSeries;
    }

    private ExactSolution optionalExactSolution(CalculationInput input) {
        return input.exact().isBlank() ? null
                : new ExactSolution(input.exact(), input.a(), input.b(), input.tau(), input.tau2());
    }

    private ExactSolution requiredExactSolution(CalculationInput input) {
        ExactSolution exact = optionalExactSolution(input);
        if (exact == null) {
            throw new IllegalArgumentException("Введите выражение точного решения, например exp(t)");
        }
        return exact;
    }

    private static String metricsMessage(int pointCount, long elapsedNanos, ErrorMetrics metrics) {
        String relative = Double.isNaN(metrics.maximumRelativeError())
                ? "не определена"
                : String.format(Locale.ROOT, "%.4e", metrics.maximumRelativeError());
        return String.format(Locale.ROOT,
                "Готово: %d точек, %.3f мс; max |ошибка| = %.4e; RMS = %.4e; max отн. = %s; в конце = %.4e",
                pointCount, elapsedNanos / 1_000_000.0, metrics.maximumAbsoluteError(),
                metrics.rootMeanSquareError(), relative, metrics.finalAbsoluteError());
    }

    private void ensureSameGrid(DDESolver first, DDESolver second) {
        if (first.getTList().size() != second.getTList().size()) {
            throw new IllegalStateException("Методы вернули сетки разного размера");
        }
        for (int index = 0; index < first.getTList().size(); index++) {
            if (Math.abs(first.getTList().get(index) - second.getTList().get(index)) > 1e-10) {
                throw new IllegalStateException("Методы вернули несовпадающие временные сетки");
            }
        }
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            setError(exception.getMessage());
            showAlert(Alert.AlertType.ERROR, "Ошибка", exception.getMessage());
        } catch (RuntimeException exception) {
            setError("Непредвиденная ошибка вычисления");
            showAlert(Alert.AlertType.ERROR, "Непредвиденная ошибка",
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    private void invalidateCurrentResult() {
        currentTimes = null;
        currentValues = null;
        currentInput = null;
        currentMethodName = null;
        saveButton.setDisable(true);
        tableButton.setDisable(true);
        statusLabel.setText("Параметры изменены — выполните расчёт заново");
        statusLabel.setStyle("-fx-padding: 8; -fx-background-color: #fff6dd; -fx-font-size: 13px;");
    }

    private void requireCurrentResult() {
        if (currentTimes == null || currentValues == null || currentInput == null) {
            throw new IllegalStateException("Сначала рассчитайте решение с текущими параметрами");
        }
    }

    private void setOptionalFieldsVisibility() {
        boolean usesB = !CLASSIC.equals(exampleCombo.getValue());
        boolean usesSecondDelay = TWO_DELAYS.equals(exampleCombo.getValue());
        setVisibleManaged(bField.getParent(), usesB);
        setVisibleManaged(tau2Field.getParent(), usesSecondDelay);
    }

    private void updateEquationAndDescription() {
        String a = display(aField.getText(), "a");
        String b = display(bField.getText(), "b");
        String tau = display(tauField.getText(), "τ");
        String tau2 = display(tau2Field.getText(), "τ₂");

        switch (exampleCombo.getValue()) {
            case STATE -> {
                equationLabel.setText("y′(t) = " + a + "·y(t−" + tau + ") + " + b + "·y(t)");
                descriptionLabel.setText("Запаздывающее и текущее состояния влияют на скорость изменения решения.");
            }
            case TWO_DELAYS -> {
                equationLabel.setText("y′(t) = " + a + "·y(t−" + tau + ") + " + b + "·y(t−" + tau2 + ")");
                descriptionLabel.setText("Правая часть использует два предыдущих состояния с разными задержками.");
            }
            default -> {
                equationLabel.setText("y′(t) = " + a + "·y(t−" + tau + ")");
                descriptionLabel.setText("Классическое уравнение с одной постоянной задержкой.");
            }
        }
    }

    private void showHelp() {
        showAlert(Alert.AlertType.INFORMATION, "Справка",
                "Задайте коэффициенты, задержки, шаг, конец интервала и историю φ(t) для t ≤ 0.\n\n"
                        + "Шаг должен быть положительным и не превышать используемые задержки. "
                        + "Допустимы выражения exp4j, например: 1, sin(t), exp(t).\n\n"
                        + "«График расхождения» показывает |Эйлер − RK4|. Это полезная оценка согласованности, "
                        + "но не точная ошибка относительно аналитического решения.");
    }

    private File chooseCsvFile(String title, String initialName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialFileName(initialName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV-файлы", "*.csv"));
        File file = chooser.showSaveDialog(owner);
        if (file != null && !file.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }
        return file;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-padding: 8; -fx-background-color: #e4f6e8; -fx-font-size: 13px;");
    }

    private void setError(String message) {
        statusLabel.setText("Ошибка: " + message);
        statusLabel.setStyle("-fx-padding: 8; -fx-background-color: #fde8e8; -fx-font-size: 13px;");
    }

    private VBox labelled(String label, javafx.scene.Node control) {
        Label title = new Label(label);
        VBox box = new VBox(4, title, control);
        box.setFillWidth(true);
        return box;
    }

    private VBox section(String title, javafx.scene.Node... nodes) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        VBox box = new VBox(9);
        box.getChildren().add(heading);
        box.getChildren().addAll(nodes);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-border-color: #d7dde5; -fx-border-radius: 6; -fx-background-radius: 6;"
                + "-fx-background-color: white;");
        return box;
    }

    private static void setVisibleManaged(javafx.scene.Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private static String display(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static double[] toArray(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private static double relativeError(double absoluteError, double exactValue) {
        return Math.abs(exactValue) <= 1e-12 ? Double.NaN : absoluteError / Math.abs(exactValue);
    }

    private static String csv(double value) {
        return Double.toString(value);
    }

    record CalculationInput(
            double a,
            double b,
            double tau,
            double tau2,
            double h,
            double tMax,
            String phi,
            String exact,
            String expression,
            boolean usesSecondDelay
    ) {
    }

    public static void main(String[] args) {
        launch(args);
    }
}
