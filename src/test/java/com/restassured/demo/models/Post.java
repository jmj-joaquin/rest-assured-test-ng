package com.restassured.demo.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Post {
    private int id;
    private int userId;
    private String title;
    private String body;

    @JsonProperty("id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @JsonProperty("userId")
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("body")
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
} 