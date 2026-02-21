package com.nagar.parishad.backend.controller;

import com.nagar.parishad.backend.entity.Complaint;
import com.nagar.parishad.backend.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/admin/migration")
public class MigrationController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @PostMapping("/fix-descriptions")
    public String fixDescriptions() {
        List<Complaint> complaints = complaintRepository.findAll();
        int updatedCount = 0;
        StringBuilder log = new StringBuilder();

        // Regex to capture "Issue: <SubIssue> --- Additional Details --- <Description>"
        // Pattern: Starts with "Issue: ", captures content until " --- Additional
        // Details --- "
        // Then captures the rest.
        // Also handle cases without "Issue: " prefix if necessary, but user screenshot
        // showed "Issue: "

        // Strict pattern based on observation: "Issue: <Content> --- Additional Details
        // --- <Content>"
        // Note: The "Issue: " part might be coming from the key in the extraInfo loop
        // if the key was "Issue".

        for (Complaint c : complaints) {
            String originalDesc = c.getDescription();
            if (originalDesc == null)
                continue;

            boolean changed = false;
            String newDesc = originalDesc;
            String subIssue = c.getSubComplaintType();

            // Strategy 1: Remove "Issue: ... --- Additional Details ---"
            if (originalDesc.contains("--- Additional Details ---")) {
                // Split by the separator
                String[] parts = originalDesc.split("--- Additional Details ---");

                if (parts.length > 0) {
                    // Part 0 often contains the Sub Issue
                    String prePart = parts[0].trim();

                    // Check if Part 0 looks like "Issue: <SubIssue>"
                    if (prePart.startsWith("Issue:")) {
                        String extractedSubIssue = prePart.substring(6).trim(); // Remove "Issue:"

                        // If subIssue column is empty, populate it
                        if (subIssue == null || subIssue.isEmpty()) {
                            c.setSubComplaintType(extractedSubIssue);
                            changed = true;
                        }

                        // Clean up description: keep parts after separator
                        if (parts.length > 1) {
                            newDesc = parts[1].trim();

                            // Also check if part 1 has duplicate info or is just keys
                            // Often part 1 starts with "citizenName: ..." etc.
                            // User wants "description_detail" which might be mixed in.
                            // If the original description was constructed as:
                            // description = subIssue + "\n\n--- Additional Details ---" + extraInfo
                            // Then the REAL description might be missing or hidden in extraInfo?
                            // Wait, if "description_detail" was missing, then description IS the subIssue.
                            // User said: "this should go in description, कृपया समस्येबाबत थोडक्यात माहिती
                            // द्या" (description_detail)

                            // If we have proper description_detail in the map, it should be in the text??
                            // In old code: description = subIssue. extraInfo appended.
                            // So the SUB ISSUE is the description.
                            // If so, we should ERASE the description if it's just the sub issue?
                            // Or keep it as fallback?

                            // If description IS "Issue: ...", effectively we remove it from description
                            // col.
                            // But we need to see if there's any other text.
                        } else {
                            // Only part 0 existed.
                            // If we moved it to SubComplaintType, Description becomes empty?
                            // Maybe better to keep it empty than duplicative.
                            newDesc = "";
                        }
                    } else {
                        // Separator exists but "Issue:" prefix missing.
                        // Maybe just "Gatar saf..."
                        // Move prePart to subIssue if subIssue empty
                        if (subIssue == null || subIssue.isEmpty()) {
                            c.setSubComplaintType(prePart);
                            changed = true;
                        }
                        if (parts.length > 1) {
                            newDesc = parts[1].trim();
                        } else {
                            newDesc = "";
                        }
                    }
                }
            }
            // Strategy 2: Check for direct duplication if no separator
            else if (subIssue != null && !subIssue.isEmpty() && originalDesc.startsWith(subIssue)) {
                newDesc = originalDesc.substring(subIssue.length()).trim();
                // Remove leading non-alphanumeric
                newDesc = newDesc.replaceFirst("^[^a-zA-Z0-9\u0900-\u097F]+", ""); // Include Marathi range roughly or
                                                                                   // just punctuation
                if (!newDesc.equals(originalDesc)) {
                    // changed = true; // Wait, only apply this if we are confident
                }
            }

            if (!newDesc.equals(originalDesc) || changed) {
                c.setDescription(newDesc);
                complaintRepository.save(c);
                updatedCount++;
                log.append("Updated Complaint ").append(c.getComplaintNo()).append("\n");
            }
        }

        return "Migration Completed. Updated: " + updatedCount + "\n" + log.toString();
    }
}
