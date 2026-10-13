package dde;

public record MethodErrorRow(
        String method,
        double maximumAbsolute,
        double maximumRelative,
        double rootMeanSquare,
        double finalAbsolute
) {
    public static MethodErrorRow from(String method, ErrorMetrics metrics) {
        return new MethodErrorRow(method, metrics.maximumAbsoluteError(), metrics.maximumRelativeError(),
                metrics.rootMeanSquareError(), metrics.finalAbsoluteError());
    }
}
