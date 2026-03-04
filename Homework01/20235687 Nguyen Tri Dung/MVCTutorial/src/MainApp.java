public class MainApp {
    
    public static void main(String[] args) {
    	
    	CalculatorView theView = new CalculatorView();
        
    	CalculatorModel theModel = new CalculatorModel();
        
        CalculatorController theController = new CalculatorController(theView,theModel);
        
        theView.setVisible(true);
        
    }
}