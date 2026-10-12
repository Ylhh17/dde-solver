package dde;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public final class ExactSolution {
    private final String expressionText;
    private final Expression expression;

    public ExactSolution(String expressionText, double a, double b, double tau, double tau2) {
        if (expressionText == null || expressionText.isBlank()) {
            throw new IllegalArgumentException("Точное решение не может быть пустым");
        }
        requireFinite(a, "a");
        requireFinite(b, "b");
        requireFinite(tau, "τ");
        requireFinite(tau2, "τ₂");
        this.expressionText = expressionText.trim();
        try {
            this.expression = new ExpressionBuilder(this.expressionText)
                    .variables("t", "a", "b", "tau", "tau2")
                    .build()
                    .setVariable("a", a)
                    .setVariable("b", b)
                    .setVariable("tau", tau)
                    .setVariable("tau2", tau2);
            valueAt(0.0);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Некорректное точное решение: " + this.expressionText, exception);
        }
    }

    public double valueAt(double t) {
        requireFinite(t, "t");
        try {
            double value = expression.setVariable("t", t).evaluate();
            requireFinite(value, "Значение точного решения");
            return value;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Не удалось вычислить точное решение '" + expressionText + "' при t=" + t,
                    exception
            );
        }
    }

    public String expressionText() {
        return expressionText;
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " должно быть конечным числом");
        }
    }
}
