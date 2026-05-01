USE riskguard;

INSERT INTO sys_user (username, password_hash, real_name, role_code, status)
VALUES ('admin', '{noop}RiskGuard@123456', 'Administrator', 'ADMIN', 'ENABLED')
ON DUPLICATE KEY UPDATE username = username;

INSERT INTO risk_rule (rule_code, rule_name, event_type, expression_text, score, action, priority, status, version, description)
VALUES
('IP_BLACKLIST_HIT', 'IP 命中黑名单', 'LOGIN', 'list.ipBlacklistHit == true', 100, 'REJECT', 10, 'ENABLED', 1, 'IP 命中黑名单时直接拒绝'),
('USER_BLACKLIST_HIT', '用户命中黑名单', 'LOGIN', 'list.userBlacklistHit == true', 100, 'REJECT', 10, 'ENABLED', 1, '用户命中黑名单时直接拒绝'),
('LOGIN_FAIL_5_TIMES', '短时间登录失败过多', 'LOGIN', 'user.failedLoginCount10m >= 5', 25, 'SCORE', 100, 'ENABLED', 1, '10 分钟内登录失败超过 5 次'),
('IP_MULTI_ACCOUNT_LOGIN', 'IP 关联账号过多', 'LOGIN', 'ip.accountCount24h >= 10', 30, 'SCORE', 110, 'ENABLED', 1, '24 小时内同一 IP 关联账号过多'),
('NEW_USER_HIGH_AMOUNT', '新用户大额支付', 'PAYMENT', 'user.registerDays <= 7 && event.amount >= 1000', 30, 'SCORE', 100, 'ENABLED', 1, '注册 7 天内用户支付金额超过 1000 元'),
('DEVICE_MULTI_ACCOUNT', '设备关联账号过多', 'PAYMENT', 'device.accountCount24h >= 5', 40, 'SCORE', 110, 'ENABLED', 1, '同一设备 24 小时关联账号超过阈值'),
('UNVERIFIED_HIGH_AMOUNT', '未实名大额支付', 'PAYMENT', 'user.realNameVerified == false && event.amount >= 500', 35, 'SCORE', 120, 'ENABLED', 1, '未实名用户进行较高金额支付'),
('PAYMENT_FREQUENCY_HIGH', '支付频率过高', 'PAYMENT', 'user.payCount24h >= 10', 25, 'SCORE', 130, 'ENABLED', 1, '24 小时内支付次数过多')
ON DUPLICATE KEY UPDATE rule_code = rule_code;

INSERT INTO risk_strategy (strategy_code, strategy_name, event_type, status, version, gray_ratio, description)
VALUES
('LOGIN_BASIC_STRATEGY', '登录基础风控策略', 'LOGIN', 'ENABLED', 1, 100, 'MVP 登录场景基础策略'),
('PAYMENT_BASIC_STRATEGY', '支付基础风控策略', 'PAYMENT', 'ENABLED', 1, 100, 'MVP 支付场景基础策略')
ON DUPLICATE KEY UPDATE strategy_code = strategy_code;

INSERT INTO risk_user_profile (user_id, register_time, account_level, real_name_verified, total_pay_amount, pay_count_24h, failed_login_count_10m, last_login_ip, last_login_time)
VALUES
('U10001', DATE_SUB(NOW(), INTERVAL 3 DAY), 'NORMAL', 0, 200.00, 4, 0, '192.168.1.10', NOW()),
('U90001', DATE_SUB(NOW(), INTERVAL 30 DAY), 'NORMAL', 1, 12000.00, 1, 6, '10.0.0.9', NOW())
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO risk_device_profile (device_id, account_count_24h, account_count_total, payment_count_24h, first_seen_time, last_seen_time)
VALUES
('D90001', 6, 9, 3, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
('D10001', 1, 1, 1, DATE_SUB(NOW(), INTERVAL 30 DAY), NOW())
ON DUPLICATE KEY UPDATE device_id = device_id;

INSERT INTO risk_ip_profile (ip, login_count_10m, failed_login_count_10m, account_count_24h, payment_count_24h, location)
VALUES
('192.168.1.10', 3, 1, 8, 2, 'local'),
('10.0.0.9', 8, 6, 12, 0, 'local')
ON DUPLICATE KEY UPDATE ip = ip;

INSERT INTO risk_list (list_type, object_type, object_value, risk_level, effect_type, score_delta, reason, start_time, status)
VALUES
('BLACK', 'IP', '10.0.0.9', 'CRITICAL', 'REJECT', 100, 'MVP 示例黑名单 IP', NOW(), 'ENABLED'),
('BLACK', 'USER', 'U90001', 'CRITICAL', 'REJECT', 100, 'MVP 示例黑名单用户', NOW(), 'ENABLED')
ON DUPLICATE KEY UPDATE object_value = object_value;

INSERT INTO risk_rule_version (rule_id, version, expression_text, score, action, priority, status, publish_note)
SELECT id, 1, expression_text, score, action, priority, 'ENABLED', 'MVP initial published rule'
FROM risk_rule
WHERE version = 1
ON DUPLICATE KEY UPDATE
    expression_text = VALUES(expression_text),
    score = VALUES(score),
    action = VALUES(action),
    priority = VALUES(priority),
    status = VALUES(status);

INSERT INTO risk_strategy_rule (strategy_id, rule_id, rule_version, execute_order, enabled)
SELECT s.id, r.id, 1, r.priority, 1
FROM risk_strategy s
JOIN risk_rule r ON r.event_type = s.event_type
WHERE s.strategy_code = 'LOGIN_BASIC_STRATEGY'
  AND r.rule_code IN ('IP_BLACKLIST_HIT', 'USER_BLACKLIST_HIT', 'LOGIN_FAIL_5_TIMES', 'IP_MULTI_ACCOUNT_LOGIN')
ON DUPLICATE KEY UPDATE
    rule_version = VALUES(rule_version),
    execute_order = VALUES(execute_order),
    enabled = VALUES(enabled);

INSERT INTO risk_strategy_rule (strategy_id, rule_id, rule_version, execute_order, enabled)
SELECT s.id, r.id, 1, r.priority, 1
FROM risk_strategy s
JOIN risk_rule r ON r.event_type = s.event_type
WHERE s.strategy_code = 'PAYMENT_BASIC_STRATEGY'
  AND r.rule_code IN ('NEW_USER_HIGH_AMOUNT', 'DEVICE_MULTI_ACCOUNT', 'UNVERIFIED_HIGH_AMOUNT', 'PAYMENT_FREQUENCY_HIGH')
ON DUPLICATE KEY UPDATE
    rule_version = VALUES(rule_version),
    execute_order = VALUES(execute_order),
    enabled = VALUES(enabled);

INSERT INTO risk_strategy_version (strategy_id, version, rule_snapshot, gray_ratio, status, publish_note)
SELECT
    s.id,
    1,
    JSON_ARRAYAGG(JSON_OBJECT(
        'ruleId', r.id,
        'ruleCode', r.rule_code,
        'ruleName', r.rule_name,
        'eventType', r.event_type,
        'ruleVersion', sr.rule_version,
        'expression', rv.expression_text,
        'score', rv.score,
        'action', rv.action,
        'priority', rv.priority,
        'executeOrder', sr.execute_order
    )),
    s.gray_ratio,
    'ENABLED',
    'MVP initial published strategy'
FROM risk_strategy s
JOIN risk_strategy_rule sr ON sr.strategy_id = s.id AND sr.enabled = 1
JOIN risk_rule r ON r.id = sr.rule_id
JOIN risk_rule_version rv ON rv.rule_id = sr.rule_id AND rv.version = sr.rule_version
WHERE s.strategy_code IN ('LOGIN_BASIC_STRATEGY', 'PAYMENT_BASIC_STRATEGY')
GROUP BY s.id, s.gray_ratio
ON DUPLICATE KEY UPDATE
    rule_snapshot = VALUES(rule_snapshot),
    gray_ratio = VALUES(gray_ratio),
    status = VALUES(status);
