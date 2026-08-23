package com.nagar.parishad.backend.repository;

import com.nagar.parishad.backend.entity.Complaint;
import com.nagar.parishad.backend.enums.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    Optional<Complaint> findByComplaintNo(String complaintNo);

    Optional<Complaint> findByComplaintNoAndCitizenMobile(String complaintNo, String citizenMobile);

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findByDepartmentId(Long departmentId);

    List<Complaint> findByCitizenMobile(String citizenMobile);

    List<Complaint> findByCitizenMobileAndAdminIdOrderByCreatedAtDesc(String citizenMobile, Long adminId);

    List<Complaint> findByComplaintTypeId(Long complaintTypeId);

    List<Complaint> findByAdminId(Long adminId);

    List<Complaint> findByRelatedTask_AssignedStaff_Id(Long staffId);

    Optional<Complaint> findByRelatedTaskId(Long taskId);
}
