-- 2026-11-19 实地调研站点数据。
-- 坐标由百度地图 POI 墨卡托坐标转换为 BD-09，可重复执行。

SET NAMES utf8mb4;
USE campus_express;

START TRANSACTION;

SET @s1 = COALESCE(
    (SELECT id FROM station WHERE name = '顺丰速运（电子科技大学中山学院店）' LIMIT 1),
    (SELECT id FROM station WHERE name = '校内·邮政服务点' LIMIT 1)
);
SET @s2 = COALESCE(
    (SELECT id FROM station WHERE name = '拼多多驿站京东快递' LIMIT 1),
    (SELECT id FROM station WHERE name = '校内·申通/韵达代收点' LIMIT 1)
);
SET @s3 = COALESCE(
    (SELECT id FROM station WHERE name = '宜小集(南门)原菜鸟驿站(电子科技大学中山学院体育馆)' LIMIT 1),
    (SELECT id FROM station WHERE name = '校内·顺丰营业点' LIMIT 1)
);
SET @s4 = COALESCE(
    (SELECT id FROM station WHERE name = '菜鸟驿站(中山石岐莲兴阁店)' LIMIT 1),
    (SELECT id FROM station WHERE name = '校外北门·菜鸟驿站' LIMIT 1)
);
SET @s5 = COALESCE(
    (SELECT id FROM station WHERE name = '多多驿站(莲兴阁店)' LIMIT 1),
    (SELECT id FROM station WHERE name = '校外岐头门·PDD代收点' LIMIT 1)
);

UPDATE station
SET name = '顺丰速运（电子科技大学中山学院店）',
    address = '广东省中山市石岐街道电子科技大学中山学院14栋1楼',
    lng = 113.4002304,
    lat = 22.5346286,
    business_hours = '08:00-22:30',
    contact_phone = NULL
WHERE id = @s1;

UPDATE station
SET name = '拼多多驿站京东快递',
    address = '广东省中山市石岐街道电子科技大学中山学院11栋1楼',
    lng = 113.3986990,
    lat = 22.5354901,
    business_hours = '09:00-21:00',
    contact_phone = NULL
WHERE id = @s2;

UPDATE station
SET name = '宜小集(南门)原菜鸟驿站(电子科技大学中山学院体育馆)',
    address = '广东省中山市石岐区学院路1号电子科技大学中山学院体育馆楼下',
    lng = 113.3964006,
    lat = 22.5336495,
    business_hours = '08:00-20:00',
    contact_phone = NULL
WHERE id = @s3;

UPDATE station
SET name = '菜鸟驿站(中山石岐莲兴阁店)',
    address = '广东省中山市龙腾路6号城市花园莲兴阁24卡',
    lng = 113.3952711,
    lat = 22.5365572,
    business_hours = '以站点公告为准',
    contact_phone = NULL
WHERE id = @s4;

UPDATE station
SET name = '多多驿站(莲兴阁店)',
    address = '广东省中山市石岐街道莲塘东路2号',
    lng = 113.3956009,
    lat = 22.5364375,
    business_hours = '以站点公告为准',
    contact_phone = NULL
WHERE id = @s5;

DELETE FROM station_company
WHERE station_id IN (@s1, @s2, @s3, @s4, @s5);

INSERT INTO station_company (station_id, company_code, company_name)
VALUES
    (@s1, 'SF', '顺丰速运'),

    (@s2, 'STO', '申通快递'),
    (@s2, 'YUNDA', '韵达快递'),
    (@s2, 'YTO', '圆通速递'),
    (@s2, 'ZTO', '中通快递'),
    (@s2, 'JD', '京东物流'),
    (@s2, 'JTSD', '极兔速递'),

    (@s3, 'POST', '中国邮政'),
    (@s3, 'YTO', '圆通速递'),
    (@s3, 'ZTO', '中通快递'),
    (@s3, 'STO', '申通快递'),
    (@s3, 'YUNDA', '韵达快递'),
    (@s3, 'JD', '京东物流'),

    (@s4, 'YTO', '圆通速递'),
    (@s4, 'POST', '中国邮政'),
    (@s4, 'SF', '顺丰速运'),
    (@s4, 'JD', '京东物流'),

    (@s5, 'ZTO', '中通快递'),
    (@s5, 'JTSD', '极兔速递'),
    (@s5, 'STO', '申通快递'),
    (@s5, 'YUNDA', '韵达快递'),
    (@s5, 'SF', '顺丰速运');

COMMIT;
