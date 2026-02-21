package com.nagar.parishad.backend.service;

import com.nagar.parishad.backend.dto.ComplaintTypeDTO;
import com.nagar.parishad.backend.entity.ComplaintType;
import com.nagar.parishad.backend.entity.Department;
import com.nagar.parishad.backend.repository.ComplaintTypeRepository;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplaintTypeService {

    @Autowired
    private ComplaintTypeRepository complaintTypeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    public List<ComplaintTypeDTO> getAllComplaintTypes() {
        return complaintTypeRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<ComplaintTypeDTO> getComplaintTypesByDepartment(Long deptId) {
        return complaintTypeRepository.findByDepartmentId(deptId).stream().map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ComplaintTypeDTO createComplaintType(ComplaintTypeDTO dto) {
        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        ComplaintType type = new ComplaintType();
        type.setDepartment(dept);
        type.setNameMr(dto.getNameMr());
        type.setNameEn(dto.getNameEn());
        type.setActive(true);

        return convertToDTO(complaintTypeRepository.save(type));
    }

    public ComplaintTypeDTO updateComplaintType(Long id, ComplaintTypeDTO dto) {
        ComplaintType type = complaintTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint Type not found"));

        type.setNameMr(dto.getNameMr());
        type.setNameEn(dto.getNameEn());
        type.setActive(dto.isActive());

        return convertToDTO(complaintTypeRepository.save(type));
    }

    public void deleteComplaintType(Long id) {
        complaintTypeRepository.deleteById(id);
    }

    private ComplaintTypeDTO convertToDTO(ComplaintType entity) {
        ComplaintTypeDTO dto = new ComplaintTypeDTO();
        dto.setId(entity.getId());
        dto.setDepartmentId(entity.getDepartment().getId());
        dto.setDepartmentName(entity.getDepartment().getName());
        dto.setNameMr(entity.getNameMr());
        dto.setNameEn(entity.getNameEn());
        dto.setActive(entity.isActive());
        return dto;
    }
}
