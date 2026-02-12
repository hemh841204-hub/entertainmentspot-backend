package com.entertainmentspot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_logs")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_ip", nullable = false, length = 50)
    private String userIp;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "theme_type", nullable = false, length = 20)
    private String themeType;

    @Column(name = "sub_theme_type", nullable = false, length = 50)
    private String subThemeType;

    @Column(name = "used", columnDefinition = "ENUM('yes','no') DEFAULT 'no'")
    private String used = "no";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserIp() { return userIp; }
    public void setUserIp(String userIp) { this.userIp = userIp; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getThemeType() { return themeType; }
    public void setThemeType(String themeType) { this.themeType = themeType; }
    public String getSubThemeType() { return subThemeType; }
    public void setSubThemeType(String subThemeType) { this.subThemeType = subThemeType; }
    public String getUsed() { return used; }
    public void setUsed(String used) { this.used = used; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
