SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS task_attachments;
DROP TABLE IF EXISTS task_comments;
DROP TABLE IF EXISTS task_history;
DROP TABLE IF EXISTS complaints;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS subscriptions;
DROP TABLE IF EXISTS chatbot_config;
DROP TABLE IF EXISTS chatbot_sessions;
DROP TABLE IF EXISTS tenant_twilio_config;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS designations;
DROP TABLE IF EXISTS complaint_types;
DROP TABLE IF EXISTS complaint_attachments;
DROP TABLE IF EXISTS complaint_comments;

create table designations (active bit not null, id bigint not null auto_increment, name varchar(255) not null, primary key (id)) engine=InnoDB;

create table users (active bit not null, admin_id bigint, created_at datetime(6), department_id bigint, designation_id bigint, id bigint not null auto_increment, reset_password_token_expiry datetime(6), email varchar(255) not null, mobile varchar(255), name varchar(255) not null, password varchar(255) not null, reset_password_token varchar(255), role enum ('OWNER','ADMIN','DEPARTMENT_HEAD','STAFF') not null, primary key (id)) engine=InnoDB;

create table departments (active bit not null, chatbot_enabled bit, admin_id bigint not null, created_at datetime(6), id bigint not null auto_increment, name varchar(255) not null, name_hi varchar(255), name_mr varchar(255), sub_questions TEXT, primary key (id)) engine=InnoDB;

create table tasks (admin_id bigint not null, assigned_staff_id bigint, created_date datetime(6), department_id bigint, due_date datetime(6), id bigint not null auto_increment, description TEXT, title varchar(255) not null, type varchar(255), priority enum ('MINOR','LOW','MEDIUM','HIGH','CRITICAL') not null, status enum ('TO_DO','IN_PROGRESS','ON_HOLD','COMPLETED') not null, primary key (id)) engine=InnoDB;

create table task_comments (id bigint not null auto_increment, task_id bigint not null, timestamp datetime(6), user_id bigint not null, text TEXT not null, primary key (id)) engine=InnoDB;

create table task_history (id bigint not null auto_increment, modified_by_id bigint not null, task_id bigint not null, timestamp datetime(6) not null, action varchar(255) not null, new_value varchar(255), old_value varchar(255), primary key (id)) engine=InnoDB;

create table task_attachments (comment_id bigint, file_size bigint, id bigint not null auto_increment, task_id bigint not null, uploaded_by bigint not null, uploaded_on datetime(6), file_name varchar(255) not null, file_path varchar(255) not null, file_type varchar(255), primary key (id)) engine=InnoDB;

create table complaint_types (active bit not null, department_id bigint not null, id bigint not null auto_increment, name_en varchar(255) not null, name_mr varchar(255) not null, primary key (id)) engine=InnoDB;

create table complaints (complaint_type_id bigint, created_at datetime(6), department_id bigint, id bigint not null auto_increment, related_task_id bigint, citizen_mobile varchar(255), citizen_name varchar(255), complaint_no varchar(255) not null, description TEXT, location TEXT, photo_url varchar(255), rejection_reason TEXT, status enum ('PENDING','REJECTED','ACCEPTED','CONVERTED_TO_TASK'), primary key (id)) engine=InnoDB;

create table complaint_attachments (file_size bigint, complaint_id bigint not null, id bigint not null auto_increment, uploaded_by bigint not null, uploaded_on datetime(6), file_name varchar(255) not null, file_path varchar(255) not null, file_type varchar(255), primary key (id)) engine=InnoDB;

create table complaint_comments (complaint_id bigint not null, id bigint not null auto_increment, timestamp datetime(6), user_id bigint not null, text TEXT not null, primary key (id)) engine=InnoDB;

create table notifications (is_read bit not null, created_at datetime(6), id bigint not null auto_increment, recipient_id bigint not null, related_task_id bigint, sender_id bigint, message varchar(255), type enum ('TASK_ASSIGNED','STATUS_CHANGED','COMMENT_ADDED'), primary key (id)) engine=InnoDB;

create table subscriptions (active bit not null, end_date date not null, start_date date not null, admin_id bigint not null, created_at datetime(6), id bigint not null auto_increment, plan_type varchar(255), primary key (id)) engine=InnoDB;

create table tenant_twilio_config (active bit not null, admin_id bigint not null, id bigint not null auto_increment, account_sid varchar(255) not null, auth_token varchar(255) not null, phone_number varchar(255) not null, primary key (id)) engine=InnoDB;

create table chatbot_config (admin_id bigint not null, id bigint not null auto_increment, conf_key varchar(255) not null, conf_value varchar(255) not null, primary key (id)) engine=InnoDB;

create table chatbot_sessions (admin_id bigint not null, id bigint not null auto_increment, last_updated datetime(6), mobile_number varchar(255) not null, state varchar(100), temp_data TEXT, language varchar(10), primary key (id)) engine=InnoDB;

-- Add Constraints
alter table chatbot_config add constraint UK_chatbot_config unique (admin_id, conf_key);
alter table chatbot_sessions add constraint UK_chatbot_sessions unique (mobile_number, admin_id);
alter table complaints add constraint UK_complaints_task unique (related_task_id);
alter table complaints add constraint UK_complaints_no unique (complaint_no);
alter table designations add constraint UK_designations_name unique (name);
alter table subscriptions add constraint UK_subscriptions_admin unique (admin_id);
alter table tenant_twilio_config add constraint UK_tenant_admin unique (admin_id);
alter table tenant_twilio_config add constraint UK_tenant_phone unique (phone_number);
alter table users add constraint UK_users_email unique (email);

alter table chatbot_config add constraint FK_chatbot_config_admin foreign key (admin_id) references users (id);
alter table chatbot_sessions add constraint FK_chatbot_sessions_admin foreign key (admin_id) references users (id);
alter table complaint_types add constraint FK_complaint_type_dept foreign key (department_id) references departments (id);
alter table complaints add constraint FK_complaints_dept foreign key (department_id) references departments (id);
alter table complaints add constraint FK_complaints_task foreign key (related_task_id) references tasks (id);
alter table complaints add constraint FK_complaints_type foreign key (complaint_type_id) references complaint_types (id);
alter table complaint_attachments add constraint FK_comp_att_comp foreign key (complaint_id) references complaints (id);
alter table complaint_attachments add constraint FK_comp_att_user foreign key (uploaded_by) references users (id);
alter table complaint_comments add constraint FK_comp_comm_comp foreign key (complaint_id) references complaints (id);
alter table complaint_comments add constraint FK_comp_comm_user foreign key (user_id) references users (id);
alter table departments add constraint FK_departments_admin foreign key (admin_id) references users (id);
alter table notifications add constraint FK_notifications_recipient foreign key (recipient_id) references users (id);
alter table notifications add constraint FK_notifications_sender foreign key (sender_id) references users (id);
alter table subscriptions add constraint FK_subscriptions_admin foreign key (admin_id) references users (id);
alter table task_attachments add constraint FK_attachments_comment foreign key (comment_id) references task_comments (id);
alter table task_attachments add constraint FK_attachments_task foreign key (task_id) references tasks (id);
alter table task_attachments add constraint FK_attachments_user foreign key (uploaded_by) references users (id);
alter table task_comments add constraint FK_comments_task foreign key (task_id) references tasks (id);
alter table task_comments add constraint FK_comments_user foreign key (user_id) references users (id);
alter table task_history add constraint FK_history_user foreign key (modified_by_id) references users (id);
alter table task_history add constraint FK_history_task foreign key (task_id) references tasks (id);
alter table tasks add constraint FK_tasks_admin foreign key (admin_id) references users (id);
alter table tasks add constraint FK_tasks_staff foreign key (assigned_staff_id) references users (id);
alter table tasks add constraint FK_tasks_dept foreign key (department_id) references departments (id);
alter table tenant_twilio_config add constraint FK_tenant_admin foreign key (admin_id) references users (id);
alter table users add constraint FK_users_admin foreign key (admin_id) references users (id);
alter table users add constraint FK_users_dept foreign key (department_id) references departments (id);
alter table users add constraint FK_users_designation foreign key (designation_id) references designations (id);

-- SEED DATA
-- Insert Owner (ID 1)
INSERT INTO users (id, name, email, password, role, mobile, active) 
VALUES (1, 'Super Owner', 'owner@govt.in', '$2a$10$N.zmdr9k7uOCQb376NoUnutj8iAt6.VwUEMOx9gdmogCp.Jb.Go2C', 'OWNER', '9876543210', TRUE);

-- Insert Admin (ID 2)
INSERT INTO users (id, name, email, password, role, admin_id, active) 
VALUES (2, 'Ram Patil (Admin)', 'admin@nagarparishad.in', '$2a$10$N.zmdr9k7uOCQb376NoUnutj8iAt6.VwUEMOx9gdmogCp.Jb.Go2C', 'ADMIN', 1, TRUE);

-- Insert Tenant Config
INSERT INTO tenant_twilio_config (admin_id, phone_number, account_sid, auth_token, active)
VALUES (2, 'whatsapp:+919876543210', 'AC_PENDING', 'AUTH_PENDING', TRUE);

-- Insert Departments (ID 1-5) linked to Admin (ID 2)
-- 1. City Cleanliness
INSERT INTO departments (id, name, name_mr, sub_questions, admin_id, active, chatbot_enabled, created_at) 
VALUES (1, 'City Cleanliness', 'शहर स्वच्छता', '["स्वच्छता होत नाही लवकर", "मेलेले जनावर तक्रार", "नाली गटार स्वच्छता तक्रार", "रस्त्यावर कचरा तक्रार", "रस्त्यावरील बांधकाम", "कचरा जाळणे तक्रार", "इतर तक्रार"]', 2, TRUE, TRUE, CURRENT_TIMESTAMP);

-- 2. Water Supply
INSERT INTO departments (id, name, name_mr, sub_questions, admin_id, active, chatbot_enabled, created_at) 
VALUES (2, 'Water Supply', 'पाणी पुरवठा', '["पाणी येत नाही तक्रार", "पाईपलाईन लिकेज तक्रार", "पाणी दबाव तक्रार", "अनधिकृत नळ तक्रार", "पाणी खराब येत असल्याबाबत"]', 2, TRUE, TRUE, CURRENT_TIMESTAMP);

-- 3. Electricity
INSERT INTO departments (id, name, name_mr, sub_questions, admin_id, active, chatbot_enabled, created_at) 
VALUES (3, 'Electricity', 'विद्युत', '["विद्युत पोलवरील दिवा बंद", "पोल दिवा सतत चालू", "हायमास्ट बंद असल्याबाबत"]', 2, TRUE, TRUE, CURRENT_TIMESTAMP);

-- 4. Public Works
INSERT INTO departments (id, name, name_mr, sub_questions, admin_id, active, chatbot_enabled, created_at) 
VALUES (4, 'Public Works', 'सार्वजनिक बांधकाम', '["चेंबर / ढापा / मॅनहोल तुटलेले"]', 2, TRUE, TRUE, CURRENT_TIMESTAMP);

-- 5. Waste Vehicle
INSERT INTO departments (id, name, name_mr, sub_questions, admin_id, active, chatbot_enabled, created_at) 
VALUES (5, 'Waste Vehicle (Ghanta Gadi)', 'घंटागाडी', '["घंटागाडी येत नाही", "मेलेले प्राणी उचलणे बाबत", "गटार साफ / चोक अप दुरुस्ती", "साचलेला कचरा उचलणे"]', 2, TRUE, TRUE, CURRENT_TIMESTAMP);

-- Insert Dept Head (ID 3)
INSERT INTO users (id, name, email, password, role, admin_id, department_id, active) 
VALUES (3, 'Suresh (Ghantagadi Head)', 'head@ghantagadi.in', '$2a$10$N.zmdr9k7uOCQb376NoUnutj8iAt6.VwUEMOx9gdmogCp.Jb.Go2C', 'DEPARTMENT_HEAD', 2, 5, TRUE);

-- Insert Staff (ID 4)
INSERT INTO users (id, name, email, password, role, admin_id, department_id, active) 
VALUES (4, 'Ramesh (Driver)', 'ramesh@ghantagadi.in', '$2a$10$N.zmdr9k7uOCQb376NoUnutj8iAt6.VwUEMOx9gdmogCp.Jb.Go2C', 'STAFF', 2, 5, TRUE);

SET FOREIGN_KEY_CHECKS = 1;

