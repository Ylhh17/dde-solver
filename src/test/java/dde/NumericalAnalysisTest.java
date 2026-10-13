package dde;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumericalAnalysisTest {

    @ParameterizedTest
    @EnumSource(DDESolver.Method.class)
    void manufacturedOneDelaySolutionIsAccurate(DDESolver.Method method) {
        double lambda = 0.4;
        double tau = 0.75;
        DDESolver solver = exponentialSolver(lambda, tau);

        NumericalSolution solution = solver.solveResult(method, lambda * Math.exp(lambda * tau), tau, 0.025, 1.5);
        ErrorMetrics metrics = ErrorMetrics.compare(
                solution,
                new ExactSolution("exp(0.4*t)", lambda * Math.exp(lambda * tau), 0.0, tau, 0.0)
        );

        double tolerance = method == DDESolver.Method.EULER ? 0.012 : 0.002;
        assertTrue(metrics.maximumAbsoluteError() < tolerance,
                () -> method + ": max error = " + metrics.maximumAbsoluteError());
    }

    @ParameterizedTest
    @EnumSource(DDESolver.Method.class)
    void manufacturedTwoDelaySolutionIsAccurate(DDESolver.Method method) {
        double lambda = 0.3;
        double tau1 = 0.8;
        double tau2 = 0.45;
        double b = 0.35;
        double a = (lambda - b * Math.exp(-lambda * tau2)) / Math.exp(-lambda * tau1);

        DDESolver solver = new DDESolver();
        solver.setInitialFunction(t -> Math.exp(lambda * t));
        solver.setTau2(tau2);
        solver.setExpression("a*y_tau1 + (" + b + ")*y_tau2");

        NumericalSolution solution = solver.solveResult(method, a, tau1, 0.025, 1.2);
        ExactSolution exact = new ExactSolution("exp(0.3*t)", a, b, tau1, tau2);
        ErrorMetrics metrics = ErrorMetrics.compare(solution, exact);

        double tolerance = method == DDESolver.Method.EULER ? 0.008 : 0.002;
        assertTrue(metrics.maximumAbsoluteError() < tolerance,
                () -> method + ": max error = " + metrics.maximumAbsoluteError());
    }

    @Test
    void eulerShowsFirstOrderConvergence() {
        double coarse = finalError(DDESolver.Method.EULER, 0.1);
        double fine = finalError(DDESolver.Method.EULER, 0.05);
        double observedOrder = Math.log(coarse / fine) / Math.log(2.0);

        assertTrue(observedOrder > 0.85 && observedOrder < 1.15,
                () -> "Наблюдаемый порядок Эйлера: " + observedOrder);
    }

    @Test
    void adamsBashforthShowsSecondOrderConvergence() {
        double coarse = finalError(DDESolver.Method.ADAMS_BASHFORTH_2, 0.1);
        double fine = finalError(DDESolver.Method.ADAMS_BASHFORTH_2, 0.05);
        double observedOrder = Math.log(coarse / fine) / Math.log(2.0);

        assertTrue(observedOrder > 1.75 && observedOrder < 2.25,
                () -> "Наблюдаемый порядок Адамса-Бэшфорта 2: " + observedOrder);
    }

    @Test
    void rk4ShowsFourthOrderConvergenceBeforeFirstDelay() {
        double coarse = finalError(DDESolver.Method.RUNGE_KUTTA_4, 0.2);
        double fine = finalError(DDESolver.Method.RUNGE_KUTTA_4, 0.1);
        double observedOrder = Math.log(coarse / fine) / Math.log(2.0);

        assertTrue(observedOrder > 3.75 && observedOrder < 4.25,
                () -> "Наблюдаемый порядок RK4: " + observedOrder);
    }

    @Test
    void rk4IsMoreAccurateThanEulerOnSmoothManufacturedProblem() {
        double eulerError = finalError(DDESolver.Method.EULER, 0.1);
        double rk4Error = finalError(DDESolver.Method.RUNGE_KUTTA_4, 0.1);
        assertTrue(rk4Error < eulerError / 100.0,
                () -> "Euler=" + eulerError + ", RK4=" + rk4Error);
    }

    @Test
    void errorMetricsMatchKnownValues() {
        NumericalSolution numerical = new NumericalSolution(
                DDESolver.Method.EULER,
                List.of(0.0, 1.0),
                List.of(1.0, 4.0)
        );
        ErrorMetrics metrics = ErrorMetrics.compare(
                numerical,
                new ExactSolution("1 + 2*t", 0.0, 0.0, 1.0, 0.0)
        );

        assertEquals(1.0, metrics.maximumAbsoluteError(), 1e-12);
        assertEquals(Math.sqrt(0.5), metrics.rootMeanSquareError(), 1e-12);
        assertEquals(1.0 / 3.0, metrics.maximumRelativeError(), 1e-12);
        assertEquals(1.0, metrics.finalAbsoluteError(), 1e-12);
    }

    @Test
    void relativeErrorIgnoresExactZeros() {
        NumericalSolution numerical = new NumericalSolution(
                DDESolver.Method.EULER, List.of(0.0), List.of(1e-3));
        ErrorMetrics metrics = ErrorMetrics.compare(
                numerical, new ExactSolution("0", 0.0, 0.0, 1.0, 0.0));
        assertTrue(Double.isNaN(metrics.maximumRelativeError()));
    }

    @Test
    void exactExpressionSupportsParameters() {
        ExactSolution exact = new ExactSolution("a + b + tau + tau2 + t", 1.0, 2.0, 3.0, 4.0);
        assertEquals(10.5, exact.valueAt(0.5), 1e-12);
        assertThrows(IllegalArgumentException.class,
                () -> new ExactSolution("unknown(t)", 0.0, 0.0, 1.0, 0.0));
    }

    @Test
    void numericalSolutionIsImmutableAndValidatesGrid() {
        NumericalSolution solution = new NumericalSolution(
                DDESolver.Method.EULER, List.of(0.0, 0.1), List.of(1.0, 1.1));
        assertThrows(UnsupportedOperationException.class, () -> solution.values().add(2.0));
        assertThrows(IllegalArgumentException.class,
                () -> new NumericalSolution(DDESolver.Method.EULER, List.of(0.0, 0.0), List.of(1.0, 1.1)));
    }

    private static DDESolver exponentialSolver(double lambda, double tau) {
        DDESolver solver = new DDESolver();
        solver.setInitialFunction(t -> Math.exp(lambda * t));
        solver.setExpression("a*y_tau1");
        return solver;
    }

    private static double finalError(DDESolver.Method method, double h) {
        double lambda = 0.4;
        double tau = 1.0;
        DDESolver solver = exponentialSolver(lambda, tau);
        NumericalSolution solution = solver.solveResult(method, lambda * Math.exp(lambda * tau), tau, h, 1.0);
        return Math.abs(solution.finalValue() - Math.exp(lambda));
    }
}
