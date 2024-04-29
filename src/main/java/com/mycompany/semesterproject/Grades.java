package com.mycompany.semesterproject;

import java.text.NumberFormat;

public class Grades extends Menu {
    private eLearn elearn;
    
    public Grades(eLearn elearn) {
        super("Display Grades by Grade", "Display Grades by Date", "Go Back");
        this.elearn = elearn;
    }
    
    @Override
    public void processMenu() {
        int option;
        
        // Loop until "Go Back" is selected
        do {
            this.displayMenu();
            option = this.getSelection();
            System.out.println();
            
            switch (option) {
                case 1 -> {
                    displayGrades(false); // Display grades by Grade
                }
                case 2 -> {
                    displayGrades(true); // Display grades by Date
                }
            }
        } while (option != this.options.length);
    } 
    
    private void displayGrades(boolean sortByDate) {
        // Make sure student is API authorized before proceeding
        if (!elearn.Authorize()) {
            System.out.println("Error: Failed to get authorization token");
            return;
        }
        
        Schedule schedule = new Schedule(0, 0); // Initialize schedule object
         
        for (int i = 0; i < elearn.courses.size(); i++) {
            String gradesUrl = String.format(
                "https://elearn.volstate.edu/d2l/api/le/1.67/%s/grades/values/%s/",
                 elearn.courses.get(i).id,
                 elearn.auth.userId
            );

            // Make a GET request to the API endpoint responsible for showing all the student's grades for the specified course
            var gradesJSON = Networking.getJSONArray(gradesUrl, "Authorization", "Bearer " + elearn.auth.token);

            // Add all the student's grades for the course to the schedule object
            schedule.addGrades(gradesJSON, sortByDate);
        }
        
        // If our schedule does not have any items stored, then end the function here
        if (schedule.items.isEmpty()) {
            System.out.println("There are no grades available for your courses");
            return;
        }

        // Initialize our percent formatter object
        var percentFormatter = NumberFormat.getPercentInstance();
        percentFormatter.setMinimumFractionDigits(2);

        System.out.printf("------------------------------------------------------------------------%n");
        System.out.printf("*                                Grades                                *%n");
        System.out.printf("------------------------------------------------------------------------%n");
        System.out.printf("| %-30s | %-25s | %-7s |%n", "             Name", "       Date Graded", " Grade");
        System.out.printf("------------------------------------------------------------------------%n");
 
        // Iterate through each item in the schedule
        schedule.items.forEach((number, list) -> {
            // number is either the grade if sortByDate is false, or it is the epoch of when it was graded if sortByDate is true.
            // Iterate through each item in the list
            list.forEach((item) -> {
                String title = item.getTitle(); // Grade Name
                
                // Cut off title if too long
                if (title.length() > 30)
                    title = title.substring(0, 27).concat("...");                
                
                // Get the grade and divide by 10,000 because we multiplied by 10,000 when stored the grade as a long to keep precision
                // Grade will be in item.epoch if sorting by date, or it will be the key this map pair, "number", if sorting by grade.
                double grade = (sortByDate ? item.epoch : number) / 10000.0;
                String timestamp = sortByDate ? Schedule.epochToTimestamp(number) : Schedule.epochToTimestamp(item.epoch); // Get the timestamp of when it was graded
                
                // Display the grade
                System.out.printf("| %-30s | %-25s | %-7s |%n", title, timestamp, percentFormatter.format(grade));
            });
        });
        
        System.out.printf("------------------------------------------------------------------------%n");
        
        System.out.println();
    }
}
