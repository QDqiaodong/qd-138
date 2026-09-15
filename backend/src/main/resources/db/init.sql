SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS script_theme (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    theme_name VARCHAR(100) NOT NULL COMMENT '剧本主题名称',
    description TEXT COMMENT '主题描述',
    era VARCHAR(50) COMMENT '适配时代',
    difficulty VARCHAR(20) DEFAULT '中等' COMMENT '难度等级',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_theme_name (theme_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='剧本主题表';

CREATE TABLE IF NOT EXISTS character_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    script_theme_id BIGINT NOT NULL COMMENT '所属剧本主题ID',
    gender VARCHAR(10) COMMENT '性别',
    age INT COMMENT '年龄',
    description TEXT COMMENT '角色描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_script (role_name, script_theme_id),
    FOREIGN KEY (script_theme_id) REFERENCES script_theme(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人物角色表';

CREATE TABLE IF NOT EXISTS prop (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prop_code VARCHAR(50) NOT NULL COMMENT '道具编号',
    prop_name VARCHAR(100) NOT NULL COMMENT '道具名称',
    era VARCHAR(50) COMMENT '适配时代',
    prop_type VARCHAR(50) COMMENT '道具类型',
    description TEXT COMMENT '道具描述',
    status VARCHAR(20) DEFAULT '正常' COMMENT '状态：正常/损坏/丢失',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_prop_code (prop_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='道具基础档案表';

CREATE TABLE IF NOT EXISTS role_prop (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_role_id BIGINT NOT NULL COMMENT '角色ID',
    prop_id BIGINT NOT NULL COMMENT '道具ID',
    script_theme_id BIGINT NOT NULL COMMENT '剧本主题ID',
    bind_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_prop (character_role_id, prop_id),
    FOREIGN KEY (character_role_id) REFERENCES character_role(id) ON DELETE CASCADE,
    FOREIGN KEY (prop_id) REFERENCES prop(id) ON DELETE CASCADE,
    FOREIGN KEY (script_theme_id) REFERENCES script_theme(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色道具关联表';

CREATE TABLE IF NOT EXISTS prop_change_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prop_id BIGINT NOT NULL COMMENT '道具ID',
    character_role_id BIGINT COMMENT '角色ID',
    script_theme_id BIGINT COMMENT '剧本主题ID',
    change_type VARCHAR(20) NOT NULL COMMENT '变更类型：绑定/解绑/更换',
    before_value TEXT COMMENT '变更前值',
    after_value TEXT COMMENT '变更后值',
    change_reason VARCHAR(500) COMMENT '变更原因',
    operator VARCHAR(100) COMMENT '操作人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (prop_id) REFERENCES prop(id) ON DELETE CASCADE,
    FOREIGN KEY (character_role_id) REFERENCES character_role(id) ON DELETE SET NULL,
    FOREIGN KEY (script_theme_id) REFERENCES script_theme(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='道具变更记录表';

CREATE TABLE IF NOT EXISTS prop_damage_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_no VARCHAR(30) COMMENT '报损单号',
    prop_id BIGINT NOT NULL COMMENT '道具ID',
    damaged_part VARCHAR(200) NOT NULL COMMENT '损坏部位（开单录入原文）',
    discoverer VARCHAR(100) NOT NULL COMMENT '发现人（开单录入原文）',
    status VARCHAR(20) DEFAULT '未结案' COMMENT '状态：未结案/已结案',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开单时间',
    closed_at DATETIME NULL COMMENT '结案时间',
    -- 未结案时取道具ID、结案后为NULL的生成列，配合唯一键保证同一道具至多一张未结案报损单
    open_prop_id BIGINT GENERATED ALWAYS AS (IF(status = '未结案', prop_id, NULL)) STORED,
    UNIQUE KEY uk_report_no (report_no),
    UNIQUE KEY uk_open_prop_id (open_prop_id)
    -- MySQL 8.0.46：同表 STORED 生成列时不能再加指向 prop 的外键（ERROR 1215），应用层仍按 prop_id 关联
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='道具报损单表';

CREATE TABLE IF NOT EXISTS performer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    performer_name VARCHAR(100) NOT NULL COMMENT '演职人员姓名',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_performer_name (performer_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演职人员名册表';

CREATE TABLE IF NOT EXISTS show_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_no VARCHAR(30) COMMENT '场次编号',
    script_theme_id BIGINT NOT NULL COMMENT '演出剧本主题ID',
    start_time DATETIME NOT NULL COMMENT '开演时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    status VARCHAR(20) DEFAULT '排班中' COMMENT '状态：排班中/已排好',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (script_theme_id) REFERENCES script_theme(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演出场次表';

CREATE TABLE IF NOT EXISTS session_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL COMMENT '场次ID',
    character_role_id BIGINT NOT NULL COMMENT '人物角色ID',
    performer_id BIGINT NOT NULL COMMENT '演职人员ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 一个人物在一场里只排一名演员；同一个人在同一场里不能演两个人物
    UNIQUE KEY uk_session_role (session_id, character_role_id),
    UNIQUE KEY uk_session_performer (session_id, performer_id),
    FOREIGN KEY (session_id) REFERENCES show_session(id) ON DELETE CASCADE,
    FOREIGN KEY (character_role_id) REFERENCES character_role(id) ON DELETE CASCADE,
    FOREIGN KEY (performer_id) REFERENCES performer(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场次排班表';

INSERT INTO script_theme (theme_name, description, era, difficulty) VALUES
('民国风云', '1930年代上海滩，帮派纷争，爱恨情仇交织的悬疑故事', '民国', '中等'),
('古风仙侠', '仙侠世界，门派恩怨，寻找失落的神器', '古代', '困难'),
('未来科幻', '2077年赛博朋克都市，人工智能反叛危机', '未来', '困难'),
('校园青春', '高中校园里的神秘失踪事件，追忆逝去的青春', '现代', '简单');

INSERT INTO character_role (role_name, script_theme_id, gender, age, description) VALUES
('许文强', 1, '男', 28, '上海滩青帮大佬，重情重义'),
('冯程程', 1, '女', 22, '冯敬尧之女，温柔善良'),
('丁力', 1, '男', 25, '许文强结拜兄弟，精明能干'),
('李逍遥', 2, '男', 19, '蜀山弟子，御剑江湖'),
('赵灵儿', 2, '女', 16, '南诏国公主，身负使命'),
('林月如', 2, '女', 18, '林家堡大小姐，性格泼辣'),
('陈博士', 3, '男', 45, 'AI领域权威科学家'),
('小雨', 3, '女', 22, '黑客少女，技术超群'),
('张伟', 4, '男', 17, '高中生，班级学霸'),
('王芳', 4, '女', 16, '神秘转学生');

INSERT INTO prop (prop_code, prop_name, era, prop_type, description) VALUES
('PROP-001', '西装外套', '民国', '服装', '深蓝色羊毛西装，许文强经典造型'),
('PROP-002', '旗袍', '民国', '服装', '红色丝绸旗袍，冯程程舞会礼服'),
('PROP-003', '礼帽', '民国', '配饰', '黑色呢料礼帽'),
('PROP-004', '折扇', '民国', '道具', '雕花檀香折扇'),
('PROP-005', '长剑', '古代', '武器', '蜀山派制式长剑'),
('PROP-006', '古装长裙', '古代', '服装', '白色纱质仙女裙'),
('PROP-007', '玉佩', '古代', '配饰', '蓝田玉平安扣'),
('PROP-008', '法杖', '古代', '武器', '镶嵌宝石的法杖'),
('PROP-009', '机械义眼', '未来', '道具', '高科技仿生义眼'),
('PROP-010', '皮质风衣', '未来', '服装', '黑色皮质长款风衣'),
('PROP-011', '平板电脑', '未来', '道具', '全息投影平板'),
('PROP-012', '校服', '现代', '服装', '蓝白相间高中校服'),
('PROP-013', '书包', '现代', '道具', '双肩帆布书包'),
('PROP-014', '眼镜', '现代', '配饰', '黑框近视眼镜');

INSERT INTO role_prop (character_role_id, prop_id, script_theme_id) VALUES
(1, 1, 1),
(1, 3, 1),
(1, 4, 1),
(2, 2, 1),
(2, 4, 1),
(3, 1, 1),
(3, 3, 1),
(4, 5, 2),
(4, 7, 2),
(5, 6, 2),
(5, 7, 2),
(6, 6, 2),
(6, 8, 2),
(7, 9, 3),
(7, 11, 3),
(8, 10, 3),
(8, 9, 3),
(9, 12, 4),
(9, 13, 4),
(9, 14, 4),
(10, 12, 4),
(10, 13, 4);

INSERT INTO performer (performer_name) VALUES
('张子昂'),
('李慕白'),
('王晓彤'),
('刘一帆'),
('陈星'),
('赵梦琪');