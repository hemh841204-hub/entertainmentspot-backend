package com.entertainmentspot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_ip", nullable = false, length = 50)
    private String userIp;

    @Column(name = "submit_date", nullable = false)
    private LocalDate submitDate;

    @Column(name = "submit_time", nullable = false)
    private LocalTime submitTime;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(name = "page", nullable = false, length = 50)
    private String page;

    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Column(name = "stars", nullable = false)
    private Integer stars;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserIp() { return userIp; }
    public void setUserIp(String userIp) { this.userIp = userIp; }
    public LocalDate getSubmitDate() { return submitDate; }
    public void setSubmitDate(LocalDate submitDate) { this.submitDate = submitDate; }
    public LocalTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalTime submitTime) { this.submitTime = submitTime; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getPage() { return page; }
    public void setPage(String page) { this.page = page; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
