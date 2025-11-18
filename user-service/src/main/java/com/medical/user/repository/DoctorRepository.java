package com.medical.user.repository;

import com.medical.user.model.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<Doctor> findBySpecialty(String specialty, Pageable pageable);

    Page<Doctor> findByCountryAndCity(String country, String city, Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE " +
           "LOWER(d.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.specialty) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Doctor> searchDoctors(@Param("query") String query, Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE d.verified = true ORDER BY d.totalFollowers DESC")
    Page<Doctor> findTopDoctors(Pageable pageable);
}
