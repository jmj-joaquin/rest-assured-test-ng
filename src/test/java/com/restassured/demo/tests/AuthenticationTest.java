package com.restassured.demo.tests;

import com.restassured.demo.config.TestConfig;
import com.restassured.demo.models.LoginRequest;
import io.restassured.http.ContentType;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import com.restassured.demo.utils.ExcelDataReader;
import com.aventstack.extentreports.Status;
import com.restassured.demo.utils.ExtentReportManager;
import org.testng.annotations.Listeners;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Listeners(com.restassured.demo.listeners.TestListener.class)
public class AuthenticationTest extends TestConfig {

    @DataProvider(name = "loginTestData")
    public Object[][] loginTestData() {
        return ExcelDataReader.readTestData("login-test-data.xlsx", "LoginTests");
    }

    @Test(dataProvider = "loginTestData")
    public void testDataDrivenLogin(String email, String password, String timezone, 
                         int expectedStatus, String expectedMessage, String expectedDetails) {
        ExtentReportManager.getTest().log(Status.INFO, "Testing login with email: " + email);
        
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        if (password != null) {
            loginRequest.setPassword(password);
            ExtentReportManager.getTest().log(Status.INFO, "Setting password: " + password);
        }
        if (timezone != null) {
            loginRequest.setTimezone(timezone);
            ExtentReportManager.getTest().log(Status.INFO, "Setting timezone: " + timezone);
        }

        ExtentReportManager.getTest().log(Status.INFO, "Sending request to " + AUTH_ENDPOINT);
        
        var response = given()
            .body(loginRequest)
        .when()
            .post(AUTH_ENDPOINT)
        .then()
            .statusCode(expectedStatus)
            .contentType(ContentType.JSON)
            .body("message", containsStringIgnoringCase(expectedMessage))
            .body("message", notNullValue());

        ExtentReportManager.getTest().log(Status.INFO, 
            "Response received with status: " + expectedStatus);

        if (expectedStatus == 200) {
            response.body("results", notNullValue())
                   .body("results.token", notNullValue());
        }
        
        if (expectedDetails != null) {
            if (email.equals("invalid.email")) {
                response.body("details.email[0]", containsStringIgnoringCase(expectedDetails));
            } else {
                response.body("details.password[0]", containsStringIgnoringCase(expectedDetails));
            }
        }
    }

    @Test
    public void testTwoFactorAuthentication() {
        ExtentReportManager.getTest().log(Status.INFO, "Starting Two Factor Authentication Test");
        
        String email = "dummy.test+2fa@gmail.com";  // Replace with test email
        String password = "dummyTestPassword123!";     // Replace with test password
        String timezone = "Asia/Manila";

        ExtentReportManager.getTest().log(Status.INFO, "Test credentials - Email: " + email + ", Timezone: " + timezone);

        // Step 1: Initial login attempt
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);
        loginRequest.setTimezone(timezone);

        ExtentReportManager.getTest().log(Status.INFO, "Step 1: Initiating login attempt to trigger 2FA");
        
        given()
            .body(loginRequest)
        .when()
            .post(AUTH_ENDPOINT)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("message", containsStringIgnoringCase("code has been sent to the registered email"))
            .extract()
            .response();

        ExtentReportManager.getTest().log(Status.PASS, "Initial login successful, 2FA code sent to email");

        // Step 2: Get 2FA code
        ExtentReportManager.getTest().log(Status.INFO, "Step 2: Retrieving 2FA code");
        
        Response twoFactorResponse = given()
            .body("{\"email\": \"" + email + "\"}")
            .contentType(ContentType.JSON)
        .when()
            .post("/api/auth/retrieve-two-factor-code")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .response();

        String pin = twoFactorResponse.jsonPath().getString("results");
        ExtentReportManager.getTest().log(Status.PASS, "Successfully retrieved 2FA code");

        // Step 3: Complete 2FA authentication
        ExtentReportManager.getTest().log(Status.INFO, "Step 3: Completing 2FA verification");
        
        given()
            .body("{\"auth_code\": \"" + pin + "\", \"email\": \"" + email + "\", \"password\": \"" + password + "\"}")
            .contentType(ContentType.JSON)
        .when()
            .post("/api/auth/verify-two-factor-auth")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("results.token", notNullValue());

        ExtentReportManager.getTest().log(Status.PASS, "Two Factor Authentication completed successfully");
    }
} 