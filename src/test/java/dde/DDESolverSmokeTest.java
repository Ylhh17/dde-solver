package dde;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DDESolverSmokeTest {

    @ParameterizedTest
    @EnumSource(DDESolver.Method.class)
    void zeroDerivativeKeepsConstantSolution(DDESolver.Method method) {
        DDESolver solver = new DDESolver();
        solver.setInitialFunctionExpression("2");
        solver.setExpression("0");

        solver.solve(method, 0.0, 1.0, 0.1, 2.0);

        List<Double> times = solver.getTList();
        List<Double> values = solver.getYList();

        assertAll(
                () -> assertFalse(times.isEmpty()),
                () -> assertEquals(times.size(), values.size()),
                () -> assertEquals(0.0, times.getFirst(), 1e-12),
                () -> values.forEach(value -> assertEquals(2.0, value, 1e-12))
        );
    }
}
