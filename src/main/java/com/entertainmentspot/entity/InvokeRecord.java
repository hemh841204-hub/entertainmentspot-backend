package com.entertainmentspot.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "invoke_record")
public class InvokeRecord {

    @Id
    @Column(name = "request_id", length = 32)
    private String requestId;

    @Column(name = "request_direstion")
    private Short requestDirection;

    @Column(length = 10)
    private String wdate;

    @Column(length = 8)
    private String wtime;

    @Column(name = "execution_duration")
    private Integer executionDuration;

    @Column(name = "invoke_status")
    private Short invokeStatus;

    @Column(name = "requst_type")
    private Integer requstType;

    @Column(name = "requst_subtype")
    private Integer requstSubtype;

    @Column(name = "reference_id", length = 35)
    private String referenceId;

    @Column(name = "invoke_url", length = 200)
    private String invokeUrl;

    @Column(name = "http_header", columnDefinition = "MEDIUMTEXT")
    private String httpHeader;

    @Column(name = "request_body", columnDefinition = "MEDIUMTEXT")
    private String requestBody;

    @Column(name = "response_body", columnDefinition = "MEDIUMTEXT")
    private String responseBody;

    @Column(name = "http_status")
    private Integer httpStatus;

    // Getters & Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Short getRequestDirection() { return requestDirection; }
    public void setRequestDirection(Short requestDirection) { this.requestDirection = requestDirection; }
    public String getWdate() { return wdate; }
    public void setWdate(String wdate) { this.wdate = wdate; }
    public String getWtime() { return wtime; }
    public void setWtime(String wtime) { this.wtime = wtime; }
    public Integer getExecutionDuration() { return executionDuration; }
    public void setExecutionDuration(Integer executionDuration) { this.executionDuration = executionDuration; }
    public Short getInvokeStatus() { return invokeStatus; }
    public void setInvokeStatus(Short invokeStatus) { this.invokeStatus = invokeStatus; }
    public Integer getRequstType() { return requstType; }
    public void setRequstType(Integer requstType) { this.requstType = requstType; }
    public Integer getRequstSubtype() { return requstSubtype; }
    public void setRequstSubtype(Integer requstSubtype) { this.requstSubtype = requstSubtype; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public String getInvokeUrl() { return invokeUrl; }
    public void setInvokeUrl(String invokeUrl) { this.invokeUrl = invokeUrl; }
    public String getHttpHeader() { return httpHeader; }
    public void setHttpHeader(String httpHeader) { this.httpHeader = httpHeader; }
    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
}
