package dde;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DDESolverTest {

    @ParameterizedTest
    @EnumSource(DDESolver.Method.class)
    void zeroDerivativeKeepsConstantSolution(DDESolver.Method method) {
        DDESolver solver = solver("2", "0");
        solver.solve(method, 0.0, 1.0, 0.1, 2.0);

        assertAll(
                () -> assertFalse(solver.getTList().isEmpty()),
                () -> assertEquals(solver.getTList().size(), solver.getYList().size()),
                () -> assertEquals(0.0, solver.getTList().getFirst(), 1e-12),
                () -> solver.getYList().forEach(value -> assertEquals(2.0, value, 1e-12))
        );
    }

    @ParameterizedTest
    @EnumSource(DDESolver.Method.class)
    void historyIsUsedForNegativeDelayedArguments(DDESolver.Method method) {
        DDESolver solver = solver("2", "y_tau1");
        solver.solve(method, 1.0, 1.0, 0.1, 0.5);
        assertEquals(3.0, solver.getYList().getLast(), 1e-10);
    }

    @Test
    void rungeKuttaSolvesCurrentStateEquation() {
        DDESolver solver = solver("1", "y");
        solver.solve(DDESolver.Method.RUNGE_KUTTA_4, 0.0, 1.0, 0.1, 1.0);
        assertEquals(Math.E, solver.getYList().getLast(), 3e-6);
    }

    @Test
    void solutionEndsExactlyAtRequestedTime() {
        DDESolver solver = solver("1", "0");
        solver.solve(DDESolver.Method.EULER, 0.0, 1.0, 0.3, 1.0);
        assertEquals(1.0, solver.getTList().getLast(), 1e-12);
    }

    @Test
    void rejectsStepGreaterThanDelay() {
        DDESolver solver = solver("1", "y_tau1");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> solver.solve(DDESolver.Method.RUNGE_KUTTA_4, 1.0, 0.1, 0.2, 1.0)
        );
        assertTrue(exception.getMessage().contains("Шаг"));
    }

    @Test
    void rejectsInvalidExpressionBeforeCalculation() {
        DDESolver solver = new DDESolver();
        assertThrows(IllegalArgumentException.class, () -> solver.setExpression("a @ y"));
        assertThrows(IllegalArgumentException.class, () -> solver.setInitialFunctionExpression(""));
    }

    @Test
    void returnedListsCannotBeModifiedExternally() {
        DDESolver solver = solver("1", "0");
        solver.solve(DDESolver.Method.EULER, 0.0, 1.0, 0.1, 0.2);
        assertThrows(UnsupportedOperationException.class, () -> solver.getYList().add(5.0));
    }

    private static DDESolver solver(String history, String rightHandSide) {
        DDESolver solver = new DDESolver();
        solver.setInitialFunctionExpression(history);
        solver.setExpression(rightHandSide);
        return solver;
    }
}
