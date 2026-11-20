-- 校园快递点导航与空闲度查询平台
-- 数据库契约：9 张表，MySQL 8.4.4
-- 坐标统一使用 BD-09；尚未采集真实坐标时保持 NULL，禁止写入占位坐标。

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS campus_express
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE campus_express;

CREATE TABLE IF NOT EXISTS station (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '物理站点名称',
    address VARCHAR(255) NOT NULL COMMENT '详细地址',
    lng DECIMAL(10, 7) NULL COMMENT '百度地图经度（BD-09）',
    lat DECIMAL(10, 7) NULL COMMENT '百度地图纬度（BD-09）',
    business_hours VARCHAR(100) NOT NULL DEFAULT '以站点公告为准' COMMENT '营业时间',
    contact_phone VARCHAR(20) NULL COMMENT '联系电话',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0停用，1正常',
    crowd_level TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '拥挤档位：0空闲，1一般，2拥挤',
    crowd_score DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT '拥挤指数：0-100',
    crowd_updated_at DATETIME NULL COMMENT '拥挤度最近计算时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_station_name (name),
    KEY idx_station_status (status),
    CONSTRAINT chk_station_status CHECK (status IN (0, 1)),
    CONSTRAINT chk_station_crowd_level CHECK (crowd_level IN (0, 1, 2)),
    CONSTRAINT chk_station_crowd_score CHECK (crowd_score BETWEEN 0.00 AND 100.00),
    CONSTRAINT chk_station_lng CHECK (lng IS NULL OR lng BETWEEN -180.0000000 AND 180.0000000),
    CONSTRAINT chk_station_lat CHECK (lat IS NULL OR lat BETWEEN -90.0000000 AND 90.0000000)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '物理快递站点';

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    password VARCHAR(100) NOT NULL COMMENT '密码哈希',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    role TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '角色：0学生，1站长，2超级管理员',
    station_id BIGINT UNSIGNED NULL COMMENT '站长所属站点',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0待审，1正常，2禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_phone (phone),
    KEY idx_sys_user_station_role (station_id, role, status),
    KEY idx_sys_user_deleted (deleted),
    CONSTRAINT fk_sys_user_station
        FOREIGN KEY (station_id) REFERENCES station (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_sys_user_role CHECK (role IN (0, 1, 2)),
    CONSTRAINT chk_sys_user_status CHECK (status IN (0, 1, 2)),
    CONSTRAINT chk_sys_user_deleted CHECK (deleted IN (0, 1)),
    CONSTRAINT chk_sys_user_station_admin CHECK (role <> 1 OR station_id IS NOT NULL)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '系统用户';

CREATE TABLE IF NOT EXISTS station_company (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    station_id BIGINT UNSIGNED NOT NULL COMMENT '站点ID',
    company_code VARCHAR(32) NOT NULL COMMENT '快递公司编码',
    company_name VARCHAR(64) NOT NULL COMMENT '快递公司名称',
    PRIMARY KEY (id),
    UNIQUE KEY uk_station_company (station_id, company_code),
    KEY idx_station_company_code (company_code),
    CONSTRAINT fk_station_company_station
        FOREIGN KEY (station_id) REFERENCES station (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '站点代理快递公司';

CREATE TABLE IF NOT EXISTS express_package (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    pickup_code VARCHAR(16) NOT NULL COMMENT '取件码，6-8 位数字或字母',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收件人手机号',
    station_id BIGINT UNSIGNED NOT NULL COMMENT '所在站点ID',
    company_code VARCHAR(32) NOT NULL COMMENT '快递公司编码',
    shelf_no VARCHAR(32) NOT NULL COMMENT '货架号',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0待取，1已取，2已撤销',
    operator_id BIGINT UNSIGNED NOT NULL COMMENT '上架操作人',
    inbound_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
    taken_at DATETIME NULL COMMENT '取件时间',
    remark VARCHAR(255) NULL COMMENT '备注',
    PRIMARY KEY (id),
    UNIQUE KEY uk_express_package_pickup_code (pickup_code),
    KEY idx_express_package_phone_status (receiver_phone, status),
    KEY idx_express_package_station_status (station_id, status),
    KEY idx_express_package_status_inbound (status, inbound_at),
    KEY idx_express_package_operator (operator_id),
    CONSTRAINT fk_express_package_station
        FOREIGN KEY (station_id) REFERENCES station (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_express_package_station_company
        FOREIGN KEY (station_id, company_code)
        REFERENCES station_company (station_id, company_code)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_express_package_operator
        FOREIGN KEY (operator_id) REFERENCES sys_user (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_express_package_status CHECK (status IN (0, 1, 2)),
    CONSTRAINT chk_express_package_pickup_code CHECK (pickup_code REGEXP '^[0-9A-Za-z]{6,8}$'),
    CONSTRAINT chk_express_package_taken_at CHECK (status <> 1 OR taken_at IS NOT NULL),
    CONSTRAINT chk_express_package_taken_order CHECK (taken_at IS NULL OR taken_at >= inbound_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '快递包裹';

CREATE TABLE IF NOT EXISTS package_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    package_id BIGINT UNSIGNED NOT NULL COMMENT '包裹ID',
    action TINYINT UNSIGNED NOT NULL COMMENT '动作：0上架，1撤销，2核销',
    operator_id BIGINT UNSIGNED NOT NULL COMMENT '操作人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_package_log_package_created (package_id, created_at),
    KEY idx_package_log_operator_created (operator_id, created_at),
    CONSTRAINT fk_package_log_package
        FOREIGN KEY (package_id) REFERENCES express_package (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_package_log_operator
        FOREIGN KEY (operator_id) REFERENCES sys_user (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_package_log_action CHECK (action IN (0, 1, 2))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '包裹状态流水';

CREATE TABLE IF NOT EXISTS idle_report (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    station_id BIGINT UNSIGNED NOT NULL COMMENT '站点ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '上报用户',
    level TINYINT UNSIGNED NOT NULL COMMENT '拥挤档位：0空闲，1一般，2拥挤',
    source TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '来源：0学生，1站长校准',
    ip VARCHAR(45) NULL COMMENT '上报IP，兼容 IPv6',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
    PRIMARY KEY (id),
    KEY idx_idle_report_station_created (station_id, created_at),
    KEY idx_idle_report_user_station_created (user_id, station_id, created_at),
    CONSTRAINT fk_idle_report_station
        FOREIGN KEY (station_id) REFERENCES station (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_idle_report_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_idle_report_level CHECK (level IN (0, 1, 2)),
    CONSTRAINT chk_idle_report_source CHECK (source IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '空闲度上报明细';

CREATE TABLE IF NOT EXISTS station_audit (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '申请用户',
    station_id BIGINT UNSIGNED NOT NULL COMMENT '申请入驻站点',
    apply_reason VARCHAR(500) NOT NULL COMMENT '申请理由',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0待审，1通过，2驳回',
    reviewer_id BIGINT UNSIGNED NULL COMMENT '审核人',
    review_time DATETIME NULL COMMENT '审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    PRIMARY KEY (id),
    KEY idx_station_audit_status_created (status, created_at),
    KEY idx_station_audit_user (user_id, created_at),
    KEY idx_station_audit_station (station_id, status),
    CONSTRAINT fk_station_audit_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_station_audit_station
        FOREIGN KEY (station_id) REFERENCES station (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_station_audit_reviewer
        FOREIGN KEY (reviewer_id) REFERENCES sys_user (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_station_audit_status CHECK (status IN (0, 1, 2))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '站长入驻审核';

CREATE TABLE IF NOT EXISTS alert_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    type TINYINT UNSIGNED NOT NULL COMMENT '类型：0超时，1异常上报，2站点停用',
    station_id BIGINT UNSIGNED NOT NULL COMMENT '站点ID',
    package_id BIGINT UNSIGNED NULL COMMENT '关联包裹ID',
    content VARCHAR(500) NOT NULL COMMENT '告警内容',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0未处理，1已处理',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    handled_at DATETIME NULL COMMENT '处理时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_alert_type_package (type, package_id),
    KEY idx_alert_station_status_created (station_id, status, created_at),
    CONSTRAINT fk_alert_station
        FOREIGN KEY (station_id) REFERENCES station (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_alert_package
        FOREIGN KEY (package_id) REFERENCES express_package (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_alert_type CHECK (type IN (0, 1, 2)),
    CONSTRAINT chk_alert_status CHECK (status IN (0, 1)),
    CONSTRAINT chk_alert_handled_at CHECK (status <> 1 OR handled_at IS NOT NULL)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '告警记录';

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT UNSIGNED NULL COMMENT '操作用户，匿名操作可为空',
    module VARCHAR(50) NOT NULL COMMENT '业务模块',
    action VARCHAR(100) NOT NULL COMMENT '操作动作',
    detail TEXT NULL COMMENT '操作详情',
    ip VARCHAR(45) NULL COMMENT '操作IP，兼容 IPv6',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_operation_log_user_created (user_id, created_at),
    KEY idx_operation_log_module_created (module, created_at),
    CONSTRAINT fk_operation_log_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '后台操作审计';
