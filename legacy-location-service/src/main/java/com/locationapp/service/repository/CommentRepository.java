package com.locationapp.service.repository;

import com.locationapp.service.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Comment Repository
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Find all top-level comments for a location (no parent)
     */
    List<Comment> findByLocationIdAndParentCommentIsNullOrderByCreatedAtDesc(Long locationId);

    /**
     * Find all comments for a location (including replies)
     */
    List<Comment> findByLocationIdOrderByCreatedAtDesc(Long locationId);

    /**
     * Find replies to a comment
     */
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    /**
     * Find all comments by a user
     */
    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Get comment count for a location
     */
    long countByLocationId(Long locationId);

    /**
     * Get reply count for a comment
     */
    long countByParentCommentId(Long parentCommentId);

    /**
     * Find top-level comments with pagination
     */
    Page<Comment> findByLocationIdAndParentCommentIsNull(Long locationId, Pageable pageable);

    /**
     * Find most liked comments
     */
    @Query("SELECT c FROM Comment c WHERE c.location.id = :locationId AND c.parentComment IS NULL ORDER BY c.likeCount DESC, c.createdAt DESC")
    List<Comment> findTopCommentsByLikes(Long locationId, Pageable pageable);
}
