public class CalculatorModel {

    public long calculate(long number1, long number2, String operation) {
        switch (operation) {
            case "+":
                return number1 + number2;
            case "-":
            case "−":
                return number1 - number2;
            case "*":
            case "×":
                return number1 * number2;
            case "/":
            case "÷":
                if (number2 == 0) {
                    return 0;
                }
                return number1 / number2;
            default:
                return 0;
        }
    }
}
