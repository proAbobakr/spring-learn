package com.medical.user.repository;

import com.medical.user.model.Following;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowingRepository extends JpaRepository<Following, Long> {

    Optional<Following> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Page<Following> findByFollowerId(Long followerId, Pageable pageable);

    Page<Following> findByFollowingId(Long followingId, Pageable pageable);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
}
