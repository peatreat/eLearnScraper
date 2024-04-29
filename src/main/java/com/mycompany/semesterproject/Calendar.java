package com.mycompany.semesterproject;

public class Calendar extends Menu {
    private eLearn elearn;
    
    public Calendar(eLearn elearn) {
        super("Events for Today", "Events for the Week", "Events for the Month", "All Upcoming Events", "Go Back");
        this.elearn = elearn;
    }
    
    public void processMenu() {
        int option;
        
        // Loop until the user selects "Go Back"
        do {
            this.displayMenu();
            option = this.getSelection();
            System.out.println();
            
            switch (option) {
                case 1 -> {
                    displayCalendar(System.currentTimeMillis(), System.currentTimeMillis() + (1000L * 60 * 60 * 24)); // Events for today
                }
                case 2 -> {
                    displayCalendar(System.currentTimeMillis(), System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 7)); // Events for the week
                }
                case 3 -> {
                    displayCalendar(System.currentTimeMillis(), System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 31)); // Events for the month
                }
                case 4 -> {
                    displayCalendar(System.currentTimeMillis(), 0); // All upcoming events
                }
            }
        } while (option != this.options.length);
    }
    
    /*
        @minTime The requested time in milliseconds to start showing calendar events from
        @maxTime The requested time in milliseconds to stop showing calendar events from
    */
    private void displayCalendar(long minTime, long maxTime) {
        // Make sure student is authorized on the API before proceeding
        if (!elearn.Authorize()) {
            System.out.println("Error: Failed to get authorization token");
            return;
        }

        Schedule schedule = new Schedule(minTime, maxTime); // Initialize a Schedule object
         
        // Iterate the course(s) the student selected
        for (int i = 0; i < elearn.courses.size(); i++) {
            String calendarUrl = String.format(
                "https://elearn.volstate.edu/d2l/api/le/1.67/%s/calendar/events/",
                 elearn.courses.get(i).id
            );

            // Make a GET request to the API endpoint responsible for showing all the calendar events for the student's selected course
            var calendarEvents = Networking.getJSONArray(calendarUrl, "Authorization", "Bearer " + elearn.auth.token);

            // Parse the JSON array and add each calendar event to the schedule
            schedule.addCalendar(calendarEvents);
        }
        
        // If schedule does not have any events stored, then stop the function here
        if (schedule.items.isEmpty()) {
            System.out.println("There are no calendar events available for your courses");
            return;
        }
        
        System.out.printf("--------------------------------------------------------------%n");
        System.out.printf("*                       Calendar Events                      *%n");
        System.out.printf("--------------------------------------------------------------%n");
        System.out.printf("| %-30s | %-25s |%n", "            Event", "     Available Until");
        System.out.printf("--------------------------------------------------------------%n");

        // Iterate through the schedule and display each calendar event
        schedule.items.forEach((deadline, list) -> {
            String deadlineTimestamp = Schedule.epochToTimestamp(deadline); // Get the deadline of the calendar events as a timestamp string in local time

            // Iterate the list of events stored for the current calendar deadline and display them
            list.forEach((event) -> {
                // Display the calendar event
                String title = event.getTitle();
                
                // Cut off title if too long
                if (title.length() > 30)
                    title = title.substring(0, 27).concat("...");
                
                System.out.printf("| %-30s | %-25s |%n", title, deadlineTimestamp);
            });
        });      
        
        System.out.printf("--------------------------------------------------------------%n");
        
        System.out.println();
    }
}
