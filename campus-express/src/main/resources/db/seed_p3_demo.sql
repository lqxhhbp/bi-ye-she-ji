-- P3 演示数据，可重复执行。
-- 仅清理带 P3 标记的数据，不覆盖 P2 测试包裹和真实站点数据。

SET NAMES utf8mb4;
USE campus_express;

DELETE a
FROM alert_record a
JOIN express_package p ON p.id = a.package_id
WHERE p.pickup_code REGEXP '^D3[0-9]{6}$';

DELETE l
FROM package_log l
JOIN express_package p ON p.id = l.package_id
WHERE p.pickup_code REGEXP '^D3[0-9]{6}$';

DELETE FROM express_package
WHERE pickup_code REGEXP '^D3[0-9]{6}$';

DELETE FROM idle_report
WHERE ip = 'P3_DEMO';

DELETE FROM station_audit
WHERE apply_reason LIKE 'P3DEMO:%';

DELETE FROM operation_log
WHERE detail LIKE 'P3DEMO:%';

DROP TEMPORARY TABLE IF EXISTS p3_seq;
CREATE TEMPORARY TABLE p3_seq (n INT PRIMARY KEY);
INSERT INTO p3_seq (n)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 400
)
SELECT n FROM seq;

INSERT INTO express_package
    (pickup_code, receiver_phone, station_id, company_code, shelf_no, status, operator_id, inbound_at, taken_at, remark)
SELECT
    CONCAT('D3', LPAD(n, 6, '0')),
    CONCAT('13800002', LPAD(MOD(n - 1, 20) + 1, 3, '0')),
    station_id,
    company_code,
    CONCAT(CHAR(65 + MOD(n, 5)), '-', LPAD(MOD(n, 50) + 1, 2, '0')),
    package_status,
    operator_id,
    inbound_at,
    taken_at,
    'P3DEMO'
FROM (
    SELECT
        n,
        MOD(n - 1, 5) + 1 AS station_no,
        CASE MOD(n - 1, 5) + 1
            WHEN 1 THEN 'SF'
            WHEN 2 THEN ELT(MOD(n - 1, 6) + 1, 'STO', 'YUNDA', 'YTO', 'ZTO', 'JD', 'JTSD')
            WHEN 3 THEN ELT(MOD(n - 1, 6) + 1, 'POST', 'YTO', 'ZTO', 'STO', 'YUNDA', 'JD')
            WHEN 4 THEN ELT(MOD(n - 1, 4) + 1, 'YTO', 'POST', 'SF', 'JD')
            ELSE ELT(MOD(n - 1, 5) + 1, 'ZTO', 'JTSD', 'STO', 'YUNDA', 'SF')
        END AS company_code,
        CASE
            WHEN n <= 280 THEN 0
            WHEN n <= 380 THEN 1
            ELSE 2
        END AS package_status,
        CASE MOD(n - 1, 5) + 1
            WHEN 1 THEN (SELECT id FROM sys_user WHERE phone = '13800001001')
            WHEN 2 THEN (SELECT id FROM sys_user WHERE phone = '13800001002')
            WHEN 3 THEN (SELECT id FROM sys_user WHERE phone = '13800001003')
            WHEN 4 THEN (SELECT id FROM sys_user WHERE phone = '13800001004')
            ELSE (SELECT id FROM sys_user WHERE phone = '13800001005')
        END AS operator_id,
        CASE
            WHEN n <= 10 THEN DATE_SUB(NOW(), INTERVAL (72 + n) HOUR)
            WHEN n <= 280 THEN DATE_SUB(NOW(), INTERVAL MOD(n, 47) HOUR)
            ELSE DATE_SUB(
                NOW(),
                INTERVAL (MOD(n, 30) * 24 + MOD(n * 7, 24)) HOUR
            )
        END AS inbound_at,
        CASE
            WHEN n > 280 AND n <= 380 THEN LEAST(
                NOW(),
                DATE_ADD(
                    DATE_SUB(
                        NOW(),
                        INTERVAL (MOD(n, 30) * 24 + MOD(n * 7, 24)) HOUR
                    ),
                    INTERVAL (1 + MOD(n, 6)) HOUR
                )
            )
            ELSE NULL
        END AS taken_at,
        id AS station_id
    FROM p3_seq
    JOIN station ON station.id = MOD(n - 1, 5) + 1
) demo_packages;

INSERT INTO package_log (package_id, action, operator_id, created_at)
SELECT id, 0, operator_id, inbound_at
FROM express_package
WHERE pickup_code REGEXP '^D3[0-9]{6}$';

INSERT INTO package_log (package_id, action, operator_id, created_at)
SELECT id, 1, operator_id, LEAST(NOW(), DATE_ADD(inbound_at, INTERVAL 10 MINUTE))
FROM express_package
WHERE pickup_code REGEXP '^D3[0-9]{6}$'
  AND status = 2;

INSERT INTO package_log (package_id, action, operator_id, created_at)
SELECT id, 2, operator_id, taken_at
FROM express_package
WHERE pickup_code REGEXP '^D3[0-9]{6}$'
  AND status = 1;

INSERT INTO idle_report (station_id, user_id, level, source, ip, created_at)
SELECT
    MOD(n - 1, 5) + 1,
    (
        SELECT id
        FROM sys_user
        WHERE phone = CONCAT('13800002', LPAD(MOD(n - 1, 20) + 1, 3, '0'))
    ),
    CASE
        WHEN hour_value IN (12, 13, 18) THEN
            CASE WHEN MOD(n, 10) < 2 THEN 2 WHEN MOD(n, 10) < 7 THEN 1 ELSE 0 END
        ELSE
            CASE WHEN MOD(n, 10) < 6 THEN 0 WHEN MOD(n, 10) < 9 THEN 1 ELSE 2 END
    END,
    0,
    'P3_DEMO',
    DATE_ADD(
        DATE_SUB(CURDATE(), INTERVAL (MOD(n, 7) + 1) DAY),
        INTERVAL (hour_value * 60 + MOD(n * 7, 60)) MINUTE
    )
FROM (
    SELECT
        n,
        CASE MOD(n, 12)
            WHEN 0 THEN 12
            WHEN 1 THEN 12
            WHEN 2 THEN 13
            WHEN 3 THEN 17
            WHEN 4 THEN 18
            WHEN 5 THEN 18
            WHEN 6 THEN 9
            WHEN 7 THEN 10
            WHEN 8 THEN 15
            WHEN 9 THEN 16
            WHEN 10 THEN 20
            ELSE 21
        END AS hour_value
    FROM p3_seq
    WHERE n <= 300
) demo_reports;

INSERT INTO idle_report (station_id, user_id, level, source, ip, created_at)
SELECT
    MOD(n - 1, 5) + 1,
    (
        SELECT id
        FROM sys_user
        WHERE phone = CONCAT('13800002', LPAD(MOD(n - 1, 20) + 1, 3, '0'))
    ),
    CASE MOD(n - 1, 5) + 1
        WHEN 1 THEN 2
        WHEN 2 THEN 1
        WHEN 3 THEN 0
        WHEN 4 THEN 1
        ELSE 2
    END,
    0,
    'P3_DEMO',
    DATE_SUB(NOW(), INTERVAL MOD(n * 3, 110) MINUTE)
FROM p3_seq
WHERE n BETWEEN 301 AND 330;

INSERT INTO station_audit (user_id, station_id, apply_reason, status)
SELECT id, 1, 'P3DEMO: 申请维护顺丰站点', 0
FROM sys_user
WHERE phone = '13800002019'
  AND NOT EXISTS (
      SELECT 1 FROM station_audit
      WHERE user_id = sys_user.id
        AND apply_reason LIKE 'P3DEMO:%'
  );

INSERT INTO station_audit (user_id, station_id, apply_reason, status)
SELECT id, 2, 'P3DEMO: 申请维护拼多多京东站点', 0
FROM sys_user
WHERE phone = '13800002020'
  AND NOT EXISTS (
      SELECT 1 FROM station_audit
      WHERE user_id = sys_user.id
        AND apply_reason LIKE 'P3DEMO:%'
  );

INSERT INTO alert_record (type, station_id, package_id, content, status)
SELECT
    0,
    p.station_id,
    p.id,
    CONCAT('包裹 ', p.pickup_code, ' 待取超过 48 小时'),
    0
FROM express_package p
WHERE p.pickup_code REGEXP '^D3[0-9]{6}$'
  AND p.status = 0
  AND p.inbound_at < DATE_SUB(NOW(), INTERVAL 48 HOUR)
ON DUPLICATE KEY UPDATE content = VALUES(content);

INSERT INTO operation_log (user_id, module, action, detail, ip)
VALUES
    (1, '系统', 'P3演示数据初始化', 'P3DEMO: seed_p3_demo.sql', '127.0.0.1'),
    (3, '包裹', '核销包裹', 'P3DEMO: POST /api/packages/verify', '127.0.0.1'),
    (3, '空闲度', '站长校准', 'P3DEMO: POST /api/idle/reports/calibrate', '127.0.0.1');
