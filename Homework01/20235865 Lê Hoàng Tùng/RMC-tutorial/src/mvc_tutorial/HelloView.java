package mvc_tutorial;

import java.util.Scanner;

public class HelloView {
    public static void main(String[] args) {
        HelloModel model = new HelloModel();
        HelloController controller = new HelloController(model);

        try (Scanner sc = new Scanner(System.in)) {
            System.out.print("Enter your name: ");
            String name = sc.nextLine();
            System.out.println(controller.handleGreet(name));
        }
    }
}