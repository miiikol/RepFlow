-- ReqFlow 数据库结构（H2 MySQL 模式 / MySQL 8 兼容）
CREATE TABLE sys_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort INT DEFAULT 0,
    created_at DATETIME
);

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    dept_id BIGINT,
    email VARCHAR(100),
    phone VARCHAR(30),
    status INT DEFAULT 1,
    must_change_pwd INT DEFAULT 0,
    fail_count INT DEFAULT 0,
    lock_until DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    deleted INT DEFAULT 0,
    CONSTRAINT uk_user_username UNIQUE (username)
);

CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(50) NOT NULL,
    data_scope VARCHAR(20) NOT NULL,
    builtin INT DEFAULT 1,
    created_at DATETIME,
    CONSTRAINT uk_role_code UNIQUE (code)
);

CREATE TABLE sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(60) NOT NULL,
    module VARCHAR(30),
    CONSTRAINT uk_perm_code UNIQUE (code)
);

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE sys_sequence (
    seq_key VARCHAR(40) PRIMARY KEY,
    seq_date VARCHAR(8),
    seq_value INT DEFAULT 0
);

CREATE TABLE requirement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    req_no VARCHAR(30) NOT NULL,
    title VARCHAR(120) NOT NULL,
    req_type VARCHAR(30) NOT NULL,
    req_source VARCHAR(30) NOT NULL,
    dept_id BIGINT NOT NULL,
    proposer_id BIGINT NOT NULL,
    analyst_id BIGINT,
    pm_id BIGINT,
    priority VARCHAR(10) NOT NULL,
    expect_date DATE,
    plan_date DATE,
    business_background TEXT,
    req_description TEXT,
    acceptance_criteria TEXT,
    status VARCHAR(30) NOT NULL,
    hold_from_status VARCHAR(30),
    version_no VARCHAR(10) DEFAULT 'V0.1',
    change_count INT DEFAULT 0,
    project_id BIGINT,
    milestone_id BIGINT,
    tags VARCHAR(100),
    archived_at DATETIME,
    archived_by BIGINT,
    reject_reason VARCHAR(500),
    created_at DATETIME,
    updated_at DATETIME,
    created_by BIGINT,
    updated_by BIGINT,
    deleted INT DEFAULT 0,
    CONSTRAINT uk_req_no UNIQUE (req_no)
);

CREATE TABLE requirement_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requirement_id BIGINT NOT NULL,
    version VARCHAR(10) NOT NULL,
    reason VARCHAR(100),
    snapshot_json TEXT,
    operator_id BIGINT,
    created_at DATETIME
);

CREATE TABLE requirement_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requirement_id BIGINT NOT NULL,
    file_name VARCHAR(255),
    stored_name VARCHAR(100),
    file_size BIGINT,
    content_type VARCHAR(100),
    uploader_id BIGINT,
    created_at DATETIME
);

CREATE TABLE review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_no VARCHAR(30) NOT NULL,
    requirement_id BIGINT NOT NULL,
    review_type VARCHAR(30),
    review_method VARCHAR(30),
    host_id BIGINT,
    reviewer_ids VARCHAR(100),
    deadline DATETIME,
    status VARCHAR(20),
    conclusion VARCHAR(30),
    comment_text VARCHAR(1000),
    snapshot_json TEXT,
    created_at DATETIME,
    closed_at DATETIME,
    CONSTRAINT uk_review_no UNIQUE (review_no)
);

CREATE TABLE review_opinion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    result VARCHAR(20),
    content VARCHAR(1000),
    created_at DATETIME
);

CREATE TABLE change_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    change_no VARCHAR(30) NOT NULL,
    requirement_id BIGINT NOT NULL,
    reason VARCHAR(500),
    content TEXT,
    impact_hours DECIMAL(10,1),
    impact_desc VARCHAR(1000),
    status VARCHAR(20),
    applicant_id BIGINT,
    approver_id BIGINT,
    approve_comment VARCHAR(500),
    approved_at DATETIME,
    applied_at DATETIME,
    created_at DATETIME,
    CONSTRAINT uk_change_no UNIQUE (change_no)
);

CREATE TABLE project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_no VARCHAR(30) NOT NULL,
    name VARCHAR(120) NOT NULL,
    dept_id BIGINT,
    pm_id BIGINT,
    status VARCHAR(20),
    start_date DATE,
    end_date DATE,
    created_at DATETIME,
    deleted INT DEFAULT 0,
    CONSTRAINT uk_project_no UNIQUE (project_no)
);

CREATE TABLE milestone (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    plan_date DATE,
    actual_date DATE,
    status VARCHAR(20)
);

CREATE TABLE task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_no VARCHAR(30) NOT NULL,
    requirement_id BIGINT NOT NULL,
    project_id BIGINT,
    milestone_id BIGINT,
    parent_id BIGINT,
    name VARCHAR(150) NOT NULL,
    task_type VARCHAR(20),
    assignee_id BIGINT,
    estimate_hours DECIMAL(10,1),
    actual_hours DECIMAL(10,1),
    plan_date DATE,
    status VARCHAR(20),
    block_reason VARCHAR(500),
    finish_note VARCHAR(500),
    finished_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    deleted INT DEFAULT 0,
    CONSTRAINT uk_task_no UNIQUE (task_no)
);

CREATE TABLE deliverable (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    deliverable_no VARCHAR(30) NOT NULL,
    requirement_id BIGINT NOT NULL,
    deliverable_type VARCHAR(30),
    name VARCHAR(150) NOT NULL,
    file_name VARCHAR(255),
    stored_name VARCHAR(100),
    file_size BIGINT,
    description VARCHAR(500),
    uploader_id BIGINT,
    created_at DATETIME,
    deleted INT DEFAULT 0,
    CONSTRAINT uk_deliverable_no UNIQUE (deliverable_no)
);

CREATE TABLE acceptance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    acceptance_no VARCHAR(30) NOT NULL,
    requirement_id BIGINT NOT NULL,
    round_no INT DEFAULT 1,
    submitter_id BIGINT,
    acceptor_id BIGINT,
    deliverable_snapshot TEXT,
    status VARCHAR(20),
    conclusion VARCHAR(20),
    comment_text VARCHAR(500),
    rework_requirement VARCHAR(500),
    submitted_at DATETIME,
    confirmed_at DATETIME,
    CONSTRAINT uk_acceptance_no UNIQUE (acceptance_no)
);

CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    role_codes VARCHAR(200),
    ip VARCHAR(64),
    uri VARCHAR(255),
    method VARCHAR(10),
    module VARCHAR(50),
    action VARCHAR(100),
    object_type VARCHAR(50),
    object_id VARCHAR(50),
    result VARCHAR(10),
    error_msg VARCHAR(500),
    duration_ms BIGINT,
    trace_id VARCHAR(50),
    created_at DATETIME
);

CREATE INDEX idx_req_status ON requirement (status);
CREATE INDEX idx_req_dept ON requirement (dept_id);
CREATE INDEX idx_req_proposer ON requirement (proposer_id);
CREATE INDEX idx_req_analyst ON requirement (analyst_id);
CREATE INDEX idx_req_pm ON requirement (pm_id);
CREATE INDEX idx_req_project ON requirement (project_id);
CREATE INDEX idx_task_requirement ON task (requirement_id);
CREATE INDEX idx_task_assignee ON task (assignee_id);
CREATE INDEX idx_review_requirement ON review (requirement_id);
CREATE INDEX idx_change_requirement ON change_request (requirement_id);
CREATE INDEX idx_deliverable_requirement ON deliverable (requirement_id);
CREATE INDEX idx_acceptance_requirement ON acceptance (requirement_id);
CREATE INDEX idx_audit_object ON audit_log (object_type, object_id);
CREATE INDEX idx_audit_user ON audit_log (user_id);
