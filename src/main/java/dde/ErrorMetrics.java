package dde;

public record ErrorMetrics(
        double maximumAbsoluteError,
        double rootMeanSquareError,
        double maximumRelativeError,
        double finalAbsoluteError
) {
    private static final double RELATIVE_ERROR_FLOOR = 1e-12;

    public static ErrorMetrics compare(NumericalSolution numerical, ExactSolution exact) {
        if (numerical == null || exact == null) {
            throw new IllegalArgumentException("Решения для сравнения не должны быть null");
        }

        double maximumAbsolute = 0.0;
        double maximumRelative = Double.NaN;
        double squaredErrorSum = 0.0;
        double finalAbsolute = 0.0;

        for (int index = 0; index < numerical.size(); index++) {
            double expected = exact.valueAt(numerical.times().get(index));
            double absolute = Math.abs(numerical.values().get(index) - expected);
            maximumAbsolute = Math.max(maximumAbsolute, absolute);
            if (Math.abs(expected) > RELATIVE_ERROR_FLOOR) {
                double relative = absolute / Math.abs(expected);
                maximumRelative = Double.isNaN(maximumRelative)
                        ? relative
                        : Math.max(maximumRelative, relative);
            }
            squaredErrorSum += absolute * absolute;
            finalAbsolute = absolute;
        }

        return new ErrorMetrics(
                maximumAbsolute,
                Math.sqrt(squaredErrorSum / numerical.size()),
                maximumRelative,
                finalAbsolute
        );
    }
}
