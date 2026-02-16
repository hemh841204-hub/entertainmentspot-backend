package com.entertainmentspot.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "payment_record")
public class PaymentRecord {

    @Id
    @Column(name = "reference_id", length = 35)
    private String referenceId;

    @Column(length = 10)
    private String wdate;

    @Column(length = 8)
    private String wtime;

    @Column(name = "user_id", length = 35)
    private String userId;

    @Column(name = "transaction_type")
    private Integer transactionType;

    @Column(name = "transaction_subtype")
    private Integer transactionSubtype;

    @Column(name = "payment_type")
    private Short paymentType;

    @Column(name = "payment_status")
    private Short paymentStatus;

    @Column(name = "order_id", length = 35)
    private String orderId;

    @Column(length = 10)
    private String lstdate;

    @Column(length = 8)
    private String lsttime;

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public String getWdate() { return wdate; }
    public void setWdate(String wdate) { this.wdate = wdate; }
    public String getWtime() { return wtime; }
    public void setWtime(String wtime) { this.wtime = wtime; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Integer getTransactionType() { return transactionType; }
    public void setTransactionType(Integer transactionType) { this.transactionType = transactionType; }
    public Integer getTransactionSubtype() { return transactionSubtype; }
    public void setTransactionSubtype(Integer transactionSubtype) { this.transactionSubtype = transactionSubtype; }
    public Short getPaymentType() { return paymentType; }
    public void setPaymentType(Short paymentType) { this.paymentType = paymentType; }
    public Short getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(Short paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getLstdate() { return lstdate; }
    public void setLstdate(String lstdate) { this.lstdate = lstdate; }
    public String getLsttime() { return lsttime; }
    public void setLsttime(String lsttime) { this.lsttime = lsttime; }
}
