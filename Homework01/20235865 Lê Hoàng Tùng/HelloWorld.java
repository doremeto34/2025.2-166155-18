package hello;

import java.util.Scanner;

public class HelloWorld {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter your name: ");
        String name = sc.nextLine().trim();

        if (name.isEmpty()) 
            System.out.println("Hello!");
        else 
            System.out.println("Hello " + name + "!");

        sc.close();
    }
}