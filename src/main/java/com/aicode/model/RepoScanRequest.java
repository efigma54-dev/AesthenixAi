package com.aicode.model;

public class RepoScanRequest {
    private String repoUrl;
    private String token;

    public RepoScanRequest() {}
    public RepoScanRequest(String repoUrl, String token) { this.repoUrl = repoUrl; this.token = token; }

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
