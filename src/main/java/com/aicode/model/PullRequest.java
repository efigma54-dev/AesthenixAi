package com.aicode.model;

public class PullRequest {

    private String id;
    private String title;
    private String state;

    public PullRequest() {}

    public PullRequest(String id, String title, String state) {
        this.id = id;
        this.title = title;
        this.state = state;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getState() { return state; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setState(String state) { this.state = state; }
}