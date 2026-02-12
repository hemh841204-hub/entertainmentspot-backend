package com.entertainmentspot.controller;

import com.entertainmentspot.entity.Comment;
import com.entertainmentspot.repository.CommentRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentRepository repo;

    public CommentController(CommentRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Comment> getAll(@RequestParam(required = false) String page) {
        if (page != null && !page.isBlank()) {
            return repo.findByPageOrderByCreatedAtDesc(page);
        }
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    public Comment create(@RequestBody Comment comment, HttpServletRequest request) {
        if (comment.getUserIp() == null || comment.getUserIp().isBlank()) {
            String ip = request.getHeader("X-Real-IP");
            if (ip == null) ip = request.getRemoteAddr();
            comment.setUserIp(ip);
        }
        if (comment.getSubmitDate() == null) comment.setSubmitDate(LocalDate.now());
        if (comment.getSubmitTime() == null) comment.setSubmitTime(LocalTime.now());
        return repo.save(comment);
    }
}
