package com.nagar.parishad.backend.service;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

        private String getHeader() {
                return "<div style='background-color: #f8fafc; padding: 20px; font-family: Arial, sans-serif;'>" +
                                "<div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>"
                                +
                                "<div style='background-color: #ea580c; padding: 20px; text-align: center;'>" +
                                "<h1 style='color: #ffffff; margin: 0; font-size: 24px;'>Nagar Parishad</h1>" +
                                "<p style='color: #fff7ed; margin: 5px 0 0; font-size: 14px;'>Staff Management System</p>"
                                +
                                "</div>" +
                                "<div style='padding: 30px; color: #334155; line-height: 1.6;'>";
        }

        private String getFooter() {
                return "</div>" +
                                "<div style='background-color: #f1f5f9; padding: 20px; text-align: center; color: #64748b; font-size: 12px;'>"
                                +
                                "<p style='margin: 0;'>© 2024 Nagar Parishad Management System. All rights reserved.</p>"
                                +
                                "<p style='margin: 5px 0 0;'>This is an automated message, please do not reply.</p>" +
                                "</div>" +
                                "</div>" +
                                "</div>";
        }

        public String getWelcomeEmail(com.nagar.parishad.backend.entity.User user, String password) {
                return getHeader() +
                                "<h2 style='color: #ea580c; margin-top: 0;'>Welcome Aboard!</h2>" +
                                "<p>Hello <strong>" + user.getName() + "</strong>,</p>" +
                                "<p>Your account has been successfully created. You can now access the Nagar Parishad Staff Management System.</p>"
                                +
                                "<div style='background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; overflow: hidden; margin: 20px 0;'>"
                                +
                                "<table style='width: 100%; border-collapse: collapse;'>" +
                                "<tr>" +
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f1f5f9; width: 35%; font-weight: bold; color: #475569;'>Full Name</td>"
                                +
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>"
                                + user.getName() + "</td>" +
                                "</tr>" +
                                "<tr>" +
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f1f5f9; font-weight: bold; color: #475569;'>Department</td>"
                                +
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>"
                                + (user.getDepartment() != null ? user.getDepartment().getName() : "N/A") + "</td>" +
                                "</tr>" +
                                "<tr>" +
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f1f5f9; font-weight: bold; color: #475569;'>Mobile Number</td>"
                                +
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>"
                                + user.getMobile() + "</td>" +
                                "</tr>" +
                                "<tr>" +
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f1f5f9; font-weight: bold; color: #475569;'>Role</td>"
                                +
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>"
                                + user.getRole() + "</td>" +
                                "</tr>" +
                                "<tr>" +
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f1f5f9; font-weight: bold; color: #475569;'>Login Email</td>"
                                +
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b; font-weight: bold;'>"
                                + user.getEmail() + "</td>" +
                                "</tr>" +
                                "<tr>" +
                                "<td style='padding: 12px 15px; background-color: #f1f5f9; font-weight: bold; color: #475569;'>Password</td>"
                                +
                                "<td style='padding: 12px 15px; color: #ea580c; font-weight: bold;'>" + password
                                + "</td>" +
                                "</tr>" +
                                "</table>" +
                                "</div>" +
                                "<p>Please login and change your password immediately for security purposes.</p>" +
                                "<div style='text-align: center; margin-top: 30px;'>" +
                                "<a href='https://loknagar.in/login' style='background-color: #ea580c; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;'>Login to Dashboard</a>"
                                +
                                "</div>" +
                                getFooter();
        }

        public String getNotificationEmail(String name, String message, String type) {
                String color = "#3b82f6"; // Default blue
                if (type.contains("TASK"))
                        color = "#ea580c"; // Orange
                if (type.contains("STATUS"))
                        color = "#8b5cf6"; // Purple

                return getHeader() +
                                "<h2 style='color: " + color + "; margin-top: 0;'>New Notification</h2>" +
                                "<p>Hello <strong>" + name + "</strong>,</p>" +
                                "<p>You have received a new update:</p>" +
                                "<div style='background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 15px; margin: 20px 0;'>"
                                +
                                "<p style='margin: 0; font-style: italic; color: #475569;'>\"" + message + "\"</p>" +
                                "</div>" +
                                "<div style='text-align: center; margin-top: 30px;'>" +
                                "<a href='https://loknagar.in/tasks' style='background-color: " + color
                                + "; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;'>View Details</a>"
                                +
                                "</div>" +
                                "</div>" +
                                getFooter();
        }

        public String getNotificationEmail(String name, String message, String type, String magicToken,
                        String targetPath) {
                String color = "#3b82f6"; // Default blue
                if (type.contains("TASK"))
                        color = "#ea580c"; // Orange
                if (type.contains("STATUS"))
                        color = "#8b5cf6"; // Purple

                String link = "https://loknagar.in/magic-login?token=" + magicToken + "&target=" + targetPath;

                return getHeader() +
                                "<h2 style='color: " + color + "; margin-top: 0;'>New Notification</h2>" +
                                "<p>Hello <strong>" + name + "</strong>,</p>" +
                                "<p>You have received a new update:</p>" +
                                "<div style='background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 15px; margin: 20px 0;'>"
                                +
                                "<p style='margin: 0; font-style: italic; color: #475569;'>\"" + message + "\"</p>" +
                                "</div>" +
                                "<div style='text-align: center; margin-top: 30px;'>" +
                                "<a href='" + link + "' style='background-color: " + color
                                + "; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;'>View Details</a>"
                                +
                                "</div>" +
                                getFooter();
        }

        public String getTaskAssignmentEmail(com.nagar.parishad.backend.entity.Task task, String magicToken) {
                String color = "#ea580c";
                StringBuilder attachmentInfo = new StringBuilder("No attachments");
                if (task.getAttachments() != null && !task.getAttachments().isEmpty()) {
                        attachmentInfo.setLength(0); // Clear "No attachments"
                        for (com.nagar.parishad.backend.entity.TaskAttachment ta : task.getAttachments()) {
                                if (attachmentInfo.length() > 0)
                                        attachmentInfo.append("<br>");
                                attachmentInfo.append(ta.getFileName());
                        }
                }

                StringBuilder sb = new StringBuilder();
                sb.append(getHeader());
                sb.append("<h2 style='color: ").append(color).append("; margin-top: 0;'>New Task Assigned</h2>");
                sb.append("<p>Hello <strong>").append(task.getAssignedStaff().getName()).append("</strong>,</p>");
                sb.append("<p>You have been assigned a new task by <strong>").append(task.getAdmin().getName())
                                .append("</strong>.</p>");

                sb.append(
                                "<div style='background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 6px; overflow: hidden; margin: 20px 0;'>");
                sb.append("<table style='width: 100%; border-collapse: collapse;'>");

                // Title
                sb.append("<tr>");
                sb.append(
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; width: 30%; font-weight: bold; color: #475569;'>Task Title</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(task.getTitle()).append("</td>");
                sb.append("</tr>");

                // Description
                sb.append("<tr>");
                sb.append(
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; font-weight: bold; color: #475569;'>Description</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(task.getDescription()).append("</td>");
                sb.append("</tr>");

                // Priority
                sb.append("<tr>");
                sb.append(
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; font-weight: bold; color: #475569;'>Priority</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(task.getPriority()).append("</td>");
                sb.append("</tr>");

                // Due Date
                sb.append("<tr>");
                sb.append(
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; font-weight: bold; color: #475569;'>Due Date</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(formatDate(task.getDueDate())).append("</td>");
                sb.append("</tr>");

                // Assigned By
                sb.append("<tr>");
                sb.append(
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; font-weight: bold; color: #475569;'>Assigned By</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(task.getAdmin().getName()).append(" (").append(task.getAdmin().getEmail())
                                .append(")</td>");
                sb.append("</tr>");

                // Assigned Date
                sb.append("<tr>");
                sb.append(
                                "<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; font-weight: bold; color: #475569;'>Assigned Date</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(formatDate(task.getCreatedDate())).append("</td>");
                sb.append("</tr>");

                // Attachments
                sb.append("<tr>");
                sb.append(
                                "<td style='padding: 12px 15px; background-color: #f8fafc; font-weight: bold; color: #475569;'>Attachments</td>");
                sb.append("<td style='padding: 12px 15px; color: #1e293b;'>").append(attachmentInfo).append("</td>");
                sb.append("</tr>");

                sb.append("</table>");
                sb.append("</div>");

                sb.append("<div style='text-align: center; margin-top: 30px;'>");
                String link = "https://loknagar.in/magic-login?token=" + magicToken + "&target=/tasks/" + task.getId();
                sb.append("<a href='").append(link)
                                .append("' style='background-color: ")
                                .append(color)
                                .append("; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;'>View Task Details</a>");
                sb.append("</div>");

                sb.append(getFooter());
                return sb.toString();
        }

        public String getCommentNotificationEmail(com.nagar.parishad.backend.entity.Task task,
                        com.nagar.parishad.backend.entity.TaskComment comment) {
                String color = "#3b82f6"; // Blue
                String commentDate = formatDate(comment.getTimestamp());

                StringBuilder sb = new StringBuilder();
                sb.append(getHeader());
                sb.append("<h2 style='color: ").append(color).append("; margin-top: 0;'>New Comment on Task</h2>");
                sb.append("<p>Hello,</p>");
                sb.append("<p><strong>").append(comment.getUser().getName())
                                .append("</strong> added a comment on task: <strong>").append(task.getTitle())
                                .append("</strong></p>");

                sb.append("<div style='background-color: #f1f5f9; padding: 15px; border-left: 4px solid ").append(color)
                                .append("; margin: 20px 0;'>");
                sb.append("<p style='margin: 0; color: #475569; font-size: 13px;'>").append(commentDate).append("</p>");
                sb.append("<p style='margin: 10px 0 0; font-style: italic;'>\"").append(comment.getText())
                                .append("\"</p>");
                sb.append("</div>");

                sb.append(getFooter());
                return sb.toString();
        }

        public String getStatusUpdateEmail(com.nagar.parishad.backend.entity.Task task,
                        com.nagar.parishad.backend.entity.User modifier, String oldStatus, String newStatus) {
                String color = "#8b5cf6"; // Purple
                String updateDate = formatDate(java.time.LocalDateTime.now());

                StringBuilder sb = new StringBuilder();
                sb.append(getHeader());
                sb.append("<h2 style='color: ").append(color).append("; margin-top: 0;'>Task Status Updated</h2>");
                sb.append("<p>Hello,</p>");
                sb.append("<p>The status of task <strong>").append(task.getTitle())
                                .append("</strong> has been updated by <strong>").append(modifier.getName())
                                .append("</strong>.</p>");

                // Status Change Highlight
                sb.append("<div style='background-color: #f1f5f9; padding: 15px; border-radius: 6px; margin: 20px 0; text-align: center;'>");
                sb.append("<table style='width: 100%; text-align: center;'>");
                sb.append("<tr>");
                sb.append("<td><p style='color: #64748b; font-size: 12px; margin: 0;'>Previous Status</p><p style='font-weight: bold; margin: 5px 0;'>")
                                .append(oldStatus).append("</p></td>");
                sb.append("<td><span style='font-size: 24px; color: #94a3b8;'>→</span></td>");
                sb.append("<td><p style='color: #64748b; font-size: 12px; margin: 0;'>New Status</p><p style='font-weight: bold; margin: 5px 0; color: ")
                                .append(color).append(";'>").append(newStatus).append("</p></td>");
                sb.append("</tr>");
                sb.append("</table>");
                sb.append("</div>");

                // Detailed Task Information
                sb.append("<div style='background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 6px; overflow: hidden; margin: 20px 0;'>");
                sb.append("<table style='width: 100%; border-collapse: collapse;'>");

                // Title
                sb.append("<tr>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; width: 30%; font-weight: bold; color: #475569;'>Task Title</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(task.getTitle()).append("</td>");
                sb.append("</tr>");

                // Description
                sb.append("<tr>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; font-weight: bold; color: #475569;'>Description</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(task.getDescription()).append("</td>");
                sb.append("</tr>");

                // Priority
                sb.append("<tr>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; font-weight: bold; color: #475569;'>Priority</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(task.getPriority()).append("</td>");
                sb.append("</tr>");

                // Due Date
                sb.append("<tr>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; font-weight: bold; color: #475569;'>Due Date</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(formatDate(task.getDueDate())).append("</td>");
                sb.append("</tr>");

                // Assigned Staff
                String assignedStaffName = (task.getAssignedStaff() != null) ? task.getAssignedStaff().getName()
                                : "Unassigned";
                sb.append("<tr>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; font-weight: bold; color: #475569;'>Assigned Staff</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(assignedStaffName).append("</td>");
                sb.append("</tr>");

                // Reported By
                String reporterName = (task.getAdmin() != null) ? task.getAdmin().getName() : "Unknown";
                sb.append("<tr>");
                sb.append("<td style='padding: 12px 15px; background-color: #f8fafc; font-weight: bold; color: #475569;'>Reported By</td>");
                sb.append("<td style='padding: 12px 15px; color: #1e293b;'>")
                                .append(reporterName).append("</td>");
                sb.append("</tr>");

                sb.append("</table>");
                sb.append("</div>");

                sb.append("<div style='text-align: center; margin-top: 30px;'>");
                sb.append("<a href='https://loknagar.in/tasks/").append(task.getId())
                                .append("' style='background-color: ").append(color)
                                .append("; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;'>View Task</a>");
                sb.append("</div>");

                sb.append(getFooter());
                return sb.toString();
        }

        public String getUserUpdateEmail(com.nagar.parishad.backend.entity.User oldUser,
                        com.nagar.parishad.backend.entity.User newUser) {
                String color = "#3b82f6"; // Blue
                StringBuilder sb = new StringBuilder();
                sb.append(getHeader());
                sb.append("<h2 style='color: ").append(color).append("; margin-top: 0;'>Profile Updated</h2>");
                sb.append("<p>Hello <strong>").append(newUser.getName()).append("</strong>,</p>");
                sb.append("<p>Your profile details have been updated. Here is a comparison of the changes:</p>");

                sb.append("<div style='margin: 20px 0; overflow-x: auto;'>");
                sb.append("<table style='width: 100%; border-collapse: collapse; font-size: 14px;'>");
                sb.append("<thead><tr style='background-color: #f1f5f9; text-align: left;'>");
                sb.append("<th style='padding: 10px; border: 1px solid #e2e8f0;'>Field</th>");
                sb.append("<th style='padding: 10px; border: 1px solid #e2e8f0; color: #64748b;'>Old Value</th>");
                sb.append("<th style='padding: 10px; border: 1px solid #e2e8f0; color: #3b82f6;'>New Value</th>");
                sb.append("</tr></thead><tbody>");

                addChangeRow(sb, "Name", oldUser.getName(), newUser.getName());
                addChangeRow(sb, "Email", oldUser.getEmail(), newUser.getEmail());
                addChangeRow(sb, "Mobile", oldUser.getMobile(), newUser.getMobile());
                addChangeRow(sb, "Role", oldUser.getRole() != null ? oldUser.getRole().toString() : "N/A",
                                newUser.getRole().toString());
                addChangeRow(sb, "Department",
                                oldUser.getDepartment() != null ? oldUser.getDepartment().getName() : "N/A",
                                newUser.getDepartment() != null ? newUser.getDepartment().getName() : "N/A");
                addChangeRow(sb, "Designation",
                                oldUser.getDesignation() != null ? oldUser.getDesignation().getName() : "N/A",
                                newUser.getDesignation() != null ? newUser.getDesignation().getName() : "N/A");
                addChangeRow(sb, "Status", oldUser.isActive() ? "Active" : "Inactive",
                                newUser.isActive() ? "Active" : "Inactive");

                sb.append("</tbody></table></div>");

                sb.append("<p>If you did not request these changes, please contact the administrator immediately.</p>");
                sb.append(getFooter());
                return sb.toString();
        }

        public String getTaskUpdateEmail(com.nagar.parishad.backend.entity.Task oldTask,
                        com.nagar.parishad.backend.entity.Task newTask) {
                String color = "#ea580c"; // Orange
                StringBuilder sb = new StringBuilder();
                sb.append(getHeader());
                sb.append("<h2 style='color: ").append(color).append("; margin-top: 0;'>Task Updated</h2>");
                sb.append("<p>Hello,</p>");
                sb.append("<p>The task <strong>").append(newTask.getTitle()).append("</strong> has been updated.</p>");

                sb.append("<div style='margin: 20px 0; overflow-x: auto;'>");
                sb.append("<table style='width: 100%; border-collapse: collapse; font-size: 14px;'>");
                sb.append("<thead><tr style='background-color: #f1f5f9; text-align: left;'>");
                sb.append("<th style='padding: 10px; border: 1px solid #e2e8f0;'>Field</th>");
                sb.append("<th style='padding: 10px; border: 1px solid #e2e8f0; color: #64748b;'>Old Value</th>");
                sb.append("<th style='padding: 10px; border: 1px solid #e2e8f0; color: ").append(color)
                                .append(";'>New Value</th>");
                sb.append("</tr></thead><tbody>");

                addChangeRow(sb, "Title", oldTask.getTitle(), newTask.getTitle());
                addChangeRow(sb, "Description", oldTask.getDescription(), newTask.getDescription());
                addChangeRow(sb, "Priority", oldTask.getPriority().toString(), newTask.getPriority().toString());
                addChangeRow(sb, "Status", oldTask.getStatus().toString(), newTask.getStatus().toString());
                addChangeRow(sb, "Due Date", formatDate(oldTask.getDueDate()), formatDate(newTask.getDueDate()));
                addChangeRow(sb, "Department",
                                oldTask.getDepartment() != null ? oldTask.getDepartment().getName() : "N/A",
                                newTask.getDepartment() != null ? newTask.getDepartment().getName() : "N/A");
                addChangeRow(sb, "Assigned To",
                                oldTask.getAssignedStaff() != null ? oldTask.getAssignedStaff().getName()
                                                : "Unassigned",
                                newTask.getAssignedStaff() != null ? newTask.getAssignedStaff().getName()
                                                : "Unassigned");

                sb.append("</tbody></table></div>");

                sb.append("<div style='text-align: center; margin-top: 30px;'>");
                sb.append("<a href='https://loknagar.in/tasks/").append(newTask.getId())
                                .append("' style='background-color: ").append(color)
                                .append("; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;'>View Task</a>");
                sb.append("</div>");

                sb.append(getFooter());
                return sb.toString();
        }

        private void addChangeRow(StringBuilder sb, String field, String oldValue, String newValue) {
                if (oldValue == null)
                        oldValue = "N/A";
                if (newValue == null)
                        newValue = "N/A";

                boolean changed = !oldValue.equals(newValue);
                String bgStyle = changed ? "background-color: #eff6ff;" : "";

                sb.append("<tr style='").append(bgStyle).append("'>");
                sb.append("<td style='padding: 10px; border: 1px solid #e2e8f0; font-weight: bold;'>").append(field)
                                .append("</td>");
                sb.append("<td style='padding: 10px; border: 1px solid #e2e8f0; color: #64748b;'>").append(oldValue)
                                .append("</td>");
                sb.append("<td style='padding: 10px; border: 1px solid #e2e8f0; font-weight: ")
                                .append(changed ? "bold" : "normal").append(";'>").append(newValue).append("</td>");
                sb.append("</tr>");
        }

        private String formatDate(java.time.LocalDateTime dateTime) {
                if (dateTime == null)
                        return "N/A";
                return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        }

        public String getTaskNotificationForSupervisorEmail(com.nagar.parishad.backend.entity.Task task,
                        com.nagar.parishad.backend.entity.User recipient, String magicToken) {
                String color = "#ea580c";
                StringBuilder attachmentInfo = new StringBuilder("No attachments");
                if (task.getAttachments() != null && !task.getAttachments().isEmpty()) {
                        attachmentInfo.setLength(0);
                        for (com.nagar.parishad.backend.entity.TaskAttachment ta : task.getAttachments()) {
                                if (attachmentInfo.length() > 0)
                                        attachmentInfo.append("<br>");
                                attachmentInfo.append(ta.getFileName());
                        }
                }

                StringBuilder sb = new StringBuilder();
                sb.append(getHeader());
                sb.append("<h2 style='color: ").append(color)
                                .append("; margin-top: 0;'>New Task in Your Department</h2>");
                sb.append("<p>Hello <strong>").append(recipient.getName()).append("</strong>,</p>");
                sb.append("<p>A new task has been assigned to <strong>")
                                .append(task.getAssignedStaff() != null ? task.getAssignedStaff().getName()
                                                : "Unassigned")
                                .append("</strong> by <strong>").append(task.getAdmin().getName())
                                .append("</strong>.</p>");

                sb.append("<div style='background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 6px; overflow: hidden; margin: 20px 0;'>");
                sb.append("<table style='width: 100%; border-collapse: collapse;'>");

                // Title
                sb.append("<tr>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; width: 30%; font-weight: bold; color: #475569;'>Task Title</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(task.getTitle()).append("</td>");
                sb.append("</tr>");

                // Description
                sb.append("<tr>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; font-weight: bold; color: #475569;'>Description</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(task.getDescription()).append("</td>");
                sb.append("</tr>");

                // Priority
                sb.append("<tr>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; font-weight: bold; color: #475569;'>Priority</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(task.getPriority()).append("</td>");
                sb.append("</tr>");

                // Assigned Staff
                sb.append("<tr>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; font-weight: bold; color: #475569;'>Assigned Staff</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(task.getAssignedStaff() != null ? task.getAssignedStaff().getName()
                                                : "Unassigned")
                                .append("</td>");
                sb.append("</tr>");

                // Due Date
                sb.append("<tr>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; background-color: #f8fafc; font-weight: bold; color: #475569;'>Due Date</td>");
                sb.append("<td style='padding: 12px 15px; border-bottom: 1px solid #e2e8f0; color: #1e293b;'>")
                                .append(formatDate(task.getDueDate())).append("</td>");
                sb.append("</tr>");

                sb.append("</table>");
                sb.append("</div>");

                sb.append("<div style='text-align: center; margin-top: 30px;'>");
                String link = "https://loknagar.in/magic-login?token=" + magicToken + "&target=/tasks/" + task.getId();
                sb.append("<a href='").append(link)
                                .append("' style='background-color: ").append(color)
                                .append("; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;'>View Task Details</a>");
                sb.append("</div>");

                sb.append(getFooter());
                return sb.toString();
        }
}
