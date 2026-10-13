package dde;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculationInputTest {

    @Test
    void classicInputKeepsOptionalExactExpression() {
        Main.CalculationInput input = Main.parseInput(
                "Классика: ẏ(t) = a·y(t-τ)", "0.5", "ignored", "1", "ignored",
                "0.1", "2", "exp(0.5*t)", "exp(0.5*t)"
        );

        assertEquals(0.0, input.b(), 1e-12);
        assertEquals(0.0, input.tau2(), 1e-12);
        assertEquals("exp(0.5*t)", input.exact());
        assertEquals("a * y_tau1", input.expression());
        assertFalse(input.usesSecondDelay());
    }

    @Test
    void twoDelayInputBuildsCorrectExpression() {
        Main.CalculationInput input = Main.parseInput(
                "Две задержки: ẏ(t) = a·y(t-τ₁) + b·y(t-τ₂)", "1", "-0.25", "1", "0.5",
                "0.1", "2", "1", ""
        );

        assertTrue(input.usesSecondDelay());
        assertEquals(0.5, input.tau2(), 1e-12);
        assertEquals("a * y_tau1 + (-0.25) * y_tau2", input.expression());
    }

    @Test
    void rejectsStepGreaterThanSecondDelay() {
        assertThrows(IllegalArgumentException.class, () -> Main.parseInput(
                "Две задержки: ẏ(t) = a·y(t-τ₁) + b·y(t-τ₂)", "1", "1", "1", "0.05",
                "0.1", "2", "1", ""
        ));
    }
}
