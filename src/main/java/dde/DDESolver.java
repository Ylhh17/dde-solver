package dde;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

public class DDESolver {

    private double tau = 1.0; // задержка - теперь поле класса

    public enum Method {
        EULER,
        RUNGE_KUTTA_4,
        ADAMS_BASHFORTH_2
    }

    private String phiExprStr = "1";
    private final List<Double> tList = new ArrayList<>();
    private final List<Double> yList = new ArrayList<>();
    private DoubleUnaryOperator phi = t -> 1.0;
    // Строка-выражение задаёт правую часть уравнения (гибко)
    private String expressionStr = "a * y_tau1"; // значение по умолчанию
    private double tau2 = 0;

    public void setInitialFunctionExpression(String expr) {
        this.phiExprStr = expr;
    }

    public void setInitialFunction(DoubleUnaryOperator phi) {
        this.phi = phi;
    }

    public void setExpression(String expr) {
        this.expressionStr = expr;
    }

    public void setTau2(double tau2) {
        this.tau2 = tau2;
    }

    public List<Double> getTList() { return tList; }
    public List<Double> getYList() { return yList; }

    // Исправлено: для t < 0 возвращаем 0, иначе считаем phi(t)
    private double getPhi(double t) {
        if (t < 0) return 0.0; // вне области — 0
        try { // иначе вычисляем значение выражения phi для данного t
            Expression expr = new ExpressionBuilder(phiExprStr)
                    .variable("t")
                    .build()
                    .setVariable("t", t);
            return expr.evaluate();
        } catch (Exception e) {
            System.err.println("Ошибка в выражении φ(t): " + e.getMessage());
            return 1.0; // fallback
        }
    }


    // Вычисление правой части уравнения с задержками (универсально)
    private double f(double t, double y, double yTau1, double yTau2, double a) {
        try {
            Expression expr = new ExpressionBuilder(expressionStr)
                    .variables("t", "y", "y_tau1", "y_tau2", "a")
                    .build()
                    .setVariable("t", t) // t — текущее время
                    .setVariable("y", y) // y — текущее значение
                    .setVariable("y_tau1", yTau1) // yTau1 — значение с задержкой tau
                    .setVariable("y_tau2", yTau2) // yTau2 — значение с задержкой tau2
                    .setVariable("a", a); // a — параметр уравнения
            return expr.evaluate();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }



    public void solve(Method method, double a, double tau, double h, double tMax) {
        tList.clear();
        yList.clear();
        this.tau = tau; // сохраняем tau в поле для использования везде

        switch (method) {
            case EULER -> solveEuler(a, h, tMax);
            case RUNGE_KUTTA_4 -> solveRK4(a, h, tMax);
            case ADAMS_BASHFORTH_2 -> solveAdams(a, h, tMax);
        }
    }
    // Метод Эйлера для DDE
    private void solveEuler(double a, double h, double tMax) {
        int steps = (int)(tMax / h);
        for (int i = 0; i <= steps; i++) {
            double t = i * h;
            double y;
            if (t <= tau) {
                y = getPhi(t);// начальная функция на интервале [0, tau]
            } else {
                double yTau1 = getYAt(t - tau, h); // y(t - tau), с интерполяцией
                double yTau2 = getYAt(t - tau2, h); // y(t - tau2), если есть вторая задержка
                double yPrev = yList.get(i - 1); // предыдущее значение y(t)
                y = yPrev + h * f(t, yPrev, yTau1, yTau2, a); // шаг Эйлера
            }

            tList.add(t); // записываем время
            yList.add(y); // записываем результат
        }
    }
    // Метод Рунге-Кутты 4 порядка для DDE
    private void solveRK4(double a, double h, double tMax) {
        int steps = (int)(tMax / h);
        for (int i = 0; i <= steps; i++) {
            double t = i * h;
            double y;
            if (t <= tau) {
                y = getPhi(t);// начальная функция
            } else {
                double yPrev = yList.get(i - 1);
                // k1: на текущем шаге
                double k1 = f(t, yPrev, getYAt(t - tau, h), getYAt(t - tau2, h), a);
                double k2 = f(t + h / 2, yPrev + h * k1 / 2, // k2: на середине шага
                        getYAt(t + h / 2 - tau, h), getYAt(t + h / 2 - tau2, h), a);
                double k3 = f(t + h / 2, yPrev + h * k2 / 2,
                        getYAt(t + h / 2 - tau, h), getYAt(t + h / 2 - tau2, h), a);
                double k4 = f(t + h, yPrev + h * k3,  // k4: на конце шага
                        getYAt(t + h - tau, h), getYAt(t + h - tau2, h), a);
                y = yPrev + (h / 6) * (k1 + 2 * k2 + 2 * k3 + k4);
            }
            tList.add(t);
            yList.add(y);
        }
    }
    // Метод Адамса-Бэшфорта второго порядка
    private void solveAdams(double a, double h, double tMax) {
        int steps = (int)(tMax / h);

        // 1. Заполняем t ∈ [0, tau] начальными значениями phi(t)
        int nInit = (int)(tau / h);
        for (int i = 0; i <= nInit; i++) {
            double t = i * h;
            tList.add(t);
            yList.add(getPhi(t));
        }

        // 2. Первые 2 точки после tau считаем по Эйлеру (или RK4)
        for (int i = nInit+1; i <= nInit+2; i++) {
            double t = i * h;
            double yPrev = yList.get(i-1);
            double yTau1 = getYAt(t - tau, h);
            double yTau2 = getYAt(t - tau2, h);
            double y = yPrev + h * f(t, yPrev, yTau1, yTau2, a);
            tList.add(t);
            yList.add(y);
        }

        // 3. Основной шаг Адамса-Бэшфорта
        for (int i = nInit+3; i <= steps; i++) {
            double t = i * h;
            double f_n = f(t, yList.get(i - 1), getYAt(t - tau, h), getYAt(t - tau2, h), a);
            double f_n1 = f(t - h, yList.get(i - 2), getYAt(t - h - tau, h), getYAt(t - h - tau2, h), a);
            double y = yList.get(i - 1) + h / 2 * (3 * f_n - f_n1);

            tList.add(t);
            yList.add(y);
        }
    }
    // Интерполяция значения y(tau) между точками
    private double getYAt(double t, double h) {
        if (t <= 0) return getPhi(t); // если t <= 0, возвращаем начальное условие
        int index = (int)(t / h);
        if (index >= yList.size() - 1) return yList.get(yList.size() - 1); // если вне сетки — последнее значение
        // Линейная интерполяция между ближайшими значениями
        double t1 = tList.get(index);
        double t2 = tList.get(index + 1);
        double y1 = yList.get(index);
        double y2 = yList.get(index + 1);
        return y1 + (y2 - y1) * (t - t1) / (t2 - t1);
    }
}


