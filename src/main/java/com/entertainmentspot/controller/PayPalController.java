package com.entertainmentspot.controller;

import com.entertainmentspot.service.PayPalService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/paypal")
public class PayPalController {

    private final PayPalService payPalService;

    public PayPalController(PayPalService payPalService) {
        this.payPalService = payPalService;
    }

    @PostMapping("/create_order")
    public Map<String, Object> createOrder(@RequestBody Map<String, String> body) throws Exception {
        return payPalService.createOrder(body.getOrDefault("planType", ""));
    }

    @PostMapping("/user_confirm")
    public Map<String, Object> userConfirm(@RequestBody Map<String, String> body) {
        return payPalService.userConfirm(
                body.getOrDefault("referenceId", ""),
                body.getOrDefault("action", "")
        );
    }

    @PostMapping("/order_confirm")
    public Map<String, Object> orderConfirm(@RequestBody Map<String, String> body) throws Exception {
        return payPalService.orderConfirm(body.getOrDefault("referenceId", ""));
    }

    @GetMapping("/order_query")
    public Map<String, Object> orderQuery(@RequestParam String referenceId) throws Exception {
        return payPalService.orderQuery(referenceId);
    }

    @PostMapping("/order_webhook")
    public Map<String, Object> webhook(@RequestBody String body, HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return payPalService.webhook(body, headers);
    }
}
