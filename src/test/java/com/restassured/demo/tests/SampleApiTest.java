package com.restassured.demo.tests;

import com.restassured.demo.config.TestConfig;
import com.restassured.demo.models.Post;
import io.restassured.http.ContentType;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class SampleApiTest extends TestConfig {

    @Test(description = "Get all posts and verify response")
    public void testGetPosts() {
        given()
            .when()
                .get("/posts")
            .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test(description = "Get a specific post and verify its content")
    public void testGetSpecificPost() {
        Post post = given()
            .when()
                .get("/posts/1")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1))
                .extract()
                .as(Post.class);

        assert post.getId() == 1;
        assert post.getTitle() != null && !post.getTitle().isEmpty();
    }

    @Test(description = "Create a new post")
    public void testCreatePost() {
        Post newPost = new Post();
        newPost.setUserId(1);
        newPost.setTitle("Test Title");
        newPost.setBody("Test Body");

        given()
            .body(newPost)
            .when()
                .post("/posts")
            .then()
                .statusCode(201)
                .body("title", equalTo("Test Title"))
                .body("body", equalTo("Test Body"))
                .body("userId", equalTo(1));
    }
} 