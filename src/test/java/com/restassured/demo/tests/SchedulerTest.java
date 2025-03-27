package com.restassured.demo.tests;

import com.restassured.demo.config.TestConfig;
import com.restassured.demo.models.LoginRequest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.awaitility.Awaitility;
import java.time.Duration;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import com.aventstack.extentreports.Status;
import com.restassured.demo.utils.ExtentReportManager;
import org.testng.annotations.Listeners;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Listeners(com.restassured.demo.listeners.TestListener.class)
public class SchedulerTest extends TestConfig {
    
    private String authToken;
    private static final int POLLING_INTERVAL_SECONDS = 60;
    private static final int MAX_WAIT_TIME_SECONDS = 300;
    private Map<String, Object> schedulePayload;
    
    @BeforeClass
    public void setupSchedulerTest() {
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
        
        initializeTestData();
    }
    
    private void initializeTestData() {
        schedulePayload = new HashMap<>();
        schedulePayload.put("round_id", 107);
        schedulePayload.put("client_id", 2386);
        schedulePayload.put("clinician_id", "131");
        schedulePayload.put("treatment_episode_id", 1466);
    }
    
    @Test(description = "Verify EMR round creation after scheduling")
    public void testScheduledRoundCreation() {
        ExtentReportManager.getTest().log(Status.INFO, "Starting EMR round scheduling test");
        
        // Step 1: Make POST schedule API call
        Response scheduleResponse = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType("application/json")
            .body(schedulePayload)
        .when()
            .post("/api/auth/rounds/emr-rounds")
        .then()
            .statusCode(201)
            .contentType("application/json")
            .extract()
            .response();
            
        ExtentReportManager.getTest().log(Status.INFO, "EMR round scheduled successfully");
        
        // Step 2: Poll for scheduled round creation
        ExtentReportManager.getTest().log(Status.INFO, "Polling for scheduled round creation");
        
        Awaitility.await()
            .pollInterval(Duration.ofSeconds(POLLING_INTERVAL_SECONDS))
            .atMost(Duration.ofSeconds(MAX_WAIT_TIME_SECONDS))
            .until(() -> isScheduledRoundCreated());
            
        // Step 3: Verify scheduled round details
        Response scheduledRoundResponse = getScheduledRound();
        verifyScheduledRoundDetails(scheduledRoundResponse);
        
        ExtentReportManager.getTest().log(Status.PASS, "Scheduled round created and verified successfully");
    }
    
    private boolean isScheduledRoundCreated() {
        try {
            Response response = getScheduledRound();
            return response.getStatusCode() == 200 && 
                   !response.jsonPath().getList("results.data").isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    private Response getScheduledRound() {
        return given()
            .header("Authorization", "Bearer " + authToken)
            .queryParam("clientId", schedulePayload.get("client_id"))
            .queryParam("treatmentEpisodeId", schedulePayload.get("treatment_episode_id"))
        .when()
            .get("/api/auth/rounds/emr-past-rounds")
        .then()
            .extract()
            .response();
    }
    
    private void verifyScheduledRoundDetails(Response response) {
        Assert.assertEquals(response.jsonPath().getInt("results.data[0].round_id"), 
            schedulePayload.get("round_id"));
        Assert.assertEquals(response.jsonPath().getInt("results.data[0].client_id"), 
            schedulePayload.get("client_id"));
        Assert.assertEquals(response.jsonPath().getString("results.data[0].clinician_id"), 
            schedulePayload.get("clinician_id"));
        Assert.assertEquals(response.jsonPath().getInt("results.data[0].treatment_episode_id"), 
            schedulePayload.get("treatment_episode_id"));
    }
} 