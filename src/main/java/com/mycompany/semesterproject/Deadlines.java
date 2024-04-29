package com.mycompany.semesterproject;

public class Deadlines extends Menu {
    private eLearn elearn;
    
    public Deadlines(eLearn elearn) {
        super("Deadlines for Today", "Deadlines for the Week", "Deadlines for the Month", "All Upcoming Deadlines", "Go Back");
        this.elearn = elearn;
    }
    
    @Override
    public void processMenu() {
        int option;
        
        // Loop until the user selects "Go Back"
        do {
            this.displayMenu();
            option = this.getSelection();
            System.out.println();
            
            switch (option) {
                case 1 -> {
                    displayDeadlines(System.currentTimeMillis(), System.currentTimeMillis() + (1000L * 60 * 60 * 24)); // Deadlines for today
                }
                case 2 -> {
                    displayDeadlines(System.currentTimeMillis(), System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 7)); // Deadlines for the week
                }
                case 3 -> {
                    displayDeadlines(System.currentTimeMillis(), System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 31)); // Deadlines for the month
                }
                case 4 -> {
                    displayDeadlines(System.currentTimeMillis(), 0); // All upcoming deadlines
                }
            }
        } while (option != this.options.length);
    }
    
    private void displayDeadlines(long minTime, long maxTime) {
        // Make sure the student is authorized on the API before proceeding
        if (!elearn.Authorize()) {
            System.out.println("Error: Failed to get authorization token");
            return;
        }
        
        Schedule schedule = new Schedule(minTime, maxTime); // Initialize Schedule object
         
        // Iterate through selected courses
        for (int i = 0; i < elearn.courses.size(); i++) {
            String assignmentsUrl = String.format(
                "https://cfd2be83-bc1c-4a43-8ac3-469bc19bfc4a.sequences.api.brightspace.com/%s?deepEmbedEntities=1&embedDepth=1&filterOnDatesAndDepth=0",
                 elearn.courses.get(i).id
            );

            // Make a GET request to the API endpoint responsible for listing all the student's assignments for the specified course
            var assignmentsJSON = Networking.getJSON(assignmentsUrl, "Authorization", "Bearer " + elearn.auth.token);

            // Parse the JSON object of assignments and add them to the schedule
            schedule.addAssignments(assignmentsJSON);
        }
        
        // If the schedule is empty, then return here
        if (schedule.items.isEmpty()) {
            System.out.println("There are no deadlines available for your courses");
            return;
        }
        
        System.out.printf("------------------------------------------------------------------------------------------%n");
        System.out.printf("*                                       Deadlines                                        *%n");
        System.out.printf("------------------------------------------------------------------------------------------%n");
        System.out.printf("| %-30s | %-25s | %-25s |%n", "          Assignment", "        Deadline", "        Submitted");
        System.out.printf("------------------------------------------------------------------------------------------%n");

        // Iterate through the schedule's items
        schedule.items.forEach((deadline, list) -> {
            String deadlineTimestamp = Schedule.epochToTimestamp(deadline); // Get the deadline timestamp

            // Iterate through all the assignments that share the current deadline and display them
            list.forEach((assignment) -> {
                String title = assignment.getTitle(); // Assignment Name
                
                // Cut off title if too long
                if (title.length() > 30)
                    title = title.substring(0, 27).concat("...");        
                
                System.out.printf("| %-30s | %-25s | %-25s |%n", title, deadlineTimestamp, assignment.isSubmitted() ? assignment.getEpochTimestamp() : "N/A");
            });
        });
        
        System.out.printf("------------------------------------------------------------------------------------------%n");
        
        System.out.println();
    }
}
