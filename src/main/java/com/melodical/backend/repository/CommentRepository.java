package com.melodical.backend.repository;

import com.melodical.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByMusicalId(Long musicalId);
    List<Comment> findByUserId(Long userId);

}
