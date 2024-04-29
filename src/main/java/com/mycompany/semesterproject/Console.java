package com.mycompany.semesterproject;

import java.util.Scanner;

public class Console {
    private static final Scanner sc = new Scanner(System.in); // Scanner object for reading console input
    
    /*
        @prompt The prompt to display to the user before reading input
        @return The user's input as a string
    */
    public static String getString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine();
            
            if (input.isEmpty()) {
                System.out.println("Error! This entry is required.");
                continue;
            }
            
            return input;
        }
    }
    
    /*
        @prompt The prompt to display to the user before reading input
        @return The user's input as an integer
    */
    public static int getInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            
            try {
                return Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException ex) {
                System.out.println("Error! Please enter a valid number.");
            }
        }
    }
    
    /*
        @prompt The prompt to display to the user before reading input
        @min The minimum value the user can input
        @max The maximum value the user can input
    
        @return The user's input as an integer
    */
    public static int getInt(String prompt, int min, int max) {
        while (true) {
            int input = getInt(prompt);
            
            if (input < min || input > max) {
                System.out.printf("Error! Number must be from %d to %d.\n", min, max);
                continue;
            }
            
            return input;
        }
    }
}
