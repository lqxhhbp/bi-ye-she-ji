-- 开发环境基础数据，可重复执行。
-- 演示账号密码均为 123456，密码以 BCrypt 哈希保存，禁止在正式环境继续使用。

SET NAMES utf8mb4;
USE campus_express;

INSERT INTO station
    (name, address, lng, lat, business_hours, contact_phone, status, crowd_level, crowd_score)
VALUES
    ('顺丰速运（电子科技大学中山学院店）', '广东省中山市石岐街道电子科技大学中山学院14栋1楼', 113.4002304, 22.5346286, '08:00-22:30', NULL, 1, 0, 0.00),
    ('拼多多驿站京东快递', '广东省中山市石岐街道电子科技大学中山学院11栋1楼', 113.3986990, 22.5354901, '09:00-21:00', NULL, 1, 0, 0.00),
    ('宜小集(南门)原菜鸟驿站(电子科技大学中山学院体育馆)', '广东省中山市石岐区学院路1号电子科技大学中山学院体育馆楼下', 113.3964006, 22.5336495, '08:00-20:00', NULL, 1, 0, 0.00),
    ('菜鸟驿站(中山石岐莲兴阁店)', '广东省中山市龙腾路6号城市花园莲兴阁24卡', 113.3952711, 22.5365572, '以站点公告为准', NULL, 1, 0, 0.00),
    ('多多驿站(莲兴阁店)', '广东省中山市石岐街道莲塘东路2号', 113.3956009, 22.5364375, '以站点公告为准', NULL, 1, 0, 0.00)
AS new
ON DUPLICATE KEY UPDATE
    address = new.address,
    lng = new.lng,
    lat = new.lat,
    business_hours = new.business_hours,
    contact_phone = new.contact_phone,
    status = new.status;

INSERT INTO station_company (station_id, company_code, company_name)
SELECT id, 'POST', '中国邮政'
FROM station
WHERE name = '宜小集(南门)原菜鸟驿站(电子科技大学中山学院体育馆)'
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);

INSERT INTO station_company (station_id, company_code, company_name)
SELECT id, 'YTO', '圆通速递'
FROM station
WHERE name = '宜小集(南门)原菜鸟驿站(电子科技大学中山学院体育馆)'
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);

INSERT INTO station_company (station_id, company_code, company_name)
SELECT id, 'ZTO', '中通快递'
FROM station
WHERE name = '宜小集(南门)原菜鸟驿站(电子科技大学中山学院体育馆)'
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);

INSERT INTO station_company (station_id, company_code, company_name)
SELECT id, 'STO', '申通快递'
FROM station
WHERE name = '宜小集(南门)原菜鸟驿站(电子科技大学中山学院体育馆)'
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);

INSERT INTO station_company (station_id, company_code, company_name)
SELECT id, 'YUNDA', '韵达快递'
FROM station
WHERE name = '宜小集(南门)原菜鸟驿站(电子科技大学中山学院体育馆)'
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);

INSERT INTO station_company (station_id, company_code, company_name)
SELECT id, 'JD', '京东物流'
FROM station
WHERE name = '宜小集(南门)原菜鸟驿站(电子科技大学中山学院体育馆)'
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);

INSERT INTO station_company (station_id, company_code, company_name)
SELECT id, 'SF', '顺丰速运'
FROM station
WHERE name = '顺丰速运（电子科技大学中山学院店）'
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);

INSERT INTO station_company (station_id, company_code, company_name)
SELECT id, company_code, company_name
FROM station
JOIN (
    SELECT '拼多多驿站京东快递' AS station_name, 'STO' AS company_code, '申通快递' AS company_name
    UNION ALL SELECT '拼多多驿站京东快递', 'YUNDA', '韵达快递'
    UNION ALL SELECT '拼多多驿站京东快递', 'YTO', '圆通速递'
    UNION ALL SELECT '拼多多驿站京东快递', 'ZTO', '中通快递'
    UNION ALL SELECT '拼多多驿站京东快递', 'JD', '京东物流'
    UNION ALL SELECT '拼多多驿站京东快递', 'JTSD', '极兔速递'
) AS companies ON companies.station_name = station.name
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);

INSERT INTO station_company (station_id, company_code, company_name)
SELECT id, company_code, company_name
FROM station
JOIN (
    SELECT '菜鸟驿站(中山石岐莲兴阁店)' AS station_name, 'YTO' AS company_code, '圆通速递' AS company_name
    UNION ALL SELECT '菜鸟驿站(中山石岐莲兴阁店)', 'POST', '中国邮政'
    UNION ALL SELECT '菜鸟驿站(中山石岐莲兴阁店)', 'SF', '顺丰速运'
    UNION ALL SELECT '菜鸟驿站(中山石岐莲兴阁店)', 'JD', '京东物流'
) AS companies ON companies.station_name = station.name
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);

INSERT INTO station_company (station_id, company_code, company_name)
SELECT id, company_code, company_name
FROM station
JOIN (
    SELECT '多多驿站(莲兴阁店)' AS station_name, 'ZTO' AS company_code, '中通快递' AS company_name
    UNION ALL SELECT '多多驿站(莲兴阁店)', 'JTSD', '极兔速递'
    UNION ALL SELECT '多多驿站(莲兴阁店)', 'STO', '申通快递'
    UNION ALL SELECT '多多驿站(莲兴阁店)', 'YUNDA', '韵达快递'
    UNION ALL SELECT '多多驿站(莲兴阁店)', 'SF', '顺丰速运'
) AS companies ON companies.station_name = station.name
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);

INSERT INTO sys_user
    (phone, password, real_name, role, station_id, status)
VALUES
    ('13800000000', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '超级管理员', 2, NULL, 1)
AS new
ON DUPLICATE KEY UPDATE
    password = new.password,
    real_name = new.real_name,
    role = new.role,
    station_id = new.station_id,
    status = new.status,
    deleted = 0;

INSERT INTO sys_user
    (phone, password, real_name, role, station_id, status)
SELECT admin_data.phone,
       '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a',
       admin_data.real_name,
       1,
       station.id,
       1
FROM (
    SELECT '13800001001' AS phone, '顺丰站点管理员' AS real_name, '顺丰速运（电子科技大学中山学院店）' AS station_name
    UNION ALL SELECT '13800001002', '拼多多京东站点管理员', '拼多多驿站京东快递'
    UNION ALL SELECT '13800001003', '南门宜小集站点管理员', '宜小集(南门)原菜鸟驿站(电子科技大学中山学院体育馆)'
    UNION ALL SELECT '13800001004', '莲兴阁菜鸟站点管理员', '菜鸟驿站(中山石岐莲兴阁店)'
    UNION ALL SELECT '13800001005', '莲兴阁多多站点管理员', '多多驿站(莲兴阁店)'
) AS admin_data
JOIN station ON station.name = admin_data.station_name
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    real_name = VALUES(real_name),
    role = VALUES(role),
    station_id = VALUES(station_id),
    status = VALUES(status),
    deleted = 0;

INSERT INTO sys_user
    (phone, password, real_name, role, station_id, status)
VALUES
    ('13800002001', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生01', 0, NULL, 1),
    ('13800002002', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生02', 0, NULL, 1),
    ('13800002003', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生03', 0, NULL, 1),
    ('13800002004', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生04', 0, NULL, 1),
    ('13800002005', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生05', 0, NULL, 1),
    ('13800002006', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生06', 0, NULL, 1),
    ('13800002007', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生07', 0, NULL, 1),
    ('13800002008', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生08', 0, NULL, 1),
    ('13800002009', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生09', 0, NULL, 1),
    ('13800002010', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生10', 0, NULL, 1),
    ('13800002011', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生11', 0, NULL, 1),
    ('13800002012', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生12', 0, NULL, 1),
    ('13800002013', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生13', 0, NULL, 1),
    ('13800002014', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生14', 0, NULL, 1),
    ('13800002015', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生15', 0, NULL, 1),
    ('13800002016', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生16', 0, NULL, 1),
    ('13800002017', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生17', 0, NULL, 1),
    ('13800002018', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生18', 0, NULL, 1),
    ('13800002019', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生19', 0, NULL, 1),
    ('13800002020', '$2a$10$vy871MgfDhEseTYdhMPimOfsngCu3g2dpBDup7QeyWegLnS/U5r2a', '测试学生20', 0, NULL, 1)
AS new
ON DUPLICATE KEY UPDATE
    password = new.password,
    real_name = new.real_name,
    role = new.role,
    station_id = new.station_id,
    status = new.status,
    deleted = 0;
