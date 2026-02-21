package com.nagar.parishad.backend.repository;

import com.nagar.parishad.backend.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Page<Department> findByAdminId(Long adminId, Pageable pageable);

    List<Department> findByAdminId(Long adminId);

    long countByAdminId(Long adminId);
}
