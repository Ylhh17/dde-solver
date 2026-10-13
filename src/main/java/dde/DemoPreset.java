package dde;

import java.util.List;

public record DemoPreset(
        String name,
        String equationType,
        String a,
        String b,
        String tau,
        String tau2,
        String step,
        String endTime,
        String history,
        String exact
) {
    private static final String CLASSIC = "Классика: ẏ(t) = a·y(t-τ)";
    private static final String STATE = "Состояние: ẏ(t) = a·y(t-τ) + b·y(t)";
    private static final String TWO_DELAYS = "Две задержки: ẏ(t) = a·y(t-τ₁) + b·y(t-τ₂)";

    public static List<DemoPreset> builtIn() {
        double lambda = 0.3;
        double tau1 = 0.8;
        double tau2 = 0.35;
        double b = -0.2;
        double stateA = (lambda - b) * Math.exp(lambda * tau1);
        double twoDelayA = (lambda - b * Math.exp(-lambda * tau2)) / Math.exp(-lambda * tau1);

        return List.of(
                new DemoPreset("Базовая: затухающие колебания", CLASSIC,
                        "-1", "0", "1", "0.5", "0.05", "10", "1", ""),
                new DemoPreset("Точная: одна задержка", CLASSIC,
                        Double.toString(0.4 * Math.exp(0.4)), "0", "1", "0.5", "0.05", "2",
                        "exp(0.4*t)", "exp(0.4*t)"),
                new DemoPreset("Точная: задержка и состояние", STATE,
                        Double.toString(stateA), Double.toString(b), Double.toString(tau1), "0.5",
                        "0.04", "2", "exp(0.3*t)", "exp(0.3*t)"),
                new DemoPreset("Точная: две задержки", TWO_DELAYS,
                        Double.toString(twoDelayA), Double.toString(b), Double.toString(tau1),
                        Double.toString(tau2), "0.035", "2", "exp(0.3*t)", "exp(0.3*t)")
        );
    }

    @Override
    public String toString() {
        return name;
    }
}
