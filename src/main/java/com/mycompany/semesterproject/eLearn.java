package com.mycompany.semesterproject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

class Authorization {
    private long expiresAtSeconds; // The expiration of the current authorized session. It is represented in seconds since epoch
    public String token, userId; // The authorization token and student's numerical ID on d2l
    private String csrfToken; // csrfToken that can be found on the home page and is used for re-authorization
    
    public Authorization(long expires_at, String token, String csrfToken, String userId) {
        this.expiresAtSeconds = expires_at;
        this.token = token;
        this.csrfToken = csrfToken;
        this.userId = userId;
    }
    
    // @return Returns true if the current authorized session is expired, false otherwise.
    public boolean isExpired() {
        return (System.currentTimeMillis() / 1000) >= this.expiresAtSeconds;
    }
}

class Course {
    public String id, name;
    
    public Course(String id, String name) {
        this.id = id;
        this.name = name;
    }
}

// This menu is for selecting the course after login. It is only used one time.
class Courses extends Menu {
    public Courses(ArrayList<Course> courses) {
        super();
        
        String[] options = new String[courses.size() + 2];
        
        for (int i = 0; i < courses.size(); i++)
            options[i] = courses.get(i).name;
        
        // The last two options of this menu will always be "All Courses" and "Exit"
        options[options.length - 2] = "All Courses";
        options[options.length - 1] = "Exit";
        
        this.options = options;
    }
    
    @Override
    public void processMenu(){};
}

public class eLearn extends Menu {
    private String sessionFilename;
    private String cookie;
    protected Authorization auth; 
    private Menu deadlines, grades, calendar, courseSelector;
    protected ArrayList<Course> courses;
    
    // @sessionFilename This will be the file name used for when the student's session cookie is stored to disk.
    public eLearn(String sessionFilename) {
        super("Deadlines", "Grades", "Calendar", "Exit");
        this.courses = new ArrayList<>();
        this.sessionFilename = sessionFilename;
        this.deadlines = new Deadlines(this);
        this.grades = new Grades(this);
        this.calendar = new Calendar(this);
    }
    
    /*
        This will be the first thing called after the student is logged in
        It will first get the student's courses and store them in this.courses
        It will remove all but one from this.courses if the student chooses to only view one courses, however, if the student selects "All Courses" then this.courses contains all courses
    */
    @Override
    public void processMenu() {
        int option;
        
        if (getCourses()) {        
            this.courseSelector = new Courses(this.courses);
            
            this.courseSelector.displayMenu();
            int selected = this.courseSelector.getSelection();
            
            if (this.courseSelector.options[selected - 1].equals("Exit"))
                return;
            
            if (!this.courseSelector.options[selected - 1].equals("All Courses")) {
                Course selectedCourse = this.courses.get(selected - 1);
                this.courses.clear();
                this.courses.add(selectedCourse);
            }
        }
        
        // After the student selects their course(s), then they will be asked again whether they want to view Grades, Deadlines, Calendar, or Exit
        // This loop does not end until they select "Exit"
        do {
            this.displayMenu();
            option = this.getSelection();
            
            switch (option) {
                case 1 -> {
                    this.deadlines.processMenu();
                }
                case 2 -> {
                    this.grades.processMenu();
                }
                case 3 -> {
                    this.calendar.processMenu();
                }
            }
        } while (option != this.options.length);
    }
    
    /*
        @return Returns true if it read the session cookie from disk and the cookie is still valid.
    */
    public boolean Load() {
        String sessionCookie;
        
        // Attempt to read the session cookie from disk. If there's an exception then return false
        try (BufferedReader reader = new BufferedReader(new FileReader(sessionFilename))) {
            sessionCookie = reader.readLine();
        } catch (IOException ex) {
            return false;
        }

        // The session cookie was read from disk and stored in sessionCookie. Now we need to verify that it isn't expired by doing a GET request to any url that needs a valid cookie
        HttpResponse<String> response = Networking.Get("https://elearn.volstate.edu/d2l/lp/profile/profile_edit.d2l", "Cookie", sessionCookie);

        var headers = response.headers().map();
        var location = headers.get("Location");

        // If the response does not try to redirect me or the redirection url does not contain "sessionExpired" then the cookie is good
        if (location.isEmpty() || !location.get(0).contains("sessionExpired")) {
            cookie = sessionCookie;
            return true;
        }         
        
        return false;
    }
    
    /*
    *   Will save the session cookie to disk so that next time the student can login without having to re-enter login credentials.
    *   Cookies expire after some time, so I preferred to store the cookie to disk rather than the student's login credentials.
    */
    private void saveSession() {
      // Store this.cookie in a text file on disk
      try (FileWriter output = new FileWriter(sessionFilename)) {
            output.write(cookie);
        } catch (IOException e) {
        System.out.println("Failed to save session");
      }
    }
    
    /*
        @return Returns true if it successfully re-authorized the student through the API and stored the new access token.
    */
    protected boolean Authorize() {
        // If auth is already set and it isn't expired, then we don't need to re-authorize, so return true.
        if (this.auth != null && !this.auth.isExpired())
            return true;
        
        // Make a GET request to the home page with the student's cookie
        String homepage = Networking.getBody("https://elearn.volstate.edu/d2l/home", "Cookie", this.cookie);

        if (homepage == null)
            return false;

        // Do a regex search in the body of the homepage to find both the csrf token needed for API authorization, and the student's numeric D2L user ID
        var regex = "'XSRF\\.Token'.*?'(.*?)'.*?'Session\\.UserId'.*?'(.*?)'";
        Matcher matcher = Pattern.compile(regex).matcher(homepage);  

        // If the regex find failed, then return false.
        if (!matcher.find() || matcher.groupCount() != 2) {
            return false;
        }

        // Extract the csrf token and user id from the regex search
        String csrfToken = matcher.group(1);
        String userId = matcher.group(2);

        // Do a POST request to the API endpoint responsible for creating authorization access tokens
        HttpResponse<String> oauth = Networking.Post("https://elearn.volstate.edu/d2l/lp/auth/oauth2/token", "scope=*:*:*", 
            "Cookie", this.cookie, 
            "x-csrf-token", csrfToken,
            "Content-Type", "application/x-www-form-urlencoded"
        );

        if (oauth == null)
            return false;

        var json = new JSONObject(oauth.body());

        // Get the access token and expiration of the token
        var expires_at = json.getLong("expires_at");
        String token = json.getString("access_token");

        // Store all the authorization data in this.auth
        this.auth = new Authorization(expires_at, token, csrfToken, userId);
        
        return true;
    }
    
    /*
        @return Returns true if it successfully stored the student's courses in this.courses, false otherwise.
    */
    private boolean getCourses() {
        // Make sure we are authorized
        if (!Authorize()) {
            System.out.println("Error: Failed to get authorization token");
            return false;
        }
        
        // Clear any previously stored courses because we are about to get the latest
        this.courses.clear();
        
        var coursesUrl = String.format(
            "https://cfd2be83-bc1c-4a43-8ac3-469bc19bfc4a.enrollments.api.brightspace.com/users/%s?search=&pageSize=20&embedDepth=0&sort=current&parentOrganizations=&orgUnitTypeId=3&promotePins=true&roles=&excludeEnded=true&excludeIndirect=false", 
            auth.userId
        );

        // Make a GET request to the API endpoint responsible for listing the student's enrolled courses
        var curCourses = Networking.getJSON(coursesUrl, "Authorization", "Bearer " + auth.token);

        if (curCourses == null)
            return false;
        
        var curCoursesList = curCourses.getJSONArray("entities");

        if (curCoursesList.isEmpty()) {
            System.out.println("You are not registered for any courses!");
            return false;
        }
        
        // Iterate through the student's enrolled courses
        for (int i = 0; i < curCoursesList.length(); i++) {
            String courseInfoUrl = curCoursesList.getJSONObject(i).getString("href");

            // Make a GET request for each course at another API endpoint responsible for giving more useful links for the course specified
            JSONObject courseInfo = Networking.getJSON(courseInfoUrl, "Authorization", "Bearer " + auth.token);
            
            if (courseInfo == null)
                continue;
            
            var courseInfoLinks = courseInfo.getJSONArray("links");

            String courseId = null;

            // Iterate through the useful links until the numeric D2L course ID is found
            for (int j = 0; j < courseInfoLinks.length(); j++) {
                var linkObj = courseInfoLinks.getJSONObject(j);
                String linkType = linkObj.getJSONArray("rel").getString(0);

                // If the current link contains "/rels/organization" then the current link also contains the D2L numeric course ID
                // We will extract the course ID and then break the useful links loop
                if (linkType.contains("/rels/organization")){
                    try {
                        var courseHref = new URI(linkObj.getString("href"));
                        courseId = courseHref.getPath().substring(1); // Extract courseId from the link
                    } catch (URISyntaxException ex) {
                        System.out.println("Error! Failed to get the ID of a course.");
                        return false;
                    }
                    
                    break;
                }
            }

            // If we found the course ID for the current course, then lets get the name of the course and add it to our courses
            if (courseId != null) {
                // Make a GET request to an API endpoint responsible for giving information about the course
                JSONObject courseData = Networking.getJSON(String.format("https://cfd2be83-bc1c-4a43-8ac3-469bc19bfc4a.organizations.api.brightspace.com/%s?localeId=100021", courseId) , "Authorization", "Bearer " + auth.token);
                
                // If request was successful and courseData has the field "properties" in the object, then extract the course name from the object and add the course to this.courses
                if (courseData != null && courseData.has("properties")) {
                    String courseName = courseData.getJSONObject("properties").getString("name");
                    this.courses.add(new Course(courseId, courseName)); // Store the current course in this.courses
                }
            }
        }
        
        return true;
    }
    
    /*
        @username The student's username for their elearn account. This is the first part of their volstate email before the "@"
        @password The student's password to their elearn account. It is the same as their volstate email password
        @return Returns true if they successfully logged in, false otherwise.
    */
    public boolean Login(String username, String password) {
        String form = String.format("username=%s&password=%s", username, password);
          
        // Make a POST request to the login API form with our username and password
        var response = Networking.Post("https://elearn.volstate.edu/d2l/lp/auth/login/login.d2l", form, "Content-Type", "application/x-www-form-urlencoded");

        if (response == null)
            return false;

        // Get the response headers and cookies
        var headers = response.headers().map();
        var redirect = headers.get("Location");
        var cookies = headers.get("Set-Cookie");

        // If any of these are true, then the login attempt failed
        if (redirect.isEmpty() || cookies.isEmpty() || redirect.get(0).contains("BAD_CREDENTIALS")) {
            System.out.println("Incorrect login!");
            return false;
        }

        // Combine all the Set-Cookie values into one cookie
        cookie = "";
        cookies.forEach(val -> {
           cookie += val.substring(0, val.indexOf(';') + 1);
        });

        // Store the cookie on disk
        saveSession();

        return true;
    }
}
