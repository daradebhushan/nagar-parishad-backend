package com.nagar.parishad.backend.repository;

import com.nagar.parishad.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    Optional<User> findByResetPasswordToken(String resetPasswordToken);

    boolean existsByEmail(String email);

    Page<User> findByAdminId(Long adminId, Pageable pageable);

    java.util.List<User> findByDepartmentIdAndRole(Long departmentId, com.nagar.parishad.backend.enums.Role role);

    java.util.List<User> findByRole(com.nagar.parishad.backend.enums.Role role);

    Page<User> findByDepartment_Id(Long departmentId, Pageable pageable);

    long countByDepartmentId(Long departmentId);

    long countByDepartment_Id(Long departmentId);

    long countByRole(com.nagar.parishad.backend.enums.Role role);

    long countByAdmin(User admin);

    long countByAdminId(Long adminId);

    long countByAdminIdAndDepartment_Id(Long adminId, Long departmentId);
}
