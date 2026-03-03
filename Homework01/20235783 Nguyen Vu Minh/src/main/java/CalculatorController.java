import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class CalculatorController {

    @FXML
    private TextField display;

    private final CalculatorModel model = new CalculatorModel();

    @FXML
    private void onDigit(ActionEvent event) {
        String digit = ((Button) event.getSource()).getText();
        display.setText(display.getText() + digit);
    }

    @FXML
    private void onOperator(ActionEvent event) {
        String operator = ((Button) event.getSource()).getText();
        String text = display.getText();
        int opIndex = findOperatorIndex(text);

        if (text.isEmpty()) {
            return;
        }

        if (opIndex != -1) {
            text = text.substring(0, opIndex) + operator;
        } else {
            text = text + operator;
        }

        display.setText(text);
    }

    @FXML
    private void onEquals(ActionEvent event) {
        String text = display.getText();
        int opIndex = findOperatorIndex(text);

        if (opIndex <= 0 || opIndex >= text.length() - 1) {
            return;
        }

        String left = text.substring(0, opIndex);
        String right = text.substring(opIndex + 1);
        String op = text.substring(opIndex, opIndex + 1);

        try {
            long number1 = Long.parseLong(left);
            long number2 = Long.parseLong(right);
            long result = model.calculate(number1, number2, op);
            display.setText(String.valueOf(result));
        } catch (NumberFormatException e) {
            display.setText("Error");
        }
    }

    @FXML
    private void onClear(ActionEvent event) {
        display.clear();
    }

    @FXML
    private void onBackspace(ActionEvent event) {
        String text = display.getText();
        if (text != null && !text.isEmpty()) {
            display.setText(text.substring(0, text.length() - 1));
        }
    }

    @FXML
    private void onLeftParen(ActionEvent event) {
    }

    @FXML
    private void onRightParen(ActionEvent event) {
    }

    private int findOperatorIndex(String text) {
        if (text == null) {
            return -1;
        }
        int index = -1;
        for (char op : new char[]{'+', '-', '*', '/', '−', '×', '÷'}) {
            int i = text.indexOf(op);
            if (i > 0) {
                index = i;
                break;
            }
        }
        return index;
    }
}

