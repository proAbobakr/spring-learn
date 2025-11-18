package com.medical.post.repository;

import com.medical.post.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByPostIdAndDoctorId(Long postId, Long doctorId);

    boolean existsByPostIdAndDoctorId(Long postId, Long doctorId);

    long countByPostId(Long postId);

    void deleteByPostIdAndDoctorId(Long postId, Long doctorId);
}
