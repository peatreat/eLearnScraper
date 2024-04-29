package com.mycompany.semesterproject;

abstract public class Menu {
    protected String[] options; // An array of strings where each string represents an option in the menu
    
    public Menu(String... options) {
        this.options = options;
    }
    
    protected void displayMenu() {
        // Loop through each option and display it
        for (int i = 1; i <= options.length; i++)
            System.out.printf("[%d] %s\n", i, options[i-1]);
        
        System.out.println();
    }
    
    protected int getSelection() {     
        // Get the user's selection and return the option they selected
        return Console.getInt(String.format("Select Option [%d-%d]: ", 1, options.length), 1, options.length);
    }
    
    // A virtual method that gets overridden by child classes
    abstract public void processMenu();
}
