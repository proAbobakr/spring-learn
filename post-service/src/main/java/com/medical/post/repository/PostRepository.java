package com.medical.post.repository;

import com.medical.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByDoctorId(Long doctorId, Pageable pageable);

    Page<Post> findByPostType(Post.PostType postType, Pageable pageable);

    Page<Post> findBySpecialty(String specialty, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.published = true ORDER BY p.createdAt DESC")
    Page<Post> findAllPublished(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.specialty) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Post> searchPosts(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.published = true ORDER BY p.likeCount DESC, p.commentCount DESC")
    Page<Post> findTrendingPosts(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.doctorId IN :doctorIds AND p.published = true ORDER BY p.createdAt DESC")
    Page<Post> findFeedPosts(@Param("doctorIds") List<Long> doctorIds, Pageable pageable);

    @Query("SELECT DISTINCT p.specialty FROM Post p WHERE p.specialty IS NOT NULL")
    List<String> findAllSpecialties();
}
