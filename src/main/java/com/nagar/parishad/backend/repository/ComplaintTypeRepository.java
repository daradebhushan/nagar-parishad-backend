package com.nagar.parishad.backend.repository;

import com.nagar.parishad.backend.entity.ComplaintType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintTypeRepository extends JpaRepository<ComplaintType, Long> {
    List<ComplaintType> findByDepartmentId(Long departmentId);

    List<ComplaintType> findByDepartmentIdAndActiveTrue(Long departmentId);
}
