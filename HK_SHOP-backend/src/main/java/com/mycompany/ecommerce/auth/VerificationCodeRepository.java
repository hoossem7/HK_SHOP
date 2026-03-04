package com.mycompany.ecommerce.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {


    void deleteByUser_Id(Long userId);

    Optional<VerificationCode> findTopByUser_EmailAndCodeOrderByIdDesc(String email, String code);}