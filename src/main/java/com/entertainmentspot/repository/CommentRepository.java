package com.entertainmentspot.repository;

import com.entertainmentspot.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByOrderByCreatedAtDesc();
    List<Comment> findByPageOrderByCreatedAtDesc(String page);
}
