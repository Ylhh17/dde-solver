package dde;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

public class DDESolver {

    private static final double EPS = 1e-12;

    public enum Method {
        EULER,
        RUNGE_KUTTA_4,
        ADAMS_BASHFORTH_2
    }

    private final List<Double> tList = new ArrayList<>();
    private final List<Double> yList = new ArrayList<>();

    private String phiExprStr = "1";
    private String expressionStr = "a * y_tau1";
    private Expression phiExpression = buildPhiExpression(phiExprStr);
    private Expression rhsExpression = buildRhsExpression(expressionStr);
    private DoubleUnaryOperator initialFunction;

    private double tau = 1.0;
    private double tau2 = 0.0;

    public void setInitialFunctionExpression(String expression) {
        String normalized = requireExpression(expression, "Начальная функция");
        this.phiExpression = buildPhiExpression(normalized);
        this.phiExprStr = normalized;
        this.initialFunction = null;
    }

    public void setInitialFunction(DoubleUnaryOperator function) {
        this.initialFunction = Objects.requireNonNull(function, "Начальная функция не должна быть null");
    }

    public void setExpression(String expression) {
        String normalized = requireExpression(expression, "Правая часть уравнения");
        this.rhsExpression = buildRhsExpression(normalized);
        this.expressionStr = normalized;
    }

    public void setTau2(double tau2) {
        requireFinite(tau2, "Вторая задержка");
        if (tau2 < 0.0) {
            throw new IllegalArgumentException("Вторая задержка не может быть отрицательной");
        }
        this.tau2 = tau2;
    }

    public List<Double> getTList() {
        return Collections.unmodifiableList(tList);
    }

    public List<Double> getYList() {
        return Collections.unmodifiableList(yList);
    }

    public void solve(Method method, double a, double tau, double h, double tMax) {
        validateParameters(method, a, tau, h, tMax);
        this.tau = tau;

        tList.clear();
        yList.clear();
        appendPoint(0.0, history(0.0));

        if (tMax == 0.0) {
            return;
        }

        switch (method) {
            case EULER -> solveEuler(a, h, tMax);
            case RUNGE_KUTTA_4 -> solveRungeKutta4(a, h, tMax);
            case ADAMS_BASHFORTH_2 -> solveAdamsBashforth2(a, h, tMax);
        }
    }

    private void solveEuler(double a, double h, double tMax) {
        while (lastTime() < tMax - EPS) {
            double t = lastTime();
            double y = lastValue();
            double step = Math.min(h, tMax - t);
            double nextY = y + step * derivative(t, y, a);
            appendPoint(nextTime(t, step, tMax), nextY);
        }
    }

    private void solveRungeKutta4(double a, double h, double tMax) {
        while (lastTime() < tMax - EPS) {
            double t = lastTime();
            double y = lastValue();
            double step = Math.min(h, tMax - t);
            appendPoint(nextTime(t, step, tMax), rungeKuttaStep(t, y, step, a));
        }
    }

    private void solveAdamsBashforth2(double a, double h, double tMax) {
        double t0 = lastTime();
        double y0 = lastValue();
        double firstStep = Math.min(h, tMax - t0);
        double previousDerivative = derivative(t0, y0, a);
        appendPoint(nextTime(t0, firstStep, tMax), rungeKuttaStep(t0, y0, firstStep, a));

        double previousStep = firstStep;
        while (lastTime() < tMax - EPS) {
            double t = lastTime();
            double y = lastValue();
            double step = Math.min(h, tMax - t);
            double currentDerivative = derivative(t, y, a);
            double nextY;

            if (approximatelyEqual(step, h) && approximatelyEqual(previousStep, h)) {
                nextY = y + step * (3.0 * currentDerivative - previousDerivative) / 2.0;
            } else {
                nextY = rungeKuttaStep(t, y, step, a);
            }

            appendPoint(nextTime(t, step, tMax), nextY);
            previousDerivative = currentDerivative;
            previousStep = step;
        }
    }

    private double rungeKuttaStep(double t, double y, double h, double a) {
        double k1 = derivative(t, y, a);
        double k2 = derivative(t + h / 2.0, y + h * k1 / 2.0, a);
        double k3 = derivative(t + h / 2.0, y + h * k2 / 2.0, a);
        double k4 = derivative(t + h, y + h * k3, a);
        return y + h * (k1 + 2.0 * k2 + 2.0 * k3 + k4) / 6.0;
    }

    private double derivative(double t, double y, double a) {
        double yTau1 = valueAt(t - tau);
        double yTau2 = tau2 == 0.0 ? y : valueAt(t - tau2);

        try {
            double result = rhsExpression
                    .setVariable("t", t)
                    .setVariable("y", y)
                    .setVariable("y_tau1", yTau1)
                    .setVariable("y_tau2", yTau2)
                    .setVariable("a", a)
                    .evaluate();
            requireFinite(result, "Значение правой части");
            return result;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Не удалось вычислить правую часть '" + expressionStr + "' при t=" + t,
                    exception
            );
        }
    }

    private double history(double t) {
        try {
            double result = initialFunction != null
                    ? initialFunction.applyAsDouble(t)
                    : phiExpression.setVariable("t", t).evaluate();
            requireFinite(result, "Значение начальной функции");
            return result;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Не удалось вычислить начальную функцию '" + phiExprStr + "' при t=" + t,
                    exception
            );
        }
    }

    private double valueAt(double t) {
        if (t <= 0.0) {
            return history(t);
        }
        if (tList.isEmpty()) {
            throw new IllegalStateException("Решение ещё не содержит вычисленных точек");
        }

        double latestTime = lastTime();
        double tolerance = EPS * Math.max(1.0, Math.abs(latestTime));
        if (t > latestTime + tolerance) {
            throw new IllegalStateException(
                    "Для вычисления требуется ещё не найденное значение y(" + t + "). " +
                    "Уменьшите шаг так, чтобы он не превышал положительные задержки."
            );
        }
        if (Math.abs(t - latestTime) <= tolerance) {
            return lastValue();
        }

        int index = Collections.binarySearch(tList, t);
        if (index >= 0) {
            return yList.get(index);
        }

        int right = -index - 1;
        int left = right - 1;
        double t1 = tList.get(left);
        double t2 = tList.get(right);
        double y1 = yList.get(left);
        double y2 = yList.get(right);
        return y1 + (y2 - y1) * (t - t1) / (t2 - t1);
    }

    private void validateParameters(Method method, double a, double tau, double h, double tMax) {
        Objects.requireNonNull(method, "Численный метод не выбран");
        requireFinite(a, "Параметр a");
        requireFinite(tau, "Задержка");
        requireFinite(h, "Шаг");
        requireFinite(tMax, "Конец интервала");

        if (tau <= 0.0) {
            throw new IllegalArgumentException("Задержка должна быть больше нуля");
        }
        if (h <= 0.0) {
            throw new IllegalArgumentException("Шаг должен быть больше нуля");
        }
        if (tMax < 0.0) {
            throw new IllegalArgumentException("Конец интервала не может быть отрицательным");
        }
        if (h > tau + EPS) {
            throw new IllegalArgumentException("Шаг не должен превышать первую задержку");
        }
        if (tau2 > 0.0 && h > tau2 + EPS) {
            throw new IllegalArgumentException("Шаг не должен превышать вторую задержку");
        }
    }

    private void appendPoint(double t, double y) {
        requireFinite(t, "Время");
        requireFinite(y, "Значение решения");
        tList.add(t);
        yList.add(y);
    }

    private double lastTime() {
        return tList.get(tList.size() - 1);
    }

    private double lastValue() {
        return yList.get(yList.size() - 1);
    }

    private static double nextTime(double t, double step, double tMax) {
        double candidate = t + step;
        return Math.abs(candidate - tMax) <= EPS * Math.max(1.0, Math.abs(tMax)) ? tMax : candidate;
    }

    private static boolean approximatelyEqual(double first, double second) {
        return Math.abs(first - second) <= EPS * Math.max(1.0, Math.max(Math.abs(first), Math.abs(second)));
    }

    private static String requireExpression(String expression, String name) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException(name + " не может быть пустой");
        }
        return expression.trim();
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " должно быть конечным числом");
        }
    }

    private static Expression buildPhiExpression(String expression) {
        try {
            return new ExpressionBuilder(expression).variable("t").build();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Некорректная начальная функция: " + expression, exception);
        }
    }

    private static Expression buildRhsExpression(String expression) {
        try {
            return new ExpressionBuilder(expression)
                    .variables("t", "y", "y_tau1", "y_tau2", "a")
                    .build();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Некорректная правая часть: " + expression, exception);
        }
    }
}
