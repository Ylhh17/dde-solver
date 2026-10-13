package dde;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoPresetTest {

    @Test
    void containsFourValidBuiltInExamples() {
        List<DemoPreset> presets = DemoPreset.builtIn();
        assertEquals(4, presets.size());
        assertTrue(presets.stream().map(DemoPreset::name).distinct().count() == presets.size());

        for (DemoPreset preset : presets) {
            Main.CalculationInput input = Main.parseInput(
                    preset.equationType(), preset.a(), preset.b(), preset.tau(), preset.tau2(),
                    preset.step(), preset.endTime(), preset.history(), preset.exact());
            assertTrue(input.h() > 0.0);
            assertTrue(input.tMax() > 0.0);
        }
    }

    @Test
    void analyticalExamplesActuallySolveTheirEquations() {
        for (DemoPreset preset : DemoPreset.builtIn()) {
            if (preset.exact().isBlank()) {
                continue;
            }
            Main.CalculationInput input = Main.parseInput(
                    preset.equationType(), preset.a(), preset.b(), preset.tau(), preset.tau2(),
                    preset.step(), preset.endTime(), preset.history(), preset.exact());
            DDESolver solver = new DDESolver();
            solver.setTau2(input.tau2());
            solver.setInitialFunctionExpression(input.phi());
            solver.setExpression(input.expression());
            NumericalSolution solution = solver.solveResult(
                    DDESolver.Method.RUNGE_KUTTA_4, input.a(), input.tau(), input.h(), input.tMax());
            ErrorMetrics metrics = ErrorMetrics.compare(solution,
                    new ExactSolution(input.exact(), input.a(), input.b(), input.tau(), input.tau2()));
            assertTrue(metrics.maximumAbsoluteError() < 1e-4,
                    () -> preset.name() + ": " + metrics.maximumAbsoluteError());
        }
    }

    @Test
    void basicExampleDoesNotClaimAnExactSolution() {
        assertFalse(DemoPreset.builtIn().getFirst().exact().length() > 0);
    }
}
