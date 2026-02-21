package com.nagar.parishad.backend.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.entity.Task;
import com.nagar.parishad.backend.enums.TaskStatus;
import com.nagar.parishad.backend.repository.TaskRepository;
import com.nagar.parishad.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.util.List;

@Service
public class PdfReportService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    public byte[] generateAdminReport() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Header
            addHeader(document);

            // 2. Executive Summary
            addExecutiveSummary(document);

            // 3. User Performance Table
            addUserPerformanceTable(document);

            // 4. Detailed Task List
            addAllTasksTable(document);

            // 5. Footer
            addFooter(document);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating PDF report", e);
        }
    }

    // ... existing headers ...

    // ... existing executive summary ...

    // ... existing user performance table ...

    private void addAllTasksTable(Document document) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
        Paragraph sectionTitle = new Paragraph("Detailed Task List", headerFont);
        sectionTitle.setSpacingBefore(20);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(5); // Title, Status, Priority, Assignee, Due Date
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setWidths(new float[] { 3, 2, 2, 2, 2 });

        // Header
        addTableHeader(table, "Task Title");
        addTableHeader(table, "Status");
        addTableHeader(table, "Priority");
        addTableHeader(table, "Assigned Staff");
        addTableHeader(table, "Due Date");

        List<Task> allTasks = taskRepository.findAll(org.springframework.data.domain.Sort
                .by(org.springframework.data.domain.Sort.Direction.DESC, "createdDate"));

        boolean alternate = false;
        for (Task task : allTasks) {
            Color bg = alternate ? new Color(248, 250, 252) : Color.WHITE; // Light Gray / White

            // Status Color Logic
            Color statusColor = Color.BLACK;
            if (task.getStatus() == TaskStatus.COMPLETED)
                statusColor = new Color(22, 163, 74); // Green
            else if (task.getStatus() == TaskStatus.IN_PROGRESS)
                statusColor = new Color(202, 138, 4); // Yellow/Orange
            else if (task.getStatus() == TaskStatus.TO_DO)
                statusColor = new Color(220, 38, 38); // Red

            addTableCell(table, task.getTitle(), bg);

            // Colored Status Text Cell
            PdfPCell statusCell = new PdfPCell(
                    new Phrase(task.getStatus().name(), FontFactory.getFont(FontFactory.HELVETICA, 12, statusColor)));
            statusCell.setBackgroundColor(bg);
            statusCell.setPadding(8);
            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(statusCell);

            addTableCell(table, task.getPriority().name(), bg);
            addTableCell(table, task.getAssignedStaff() != null ? task.getAssignedStaff().getName() : "Unassigned", bg);
            addTableCell(table, task.getDueDate() != null ? task.getDueDate().toLocalDate().toString() : "N/A", bg);

            alternate = !alternate;
        }

        document.add(table);
    }

    private void addHeader(Document document) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Color.ORANGE);
        Paragraph title = new Paragraph("Nagar Parishad", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 16, Color.DARK_GRAY);
        Paragraph subtitle = new Paragraph("Admin Executive Report", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);

        Paragraph date = new Paragraph("Generated on: " + java.time.LocalDate.now().toString());
        date.setAlignment(Element.ALIGN_RIGHT);
        date.setSpacingAfter(30);
        document.add(date);
    }

    private void addExecutiveSummary(Document document) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
        document.add(new Paragraph("Executive Summary", headerFont));

        long totalTasks = taskRepository.count();
        long completedTasks = taskRepository.countByStatus(TaskStatus.COMPLETED);
        long pendingTasks = taskRepository.countByStatus(TaskStatus.TO_DO);
        long inProgressTasks = taskRepository.countByStatus(TaskStatus.IN_PROGRESS);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(20);

        addMetricCell(table, "Total Tasks", String.valueOf(totalTasks), Color.LIGHT_GRAY);
        addMetricCell(table, "Completed", String.valueOf(completedTasks), new Color(220, 252, 231)); // Green-ish
        addMetricCell(table, "In Progress", String.valueOf(inProgressTasks), new Color(254, 249, 195)); // Yellow-ish
        addMetricCell(table, "Pending", String.valueOf(pendingTasks), new Color(254, 226, 226)); // Red-ish

        document.add(table);
    }

    private void addMetricCell(PdfPTable table, String label, String value, Color bgColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bgColor);
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        cell.addElement(new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
        cell.addElement(new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA, 10)));

        table.addCell(cell);
    }

    private void addUserPerformanceTable(Document document) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
        document.add(new Paragraph("Staff Performance", headerFont));

        PdfPTable table = new PdfPTable(4); // Name, Total, Completed, Efficiency
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setWidths(new float[] { 3, 1, 1, 1 });

        // Table Header
        addTableHeader(table, "Staff Name");
        addTableHeader(table, "Total Assigned");
        addTableHeader(table, "Completed");
        addTableHeader(table, "Pending");

        List<User> staffMembers = userRepository.findByRole(com.nagar.parishad.backend.enums.Role.STAFF);

        boolean alternate = false;
        for (User staff : staffMembers) {
            long total = taskRepository.countByAssignedStaffId(staff.getId());
            long completed = taskRepository.countByAssignedStaffIdAndStatus(staff.getId(), TaskStatus.COMPLETED);
            long pending = total - completed;

            Color bg = alternate ? new Color(248, 250, 252) : Color.WHITE;

            addTableCell(table, staff.getName(), bg);
            addTableCell(table, String.valueOf(total), bg);
            addTableCell(table, String.valueOf(completed), bg);
            addTableCell(table, String.valueOf(pending), bg);

            alternate = !alternate;
        }

        document.add(table);
    }

    private void addTableHeader(PdfPTable table, String title) {
        PdfPCell cell = new PdfPCell(
                new Phrase(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE)));
        cell.setBackgroundColor(Color.ORANGE);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBackgroundColor(bg);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph("© 2024 Nagar Parishad Management System. Internal Use Only.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(30);
        document.add(footer);
    }
}
