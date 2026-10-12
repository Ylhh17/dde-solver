package dde;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InputValidatorTest {

    @Test
    void acceptsDotAndCommaDecimalSeparators() {
        assertEquals(1.25, InputValidator.parseFinite("1.25", "a"));
        assertEquals(1.25, InputValidator.parseFinite("1,25", "a"));
    }

    @Test
    void rejectsEmptyAndNonNumericValues() {
        assertThrows(IllegalArgumentException.class, () -> InputValidator.parseFinite("", "a"));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.parseFinite("abc", "a"));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.parseFinite("NaN", "a"));
    }

    @Test
    void validatesPositiveAndNonNegativeFields() {
        assertThrows(IllegalArgumentException.class, () -> InputValidator.parsePositive("0", "h"));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.parseNonNegative("-1", "t max"));
        assertEquals(0.0, InputValidator.parseNonNegative("0", "t max"));
    }

    @Test
    void rejectsStepGreaterThanUsedDelay() {
        assertThrows(IllegalArgumentException.class, () -> InputValidator.validateStep(0.2, 0.1, 0.0));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.validateStep(0.2, 1.0, 0.1));
    }
}
