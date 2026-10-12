package dde;

final class InputValidator {

    private static final double EPS = 1e-12;

    private InputValidator() {
    }

    static double parseFinite(String text, String fieldName) {
        String value = requireText(text, fieldName).replace(',', '.');
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) {
                throw new IllegalArgumentException("Поле «" + fieldName + "» должно содержать конечное число");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Поле «" + fieldName + "» должно содержать число, получено: " + text,
                    exception
            );
        }
    }

    static double parsePositive(String text, String fieldName) {
        double value = parseFinite(text, fieldName);
        if (value <= 0.0) {
            throw new IllegalArgumentException("Поле «" + fieldName + "» должно быть больше нуля");
        }
        return value;
    }

    static double parseNonNegative(String text, String fieldName) {
        double value = parseFinite(text, fieldName);
        if (value < 0.0) {
            throw new IllegalArgumentException("Поле «" + fieldName + "» не может быть отрицательным");
        }
        return value;
    }

    static String requireText(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Поле «" + fieldName + "» не заполнено");
        }
        return text.trim();
    }

    static void validateStep(double step, double firstDelay, double secondDelay) {
        if (step > firstDelay + EPS) {
            throw new IllegalArgumentException("Шаг h не должен превышать задержку τ");
        }
        if (secondDelay > 0.0 && step > secondDelay + EPS) {
            throw new IllegalArgumentException("Шаг h не должен превышать задержку τ₂");
        }
    }
}
