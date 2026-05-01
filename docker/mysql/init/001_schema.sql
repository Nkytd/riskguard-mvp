USE riskguard;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    real_name VARCHAR(64),
    role_code VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(64) NOT NULL UNIQUE,
    rule_name VARCHAR(128) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    expression_text TEXT NOT NULL,
    score INT NOT NULL DEFAULT 0,
    action VARCHAR(32) NOT NULL,
    priority INT NOT NULL DEFAULT 100,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    version INT NOT NULL DEFAULT 0,
    description VARCHAR(512),
    created_by BIGINT,
    updated_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rule_event_status (event_type, status),
    INDEX idx_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_rule_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    version INT NOT NULL,
    expression_text TEXT NOT NULL,
    score INT NOT NULL DEFAULT 0,
    action VARCHAR(32) NOT NULL,
    priority INT NOT NULL DEFAULT 100,
    status VARCHAR(32) NOT NULL,
    publish_note VARCHAR(512),
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rule_version (rule_id, version),
    INDEX idx_rule_version_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_strategy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    strategy_code VARCHAR(64) NOT NULL UNIQUE,
    strategy_name VARCHAR(128) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    version INT NOT NULL DEFAULT 0,
    gray_ratio INT NOT NULL DEFAULT 100,
    description VARCHAR(512),
    created_by BIGINT,
    updated_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_strategy_event_status (event_type, status),
    INDEX idx_strategy_code (strategy_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_strategy_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    strategy_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    rule_version INT NOT NULL,
    execute_order INT NOT NULL DEFAULT 100,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_strategy_rule (strategy_id, rule_id),
    INDEX idx_strategy_rule_strategy (strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_strategy_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    strategy_id BIGINT NOT NULL,
    version INT NOT NULL,
    rule_snapshot JSON NOT NULL,
    gray_ratio INT NOT NULL DEFAULT 100,
    status VARCHAR(32) NOT NULL,
    publish_note VARCHAR(512),
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_strategy_version (strategy_id, version),
    INDEX idx_strategy_version_strategy (strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_list (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    list_type VARCHAR(32) NOT NULL,
    object_type VARCHAR(32) NOT NULL,
    object_value VARCHAR(128) NOT NULL,
    risk_level VARCHAR(32),
    effect_type VARCHAR(32) NOT NULL,
    score_delta INT NOT NULL DEFAULT 0,
    reason VARCHAR(512),
    start_time DATETIME,
    end_time DATETIME,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_list_object (list_type, object_type, object_value),
    INDEX idx_list_lookup (object_type, object_value, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_no VARCHAR(64) NOT NULL UNIQUE,
    request_no VARCHAR(64) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    user_id VARCHAR(64),
    device_id VARCHAR(128),
    ip VARCHAR(64),
    amount DECIMAL(18, 2),
    biz_id VARCHAR(128),
    scene VARCHAR(32),
    event_time DATETIME NOT NULL,
    raw_payload JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_event_request (request_no),
    INDEX idx_event_user_time (user_id, event_time),
    INDEX idx_event_device_time (device_id, event_time),
    INDEX idx_event_type_time (event_type, event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_decision_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    decision_no VARCHAR(64) NOT NULL UNIQUE,
    event_no VARCHAR(64) NOT NULL,
    request_no VARCHAR(64) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    user_id VARCHAR(64),
    strategy_id BIGINT,
    strategy_version INT,
    risk_score INT NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    decision VARCHAR(32) NOT NULL,
    reason TEXT,
    cost_ms INT NOT NULL DEFAULT 0,
    trace_id VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_decision_event (event_no),
    INDEX idx_decision_user_time (user_id, created_at),
    INDEX idx_decision_result_time (decision, created_at),
    INDEX idx_decision_request (request_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_decision_hit_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    decision_no VARCHAR(64) NOT NULL,
    rule_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    rule_version INT NOT NULL,
    score_delta INT NOT NULL DEFAULT 0,
    action VARCHAR(32) NOT NULL,
    hit_detail TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_hit_decision (decision_no),
    INDEX idx_hit_rule_time (rule_code, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_no VARCHAR(64) NOT NULL UNIQUE,
    decision_no VARCHAR(64) NOT NULL,
    event_no VARCHAR(64) NOT NULL,
    user_id VARCHAR(64),
    risk_score INT NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    assignee_id BIGINT,
    ai_summary TEXT,
    audit_result VARCHAR(32),
    audit_opinion TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_case_status_time (status, created_at),
    INDEX idx_case_user_time (user_id, created_at),
    INDEX idx_case_decision (decision_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_case_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_no VARCHAR(64) NOT NULL,
    operator_id BIGINT,
    operation VARCHAR(64) NOT NULL,
    before_status VARCHAR(32),
    after_status VARCHAR(32),
    remark TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_case_operation (case_no, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_user_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    register_time DATETIME,
    account_level VARCHAR(32),
    real_name_verified TINYINT NOT NULL DEFAULT 0,
    total_pay_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    pay_count_24h INT NOT NULL DEFAULT 0,
    failed_login_count_10m INT NOT NULL DEFAULT 0,
    last_login_ip VARCHAR(64),
    last_login_time DATETIME,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_profile_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_device_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id VARCHAR(128) NOT NULL UNIQUE,
    account_count_24h INT NOT NULL DEFAULT 0,
    account_count_total INT NOT NULL DEFAULT 0,
    payment_count_24h INT NOT NULL DEFAULT 0,
    first_seen_time DATETIME,
    last_seen_time DATETIME,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_device_profile_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_ip_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ip VARCHAR(64) NOT NULL UNIQUE,
    login_count_10m INT NOT NULL DEFAULT 0,
    failed_login_count_10m INT NOT NULL DEFAULT 0,
    account_count_24h INT NOT NULL DEFAULT 0,
    payment_count_24h INT NOT NULL DEFAULT 0,
    location VARCHAR(128),
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ip_profile_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
