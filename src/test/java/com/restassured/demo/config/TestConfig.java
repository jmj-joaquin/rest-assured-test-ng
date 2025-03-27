package com.restassured.demo.config;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.testng.annotations.BeforeSuite;

public class TestConfig {
    protected static RequestSpecification requestSpec;
    protected static ResponseSpecification responseSpec;
    
    protected static final String BASE_URI = "https://test-dummy.website.com";
    protected static final String AUTH_ENDPOINT = "/api/auth/login";

    @BeforeSuite
    public void setup() {
        // Relax SSL verification for testing
        RestAssured.useRelaxedHTTPSValidation();
        
        // Configure base request specification
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(BASE_URI)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();

        // Configure base response specification
        responseSpec = new ResponseSpecBuilder()
                .build();

        // Set default specifications
        RestAssured.requestSpecification = requestSpec;
        RestAssured.responseSpecification = responseSpec;
    }
} 