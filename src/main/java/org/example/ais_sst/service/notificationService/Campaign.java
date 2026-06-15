package org.example.ais_sst.service.notificationService;

// Campaign.java
public class Campaign {
    private String id;
    private String name;
    private String subject;
    private String status;
    private int sentCount;
    private int openCount;
    private int clickCount;
    private String createdAt;

    // Getters и Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getSentCount() { return sentCount; }
    public void setSentCount(int sentCount) { this.sentCount = sentCount; }

    public int getOpenCount() { return openCount; }
    public void setOpenCount(int openCount) { this.openCount = openCount; }

    public int getClickCount() { return clickCount; }
    public void setClickCount(int clickCount) { this.clickCount = clickCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}