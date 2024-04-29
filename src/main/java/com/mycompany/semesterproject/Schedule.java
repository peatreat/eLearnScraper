package com.mycompany.semesterproject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;

class Assignment {
    public String title; // The name of the assignment
    public Long epoch; // An epoch timestamp for when it was submitted
    
    public Assignment(String title, Long epoch) {
        this.title = title;
        this.epoch = epoch;
    }
    
    // @return The title of the assignment
    String getTitle() {
        return this.title;
    }
    
    // @return True if this.epoch is not null, false otherwise
    boolean isSubmitted() {
        return this.epoch != null;
    }
    
    // @return The timestamp string of this.epoch
    String getEpochTimestamp() {
        if (!this.isSubmitted())
            return null;
        
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        return df.format(new Date(this.epoch));
    }
}

public class Schedule {
    public TreeMap<Long, List<Assignment>> items; // A TreeMap that sorts entries by the key
    private long minTime, maxTime; // The minimum epoch and maxmimum epoch time for an item to be stored
    
    public Schedule(long minTime, long maxTime) {
        this.items = new TreeMap<>();
        this.minTime = minTime;
        this.maxTime = maxTime;
    }
    
    /*
        @json The JSON array object containing all the student's grades for a course
        @sortByDate Whether or not to sort by date. If false, then it will sort by grade.
    */
    public void addGrades(JSONArray json, boolean sortByDate) {
        if (json == null) return;
        
        // Loop through each grade
        for (var i = 0; i < json.length(); i++) {
            var obj = json.getJSONObject(i); // The current grade object
            
            String name = obj.getString("GradeObjectName"); // The name of the grade
            double numerator = obj.getDouble("PointsNumerator"); // The points the student received
            double denominator = obj.getDouble("PointsDenominator"); // The total points for the grade possible
            Long dateEpoch = Schedule.timestampToEpoch(obj.getString("LastModified"), "yyyy-MM-dd'T'HH:mm:ss", false); // The numeric epoch of the timestamp when this was graded
            Long gradeKey = (long)((numerator / denominator) * 10000); // The grade multiplied by 10,000 and stored as a long. It is multiplied by 10,000 because longs can't have decimal points and I want to keep some precision.
            
            // If sorting by date, then the key in the map will be the date epoch, but if sorting by grade then the key in the map will be the grade * 10,000 as a long.
            
            var gradeList = items.getOrDefault(sortByDate ? dateEpoch : gradeKey, new ArrayList<>()); // Get the grade list from the map, or construct one if not found

            gradeList.add(new Assignment(name, sortByDate ? gradeKey : dateEpoch)); // Add the current grade to our list

            items.put(sortByDate ? dateEpoch : gradeKey, gradeList); // Put the list into our map
        }
    }
    
    /*
        @json The JSON array object containing all the student's calendar events for a course
    */
    public void addCalendar(JSONArray json) {
        if (json == null) return;
        
        // Loop through each calendar event
        for (var i = 0; i < json.length(); i++) {
            var event = json.getJSONObject(i); // The current calendar event
            
            // Add the calendar event if it has an end date
            if (event.has("EndDateTime")) {
                var title = event.getString("Title"); // Get the title of the current calendar event
                var endEpoch = this.timestampToEpoch(event.getString("EndDateTime"), "yyyy-MM-dd'T'HH:mm:ss", false); // Get the numeric epoch of the end date time
                
                // If the end time is in our minTime and maxTime range, then add it to our schedule
                if (endEpoch >= minTime && (maxTime == 0 || endEpoch <= maxTime)) {
                    var calendarList = items.getOrDefault(endEpoch, new ArrayList<>()); // Get the calendar list for all events that have this endEpoch, or construct a new list if one isn't found

                    calendarList.add(new Assignment(title, null)); // Add the calendar event to the list

                    items.put(endEpoch, calendarList); // Put the list in our map
                }
            }
        }
    }
    
    /*
        @json The JSON object containing the submitted date for an assignment
        @return If submitted then it will return the epoch of when the assignment was submitted. If not submitted, then it will return null.
    */
    Long getSubmittedDate(JSONObject json) {
        // If the current object has field "class" then it may contain the submission date
        if (json.has("class")) {
            var classification = json.getJSONArray("class");
            boolean isCompletion = false, hasDate = false;
            
            for (int i = 0; i < classification.length() && (!isCompletion || !hasDate); i++) {
                var type = classification.getString(i);
                
                if (type.equals("completion"))
                    isCompletion = true;
                
                if (type.equals("date"))
                    hasDate = true;
            }
            
            // If isCompletion is true and hasDate is true then we found the submittion date for the assignment
            if (isCompletion && hasDate) {
                String completedTimestamp = json.getJSONObject("properties").getString("date"); // Get the string timestamp for the submission
                
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                return this.timestampToEpoch(completedTimestamp, "yyyy-MM-dd'T'HH:mm:ss", false); // Return the numeric epoch parsed from the timestamp string
            }
        }
        
        // Find the submittion date by recursing
        
        if (json.has("entities")) {
            var entities = json.getJSONArray("entities");
        
            for (int i = 0; i < entities.length(); i++) {
                Long date = getSubmittedDate(entities.getJSONObject(i));
                
                if (date != null)
                    return date;
            }
        }
        
        return null;
    }
    
    /*
        @timestamp The string timestamp to be converted to a numeric epoch value
        @format The format of the string timestamp
        @bypassTimezoneConversion It will ignore the local timezone conversion when parsing the timestamp string
    
        @return A Long epoch value for the string timestamp, or null if it failed parsing the string.
    */
    public static Long timestampToEpoch(String timestamp, String format, boolean bypassTimzoneConversion) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(format);
            
            if (!bypassTimzoneConversion) {
                formatter.setTimeZone(TimeZone.getTimeZone(java.util.Calendar.getInstance().getTimeZone().getDisplayName())); // Set formatter timezone to local timezone
            }
            
            Date date = formatter.parse(timestamp);
            return date.getTime();   
        } catch (ParseException ex) {
            System.out.println("Failed to determine epoch timestamp for " + timestamp);
        }
        
        return null;
    }
    
    /*
        @epoch The numeric epoch value of a date
        @return A string timestamp for the provided epoch. If the provided epoch was null, then "N/A" is returned.
    */
    public static String epochToTimestamp(Long epoch) {
        if (epoch == null)
            return "N/A";
        
        DateFormat format = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");        
        return format.format(new Date(epoch));
    }
    
    /*
        @json The JSON object containing all the student's assignments for a specified course
    */
    public void addAssignments(JSONObject json) {
        if (json == null) return;
        
        if (json.has("properties")) {
            JSONObject properties = json.getJSONObject("properties");

            if (properties.has("dueDate")) {
                String title = properties.getString("title");
                JSONObject dueDate = properties.getJSONObject("dueDate");
                
                int year = dueDate.getInt("Year");
                int month = dueDate.getInt("Month");
                int day = dueDate.getInt("Day");
                int hour = dueDate.getInt("Hour");
                int minute = dueDate.getInt("Minute");
                int second = dueDate.getInt("Second");

                // Construct a timestamp string using the provided fields in the json object
                var timestamp = String.format("%02d/%02d/%04d %02d:%02d:%02d", month, day, year, hour, minute, second);
                
                // Get the epoch value of the timestamp that was constructed
                // We will bypass the local timezone conversion here because this API endpoint already provided the date in local time
                long epoch = this.timestampToEpoch(timestamp, "MM/dd/yyyy HH:mm:ss", true);   

                // Only store the assignment if the deadline is within the minTime and maxTime range
                // If maxTime is 0, then the maxTime limit is ignored
                if (epoch >= minTime && (maxTime == 0 || epoch <= maxTime)) {                    
                    var assignmentList = items.getOrDefault(epoch, new ArrayList<>()); // Get the entry in the map for the current epoch, or construct one if one isn't found

                    Long completedDate = getSubmittedDate(json); // Get the submittion date of the current assignment
                    assignmentList.add(new Assignment(title, completedDate)); // Add the assignment to the assignment list

                    items.put(epoch, assignmentList); // Put the assignmentList into the map using the deadline epoch as a key
                }
            }
        }
        
        // Recurse all objects to find all assignments because the provided assignment list from the API is not in a single array
        
        if (!json.has("entities"))
            return;
        
        var entities = json.getJSONArray("entities");
        
        for (int i = 0; i < entities.length(); i++) {
            addAssignments(entities.getJSONObject(i));
        }
    }
}