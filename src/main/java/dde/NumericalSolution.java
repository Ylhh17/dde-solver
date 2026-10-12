package dde;

import java.util.List;
import java.util.Objects;

public record NumericalSolution(
        DDESolver.Method method,
        List<Double> times,
        List<Double> values
) {
    public NumericalSolution {
        Objects.requireNonNull(method, "Численный метод не должен быть null");
        times = List.copyOf(Objects.requireNonNull(times, "Сетка не должна быть null"));
        values = List.copyOf(Objects.requireNonNull(values, "Значения не должны быть null"));
        if (times.isEmpty()) {
            throw new IllegalArgumentException("Решение должно содержать хотя бы одну точку");
        }
        if (times.size() != values.size()) {
            throw new IllegalArgumentException("Размеры сетки и значений должны совпадать");
        }
        for (int index = 0; index < times.size(); index++) {
            if (!Double.isFinite(times.get(index)) || !Double.isFinite(values.get(index))) {
                throw new IllegalArgumentException("Решение содержит неконечное число");
            }
            if (index > 0 && times.get(index) <= times.get(index - 1)) {
                throw new IllegalArgumentException("Временная сетка должна строго возрастать");
            }
        }
    }

    public int size() {
        return times.size();
    }

    public double finalTime() {
        return times.getLast();
    }

    public double finalValue() {
        return values.getLast();
    }
}
