package com.restassured.demo.tests;

import com.restassured.demo.config.TestConfig;
import com.restassured.demo.models.LoginRequest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.aventstack.extentreports.Status;
import com.restassured.demo.utils.ExtentReportManager;

@Listeners(com.restassured.demo.listeners.TestListener.class)
public class CRUDTest extends TestConfig {
    
    private Map<String, Object> shiftNotePayload;
    private int createdNoteId;
    private String authToken;

    @BeforeClass
    public void setupTestData() {
        // Authenticate and get token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("dummy.test@gmail.com");
        loginRequest.setPassword("dummyTestPassword123!");
        loginRequest.setTimezone("Asia/Manila");
        
        // Get authentication token
        Response authResponse = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post(AUTH_ENDPOINT)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("results.token", notNullValue())
            .extract()
            .response();

        // Store the auth token
        authToken = authResponse.jsonPath().getString("results.token");
        Assert.assertNotNull(authToken, "Authentication token should not be null");

        // Initialize shift note test data
        Map<String, Object> clientData = new HashMap<>();
        clientData.put("client_id", 2386);

        shiftNotePayload = new HashMap<>();
        shiftNotePayload.put("clinician_id", "131");
        shiftNotePayload.put("location_id", 153);
        shiftNotePayload.put("shift", "Day Shift");
        shiftNotePayload.put("note", "notes\ntest");
        shiftNotePayload.put("start_date", "2025/03/26 16:30");
        shiftNotePayload.put("end_date", "2025/03/27 00:30");
        shiftNotePayload.put("clients", Collections.singletonList(clientData));
    }

    @Test(description = "Create a new shift note and verify its creation", priority = 1)
    public void testCreatePost() {
        ExtentReportManager.getTest().log(Status.INFO, "Starting shift note creation test");
        ExtentReportManager.getTest().log(Status.INFO, "Using authentication token and initialized test data");
        
        Response response = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(shiftNotePayload)
        .when()
            .post("/api/auth/shift-notes")
        .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("message", containsStringIgnoringCase("shift note successfully added"))
            .extract()
            .response();

        // Store the created note ID for subsequent tests
        createdNoteId = response.path("results.id");
        Assert.assertTrue(createdNoteId > 0, "Shift Note ID should be greater than 0");
        ExtentReportManager.getTest().log(Status.PASS, "Successfully created shift note with ID: " + createdNoteId);
    }

    @Test(description = "Read the created shift note and verify its contents", priority = 2, dependsOnMethods = "testCreatePost")
    public void testReadPost() {
        ExtentReportManager.getTest().log(Status.INFO, "Starting shift note read test for ID: " + createdNoteId);
        
        given()
            .header("Authorization", "Bearer " + authToken)
            .queryParam("shift_note_id", createdNoteId)
        .when()
            .get("/api/auth/shift-notes-clients")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("results.data[0].shift_notes.id", equalTo(createdNoteId))
            .body("results.data[0].shift_notes.clinician_id", is(131))
            .body("results.data[0].shift_notes.location_id", equalTo(153))
            .body("results.data[0].shift_notes.shift", equalTo("Day Shift"))
            .body("results.data[0].shift_notes.note", equalTo("notes\ntest"));
            
        ExtentReportManager.getTest().log(Status.PASS, "Successfully verified shift note contents");
    }

    @Test(description = "Update the created shift note and verify the changes", priority = 3, dependsOnMethods = "testReadPost")
    public void testUpdatePost() {
        ExtentReportManager.getTest().log(Status.INFO, "Starting shift note update test for ID: " + createdNoteId);
        
        // Create new update payload
        Map<String, Object> clientData = new HashMap<>();
        clientData.put("client_id", 2387);

        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("clinician_id", "131");
        updatePayload.put("location_id", 154);
        updatePayload.put("shift", "Night Shift");
        updatePayload.put("note", "test\nnotes");
        updatePayload.put("start_date", "2025/04/27 10:44");
        updatePayload.put("end_date", "2025/04/27 11:44");
        updatePayload.put("clients", Collections.singletonList(clientData));

        ExtentReportManager.getTest().log(Status.INFO, "Sending update request");
        
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .pathParam("noteId", createdNoteId)
            .body(updatePayload)
        .when()
            .put("/api/auth/shift-notes/{noteId}")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("message", containsStringIgnoringCase("shift note successfully updated"));

        ExtentReportManager.getTest().log(Status.INFO, "Verifying updated shift note");
        
        // Verify the update with a GET request
        given()
            .header("Authorization", "Bearer " + authToken)
            .queryParam("shift_note_id", createdNoteId)
        .when()
            .get("/api/auth/shift-notes-clients")
        .then()
            .statusCode(200)
            .body("results.data[0].shift_notes.clinician_id", is(131))
            .body("results.data[0].shift_notes.location_id", equalTo(154))
            .body("results.data[0].shift_notes.shift", equalTo("Night Shift"))
            .body("results.data[0].shift_notes.note", equalTo("test\nnotes"));
            
        ExtentReportManager.getTest().log(Status.PASS, "Successfully updated and verified shift note");
    }

    @Test(description = "Delete the created shift note and verify deletion", priority = 4, dependsOnMethods = "testUpdatePost")
    public void testDeletePost() {
        ExtentReportManager.getTest().log(Status.INFO, "Starting shift note deletion test for ID: " + createdNoteId);
        
        given()
            .header("Authorization", "Bearer " + authToken)
            .pathParam("noteId", createdNoteId)
        .when()
            .delete("/api/auth/shift-notes/{noteId}")
        .then()
            .statusCode(200)
            .body("message", containsStringIgnoringCase("shift note successfully archived"));

        ExtentReportManager.getTest().log(Status.INFO, "Verifying shift note deletion");
        
        // Verify the note is deleted
        given()
            .header("Authorization", "Bearer " + authToken)
            .pathParam("noteId", createdNoteId)
        .when()
            .get("/api/auth/shift-notes/{noteId}")
        .then()
            .statusCode(200)
            .body("results", nullValue());
            
        ExtentReportManager.getTest().log(Status.PASS, "Successfully archived and verified shift note deletion");
    }
} 