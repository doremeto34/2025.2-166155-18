import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class CalculatorController {

    @FXML
    private TextField firstNumber;

    @FXML
    private TextField secondNumber;

    @FXML
    private TextField calcSolution;

    private CalculatorModel model = new CalculatorModel();

    @FXML
    private void handleCalculate() {

        try {
            int num1 = Integer.parseInt(firstNumber.getText());
            int num2 = Integer.parseInt(secondNumber.getText());

            model.addTwoNumbers(num1, num2);

            calcSolution.setText(String.valueOf(model.getCalculationValue()));

        } catch (NumberFormatException e) {

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("You must enter 2 integers!");
            alert.showAndWait();
        }
    }
}