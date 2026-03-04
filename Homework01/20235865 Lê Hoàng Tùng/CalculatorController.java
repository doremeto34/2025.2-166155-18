package test;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.text.Text; 

public class CalculatorController {
	
	private CalculatorModel model = new CalculatorModel();
	private long number1;
	private String operation;
	private boolean clearDisplay = false;
    @FXML
    private Text displayingText;
    
    @FXML
    void clearPressed(ActionEvent event) {
    	displayingText.setText("0");
    	operation="";
    }

    @FXML
    void decimalPressed(ActionEvent event) {

    }

    @FXML
    void equalsPressed(ActionEvent event) {
    	if(!operation.equals("") && !clearDisplay)
    		displayingText.setText(""+ model.calculate(number1, Long.parseLong(displayingText.getText()), operation));
    }
    
    @FXML
    void numberPressed(ActionEvent event) {
    	if(!clearDisplay) {
    		String value = ((Button)event.getSource()).getText();
    		if(displayingText.getText().equals("0")) {
    			if(value.equals("0"))return;
    			displayingText.setText(value);
    		}else
    		displayingText.setText(displayingText.getText() + value);
    	}else {
    		String value = ((Button)event.getSource()).getText();
    		displayingText.setText(value);
    		clearDisplay = false;
    	}
    }

    @FXML
    void operationPressed(ActionEvent event) {
    	operation = ((Button)event.getSource()).getText();
    	number1 = Long.parseLong(displayingText.getText());
    	clearDisplay = true;
    }

    @FXML
    void switchPressed(ActionEvent event) {

    }

    
}
