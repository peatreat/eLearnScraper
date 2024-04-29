package com.mycompany.semesterproject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Networking {
    private static final HttpClient client = HttpClient.newHttpClient(); // Our HttpClient object used for making network requests
    
    /*
        @url The url to make the network request to
        @headers The request headers to send in the network request. Each header key pair is seperated by commas, Example: Networking.Get("https://google.com/", "User-Agent", "My Useragent")
        @return The HttpResponse object for the network request.
    */
    public static HttpResponse<String> Get(String url, String... headers) {
        HttpResponse<String> response;
        
        try {        
            var request = HttpRequest.newBuilder()
            .uri(new URI(url))
            .GET();

            if (headers.length > 0)
                request.headers(headers);
            
            response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } 
        catch (URISyntaxException | IOException | InterruptedException ex){
            System.out.println("Error! Failed to connect to " + url);
            return null;
        }
        
        return response;
    }
    
    /*
        @url The url to make the network request to
        @headers The request headers to send in the network request. Each header key pair is seperated by commas, Example: Networking.Get("https://google.com/", "User-Agent", "My Useragent")
        @return The response body as a string
    */
    public static String getBody(String url, String... headers) {
        var response = Get(url, headers);
        
        if (response == null)
            return null;
        
        return response.body();
    }
    
    /*
        @url The url to make the network request to
        @headers The request headers to send in the network request. Each header key pair is seperated by commas, Example: Networking.Get("https://google.com/", "User-Agent", "My Useragent")
        @return The response body as JSONObject
    */
    public static JSONObject getJSON(String url, String... headers) {
        String body = getBody(url, headers);
        
        if (body == null)
            return null;

        try {
            return new JSONObject(body);
        } catch (JSONException ex) {
            return null;
        }
    }
    
    /*
        @url The url to make the network request to
        @headers The request headers to send in the network request. Each header key pair is seperated by commas, Example: Networking.Get("https://google.com/", "User-Agent", "My Useragent")
        @return The response body as JSONArray
    */
    public static JSONArray getJSONArray(String url, String... headers) {
        String body = getBody(url, headers);
        
        if (body == null)
            return null;

        try {
            return new JSONArray(body);
        } catch (JSONException ex) {
            return null;
        }
    }
    
    /*
        @url The url to make the network request to
        @date The form data to send as a string
        @headers The request headers to send in the network request. Each header key pair is seperated by commas, Example: Networking.Get("https://google.com/", "User-Agent", "My Useragent")
        @return The HttpResponse object for the network request.
    */
    public static HttpResponse<String> Post(String url, String data, String... headers) {
        HttpResponse<String> response;
        
        try {        
            var request = HttpRequest.newBuilder()
            .uri(new URI(url))
            .POST(HttpRequest.BodyPublishers.ofString(data)); 
            
            if (headers.length > 0)
                request.headers(headers);

            response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } 
        catch (URISyntaxException | IOException | InterruptedException ex){
            System.out.println("Error! Failed to connect to " + url);
            return null;
        }
        
        return response;
    }
}
