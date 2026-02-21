package com.nagar.parishad.backend.service;

import com.nagar.parishad.backend.entity.Designation;
import com.nagar.parishad.backend.repository.DesignationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DesignationService {

    @Autowired
    DesignationRepository designationRepository;

    public List<Designation> getAll() {
        return designationRepository.findAll();
    }

    public Designation createDesignation(String name) {
        Designation designation = new Designation();
        designation.setName(name);
        return designationRepository.save(designation);
    }

    public void delete(Long id) {
        designationRepository.deleteById(id);
    }
}
