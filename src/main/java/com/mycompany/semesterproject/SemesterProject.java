package com.mycompany.semesterproject;

public class SemesterProject {
    public static void main(String[] args) {
        var eLearn = new eLearn("session.txt"); // Initialize eLearn object
        boolean loggedIn = eLearn.Load(); // Attempt to load session cookie from disk
        
        // Loop until successful login
        while (!loggedIn) {
            String username = Console.getString("Username: "); // Get the student's volstate username
            String password = Console.getString("Password: "); // Get the student's volstate password
            
            // If they enter their whole email, then only use the part before the "@"
            if (username.contains("@")) {
                username = username.split("@")[0];
            }
            
            loggedIn = eLearn.Login(username, password); // Attempt to login with the provided credentials
        }
        
        System.out.println("Successfully logged in!\n");
        
        // Display main menu
        eLearn.processMenu();
    }
}
