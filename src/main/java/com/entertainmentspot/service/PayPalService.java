package com.entertainmentspot.service;

import com.entertainmentspot.entity.InvokeRecord;
import com.entertainmentspot.entity.PaymentRecord;
import com.entertainmentspot.repository.InvokeRecordRepository;
import com.entertainmentspot.repository.PaymentRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class PayPalService {

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Value("${paypal.base-url}")
    private String baseUrl;

    @Value("${paypal.return-url}")
    private String returnUrl;

    @Value("${paypal.cancel-url}")
    private String cancelUrl;

    private final PaymentRecordRepository paymentRepo;
    private final InvokeRecordRepository invokeRepo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String cachedToken;
    private long tokenExpiresAt;

    private static final ZoneId SGT = ZoneId.of("Asia/Singapore");
    private static final Map<String, String> PLAN_AMOUNTS = Map.of(
            "monthly", "15.00",
            "quarterly", "36.00",
            "yearly", "120.00"
    );
    private static final Map<String, Integer> PLAN_SUBTYPES = Map.of(
            "monthly", 100001,
            "quarterly", 100002,
            "yearly", 100003
    );

    public PayPalService(PaymentRecordRepository paymentRepo, InvokeRecordRepository invokeRepo) {
        this.paymentRepo = paymentRepo;
        this.invokeRepo = invokeRepo;
    }

    private String nowDate() {
        return LocalDate.now(SGT).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String nowTime() {
        return LocalTime.now(SGT).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private synchronized String getAccessToken() throws Exception {
        // Return cached token if still valid (with 60s buffer)
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAt - 60000) {
            return cachedToken;
        }
        String auth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/oauth2/token"))
                .header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();
        long t = System.currentTimeMillis();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        long dur = System.currentTimeMillis() - t;
        JsonNode json = mapper.readTree(resp.body());
        cachedToken = json.get("access_token").asText();
        int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 3600;
        tokenExpiresAt = System.currentTimeMillis() + expiresIn * 1000L;
        // Log token request
        saveInvoke("token", 100000, baseUrl + "/v1/oauth2/token",
                mapper.writeValueAsString(Map.of("grant_type", "client_credentials")), resp.body(), resp.statusCode(),
                buildHeaderJson(req, resp), dur);
        return cachedToken;
    }

    /**
     * POST /api/paypal/create_order
     */
    public Map<String, Object> createOrder(String planType) throws Exception {
        String amount = PLAN_AMOUNTS.get(planType);
        if (amount == null) {
            return Map.of("error", "Invalid plan type");
        }

        String referenceId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String token = getAccessToken();

        // Build PayPal order request
        ObjectNode orderReq = mapper.createObjectNode();
        orderReq.put("intent", "CAPTURE");
        var pu = orderReq.putArray("purchase_units").addObject();
        pu.put("reference_id", referenceId);
        pu.put("description", planType.substring(0, 1).toUpperCase() + planType.substring(1) + " VIP Membership");
        var amountNode = pu.putObject("amount");
        amountNode.put("currency_code", "USD");
        amountNode.put("value", amount);
        var appCtx = orderReq.putObject("payment_source").putObject("paypal").putObject("experience_context");
        appCtx.put("return_url", returnUrl + "?referenceId=" + referenceId);
        appCtx.put("cancel_url", cancelUrl + "?referenceId=" + referenceId);
        appCtx.put("brand_name", "Entertainment Spot");
        appCtx.put("user_action", "PAY_NOW");

        String reqBody = mapper.writeValueAsString(orderReq);

        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v2/checkout/orders"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(reqBody))
                .build();

        long t0 = System.currentTimeMillis();
        HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
        long duration = System.currentTimeMillis() - t0;
        JsonNode respJson = mapper.readTree(resp.body());

        // Save invoke record
        saveInvoke(referenceId, 100001, baseUrl + "/v2/checkout/orders",
                reqBody, resp.body(), resp.statusCode(), buildHeaderJson(httpReq, resp), duration);

        if (resp.statusCode() != 201 && resp.statusCode() != 200) {
            return Map.of("error", "PayPal API error: " + resp.statusCode());
        }

        String orderId = respJson.get("id").asText();
        String approveLink = "";
        for (JsonNode link : respJson.get("links")) {
            if ("payer-action".equals(link.get("rel").asText())) {
                approveLink = link.get("href").asText();
                break;
            }
        }

        // Save payment record
        PaymentRecord pr = new PaymentRecord();
        pr.setReferenceId(referenceId);
        pr.setWdate(nowDate());
        pr.setWtime(nowTime());
        pr.setUserId("guest");
        pr.setTransactionType(1);
        pr.setTransactionSubtype(PLAN_SUBTYPES.get(planType));
        pr.setPaymentType((short) 1);
        pr.setPaymentStatus((short) 1); // Created
        pr.setOrderId(orderId);
        pr.setLstdate(nowDate());
        pr.setLsttime(nowTime());
        paymentRepo.save(pr);

        return Map.of(
                "referenceId", referenceId,
                "orderId", orderId,
                "approveLink", approveLink
        );
    }

    /**
     * POST /api/paypal/user_confirm
     */
    public Map<String, Object> userConfirm(String referenceId, String action) {
        var opt = paymentRepo.findById(referenceId);
        if (opt.isEmpty()) {
            return Map.of("error", "Order not found");
        }
        PaymentRecord pr = opt.get();
        if ("cancel".equals(action)) {
            pr.setPaymentStatus((short) 5);
            pr.setLstdate(nowDate());
            pr.setLsttime(nowTime());
            paymentRepo.save(pr);
            return Map.of("status", "cancelled");
        }
        // confirm → set status to Paying
        pr.setPaymentStatus((short) 2);
        pr.setLstdate(nowDate());
        pr.setLsttime(nowTime());
        paymentRepo.save(pr);
        return Map.of("status", "confirmed");
    }

    /**
     * POST /api/paypal/order_confirm — capture the order after PayPal redirect
     */
    public Map<String, Object> orderConfirm(String referenceId) throws Exception {
        var opt = paymentRepo.findById(referenceId);
        if (opt.isEmpty()) {
            return Map.of("error", "Order not found");
        }
        PaymentRecord pr = opt.get();

        // If already final status, just return
        if (pr.getPaymentStatus() == 3 || pr.getPaymentStatus() == 4 || pr.getPaymentStatus() == 5) {
            return recordToMap(pr);
        }

        // Try to capture
        String token = getAccessToken();
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v2/checkout/orders/" + pr.getOrderId() + "/capture"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();

        long t1 = System.currentTimeMillis();
        HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
        long dur1 = System.currentTimeMillis() - t1;
        saveInvoke(referenceId, 100002, baseUrl + "/v2/checkout/orders/" + pr.getOrderId() + "/capture",
                "{}", resp.body(), resp.statusCode(), buildHeaderJson(httpReq, resp), dur1);

        JsonNode respJson = mapper.readTree(resp.body());
        String status = respJson.has("status") ? respJson.get("status").asText() : "";

        if ("COMPLETED".equals(status)) {
            pr.setPaymentStatus((short) 3);
        } else if ("APPROVED".equals(status)) {
            // Still approved but not captured yet — keep polling
            pr.setPaymentStatus((short) 2);
        } else if (resp.statusCode() == 422) {
            // UNPROCESSABLE_ENTITY — could be already captured or not approved
            // Check if order was already captured
            JsonNode details = respJson.get("details");
            if (details != null && details.isArray()) {
                for (JsonNode d : details) {
                    if ("ORDER_ALREADY_CAPTURED".equals(d.path("issue").asText())) {
                        pr.setPaymentStatus((short) 3);
                        break;
                    }
                }
            }
            if (pr.getPaymentStatus() != 3) {
                pr.setPaymentStatus((short) 4);
            }
        } else if (resp.statusCode() >= 400) {
            pr.setPaymentStatus((short) 4);
        }

        pr.setLstdate(nowDate());
        pr.setLsttime(nowTime());
        paymentRepo.save(pr);

        return recordToMap(pr);
    }

    /**
     * GET /api/paypal/order_query?referenceId=xxx
     * Queries PayPal for latest order status and saves invoke record
     */
    public Map<String, Object> orderQuery(String referenceId) throws Exception {
        var opt = paymentRepo.findById(referenceId);
        if (opt.isEmpty()) {
            return Map.of("error", "Order not found");
        }
        PaymentRecord pr = opt.get();

        // Call PayPal Get Order API for latest status
        if (pr.getOrderId() != null && !pr.getOrderId().isEmpty()) {
            try {
                String token = getAccessToken();
                HttpRequest httpReq = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/v2/checkout/orders/" + pr.getOrderId()))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();
                long t2 = System.currentTimeMillis();
                HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
                long dur2 = System.currentTimeMillis() - t2;
                saveInvoke(referenceId, 100002, baseUrl + "/v2/checkout/orders/" + pr.getOrderId(),
                        "{}", resp.body(), resp.statusCode(), buildHeaderJson(httpReq, resp), dur2);

                // Update local status based on PayPal response
                if (resp.statusCode() == 200) {
                    JsonNode respJson = mapper.readTree(resp.body());
                    String paypalStatus = respJson.path("status").asText("");
                    if ("COMPLETED".equals(paypalStatus) && pr.getPaymentStatus() != 3) {
                        pr.setPaymentStatus((short) 3);
                        pr.setLstdate(nowDate());
                        pr.setLsttime(nowTime());
                        paymentRepo.save(pr);
                    } else if ("APPROVED".equals(paypalStatus) && pr.getPaymentStatus() < 2) {
                        pr.setPaymentStatus((short) 2);
                        pr.setLstdate(nowDate());
                        pr.setLsttime(nowTime());
                        paymentRepo.save(pr);
                    }
                }
            } catch (Exception e) {
                // Don't fail the query if PayPal call fails
            }
        }

        return recordToMap(pr);
    }

    /**
     * POST /api/paypal/order_webhook — PayPal webhook callback
     */
    public Map<String, Object> webhook(String body, Map<String, String> headers) {
        long t = System.currentTimeMillis();
        try {
            JsonNode json = mapper.readTree(body);
            String eventType = json.path("event_type").asText("");
            JsonNode resource = json.path("resource");

            // For CAPTURE event, orderId is in supplementary_data.related_ids.order_id
            // For ORDER event, orderId is resource.id
            String orderId;
            if ("PAYMENT.CAPTURE.COMPLETED".equals(eventType)) {
                orderId = resource.path("supplementary_data").path("related_ids").path("order_id").asText(
                        resource.path("id").asText(""));
            } else {
                orderId = resource.path("id").asText("");
            }

            // Find the payment record by orderId to get real referenceId
            String referenceId = "unknown";
            PaymentRecord matched = null;
            for (PaymentRecord pr : paymentRepo.findAll()) {
                if (orderId.equals(pr.getOrderId())) {
                    referenceId = pr.getReferenceId();
                    matched = pr;
                    break;
                }
            }

            long dur = System.currentTimeMillis() - t;

            // Build header JSON
            String headerJson = mapper.writeValueAsString(headers);

            String responseBody = mapper.writeValueAsString(Map.of("status", "ok"));
            saveInvokeWithDirection(referenceId, 100003, (short) 2, "/api/paypal/order_webhook", body, responseBody, 200, headerJson, dur);

            // If already final status, skip update
            if (matched != null) {
                short currentStatus = matched.getPaymentStatus();
                if (currentStatus == 3 || currentStatus == 4 || currentStatus == 5) {
                    // Already terminal, no further processing
                } else if ("PAYMENT.CAPTURE.COMPLETED".equals(eventType)) {
                    matched.setPaymentStatus((short) 3);
                    matched.setLstdate(nowDate());
                    matched.setLsttime(nowTime());
                    paymentRepo.save(matched);
                } else if ("CHECKOUT.ORDER.APPROVED".equals(eventType)) {
                    matched.setPaymentStatus((short) 2);
                    matched.setLstdate(nowDate());
                    matched.setLsttime(nowTime());
                    paymentRepo.save(matched);
                }
            }
        } catch (Exception e) {
            // Log but don't fail
        }
        return Map.of("status", "ok");
    }

    private void saveInvoke(String refId, int subtype, String invokeUrl, String reqBody, String respBody, int httpStatus, String httpHeader, long durationMs) {
        saveInvokeWithDirection(refId, subtype, (short) 1, invokeUrl, reqBody, respBody, httpStatus, httpHeader, durationMs);
    }

    private void saveInvokeWithDirection(String refId, int subtype, short direction, String invokeUrl, String reqBody, String respBody, int httpStatus, String httpHeader, long durationMs) {
        InvokeRecord ir = new InvokeRecord();
        ir.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        ir.setReferenceId(refId);
        ir.setWdate(nowDate());
        ir.setWtime(nowTime());
        ir.setRequestDirection(direction); // 1=outbound, 2=inbound
        ir.setInvokeStatus((short) (httpStatus < 400 ? 1 : 0));
        ir.setRequstType(1); // 1=PayPal
        ir.setRequstSubtype(subtype);
        ir.setExecutionDuration((int) durationMs);
        ir.setInvokeUrl(invokeUrl);
        ir.setHttpHeader(httpHeader);
        ir.setRequestBody(reqBody);
        ir.setResponseBody(respBody);
        ir.setHttpStatus(httpStatus);
        invokeRepo.save(ir);
    }

    private String buildHeaderJson(HttpRequest req, HttpResponse<String> resp) {
        try {
            ObjectNode node = mapper.createObjectNode();
            ObjectNode reqHeaders = mapper.createObjectNode();
            req.headers().map().forEach((k, v) -> {
                String val = k.equalsIgnoreCase("Authorization") ? "***" : String.join(", ", v);
                reqHeaders.put(k, val);
            });
            ObjectNode respHeaders = mapper.createObjectNode();
            resp.headers().map().forEach((k, v) -> respHeaders.put(k, String.join(", ", v)));
            node.set("request", reqHeaders);
            node.set("response", respHeaders);
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> recordToMap(PaymentRecord pr) {
        return Map.of(
                "referenceId", pr.getReferenceId(),
                "orderId", pr.getOrderId() != null ? pr.getOrderId() : "",
                "paymentStatus", pr.getPaymentStatus(),
                "transactionSubtype", pr.getTransactionSubtype() != null ? pr.getTransactionSubtype() : 0,
                "paymentType", pr.getPaymentType() != null ? pr.getPaymentType() : 0,
                "userId", pr.getUserId() != null ? pr.getUserId() : "",
                "wdate", pr.getWdate() != null ? pr.getWdate() : "",
                "wtime", pr.getWtime() != null ? pr.getWtime() : "",
                "lstdate", pr.getLstdate() != null ? pr.getLstdate() : ""
        );
    }
}
