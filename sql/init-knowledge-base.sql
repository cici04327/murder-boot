-- ============================================
-- AI知识库表建表与初始化数据
-- 执行前请确认已连接到 murder 数据库
-- ============================================

CREATE TABLE IF NOT EXISTS `ai_knowledge_base` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `category`    VARCHAR(50)  NOT NULL COMMENT '分类: reservation/refund/vip/coupon/points/group/payment/dm/store/system',
    `title`       VARCHAR(200) NOT NULL COMMENT '知识标题',
    `content`     TEXT         NOT NULL COMMENT '知识内容',
    `keywords`    VARCHAR(500)          COMMENT '关键词（逗号分隔）',
    `priority`    INT          NOT NULL DEFAULT 5 COMMENT '优先级(1-10)，越大越优先',
    `status`      INT          NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    `is_faq`      INT          NOT NULL DEFAULT 0 COMMENT '是否FAQ: 1是 0否',
    `faq_question` VARCHAR(255)         COMMENT 'FAQ展示问题',
    `hit_count`   INT          NOT NULL DEFAULT 0 COMMENT '命中次数',
    `last_hit_time` DATETIME            COMMENT '最后命中时间',
    `is_deleted`  INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_status`   (`status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI客服知识库';

-- ============================================
-- 初始化知识库数据
-- ============================================

INSERT INTO `ai_knowledge_base` (`category`, `title`, `content`, `keywords`, `priority`) VALUES

-- ===== 预约流程 =====
('reservation', '预约剧本杀流程',
'预约剧本杀完整步骤：
1. 在剧本大厅浏览剧本，选择感兴趣的剧本
2. 选择门店和场次（排期）
3. 填写参与人数（需满足剧本最低人数要求）
4. 选择可用优惠券（可选）
5. 确认总价，通过支付宝完成支付
6. 支付成功后系统自动生成核销码并推送通知
7. 到店后出示核销码，由门店员工扫码核销
8. 场次结束后员工标记完成，用户可提交评价获得积分',
'预约,怎么订,如何订,预约流程,订剧本,预约步骤', 10),

('reservation', '预约状态说明',
'预约订单状态说明：
- 待确认（1）：已提交预约，等待门店确认，请耐心等待
- 已确认（2）：门店已确认，请按时到店，记得携带核销码
- 已完成（3）：游戏结束且已核销完成
- 已取消（4）：订单已取消（用户主动取消或超时未支付自动取消）

支付状态：
- 未支付（0）：订单创建后30分钟内需完成支付，超时自动取消
- 已支付（1）：支付成功，可查看核销码
- 退款中（2）：退款申请审核中
- 已退款（3）：退款已到账',
'状态,订单状态,预约状态,待确认,已确认,已完成,已取消,未支付', 9),

('reservation', '核销码使用说明',
'核销码使用说明：
- 支付成功后，系统自动生成6位核销码
- 核销码显示在"我的预约 → 预约详情"页面中
- 到店后将核销码出示给门店员工，由员工进行扫码核销
- 核销码仅可使用一次，核销后不可撤销
- 如忘记核销码，可在预约详情页面重新查看',
'核销码,核销,到店,扫码,验证码', 9),

('reservation', '改期规则',
'改期规则（免费改期）：
- 游戏开始前24小时以上：可免费改期1次 ✅
- 游戏开始前12~24小时：需联系门店协商确认
- 游戏开始前12小时内：不支持改期 ❌

改期操作步骤：
1. 进入「我的预约」
2. 找到要改期的订单
3. 点击「申请改期」
4. 选择新的场次时间
5. 确认提交

注意：每笔订单最多可改期1次，改期不影响支付状态',
'改期,修改时间,换时间,改时间,改场次', 9),

-- ===== 退款政策 =====
('refund', '退款规则',
'退款政策（以游戏开始时间为基准）：
- 游戏开始前24小时以上：全额退款 ✅
- 游戏开始前12~24小时：退款80% ⚠️
- 游戏开始前6~12小时：退款50% ⚠️
- 游戏开始前6小时内：不支持退款 ❌
- 已核销（已到店）的订单：不支持退款 ❌

退款申请步骤：
1. 进入「我的预约」
2. 找到订单，点击「申请退款」
3. 填写退款原因
4. 提交等待审核（门店管理员审核）

退款到账时间：审核通过后，支付宝退款1-3个工作日到账',
'退款,取消订单,退钱,退费,退票,退款政策,退款规则,退款时间', 10),

('refund', '退款审核流程',
'退款审核说明：
- 用户申请退款后，订单状态变为"退款审核中"
- 门店管理员会在工作时间内审核退款申请
- 审核通过后系统自动发起支付宝退款，1-3个工作日到账
- 如退款被拒绝，用户会收到通知，订单恢复为已支付状态
- 若对退款结果有异议，请联系人工客服：400-123-4567',
'退款审核,退款多久,退款到账,退款进度,退款状态', 8),

-- ===== VIP会员 =====
('vip', 'VIP会员等级与权益',
'VIP会员分4个等级，购买后立即生效：

等级1 见习侦探：
- 预约折扣：9.5折
- 每月体验券：2张×10元
- 生日礼券：30元

等级2 银章侦探：
- 预约折扣：9折
- 每月体验券：5张×20元
- 生日礼券：80元

等级3 金章侦探：
- 预约折扣：8.5折
- 每月体验券：10张×50元
- 生日礼券：150元

等级4 传奇侦探：
- 预约折扣：8折
- 每月体验券：15张×100元
- 生日礼券：200元

VIP通用权益：积分倍率加成、月度体验券自动发放（当月有效）、生日月自动发放生日礼券',
'VIP,会员,会员权益,折扣,等级,见习侦探,银章,金章,传奇,体验券', 10),

('vip', 'VIP购买与续费',
'VIP购买说明：
- 购买方式：在「VIP中心」页面选择套餐，仅支持支付宝支付
- 续费说明：续费天数自动叠加到当前VIP到期时间之后，不会浪费剩余天数
- 激活时机：支付宝支付成功后立即激活，权益实时生效
- 月度体验券：激活后次月1号自动发放，购买当月即可获得首批体验券

注意：VIP折扣与优惠券可叠加使用（先计算VIP折扣，再减优惠券金额）',
'VIP购买,会员购买,开通会员,续费,VIP续费,买会员', 9),

-- ===== 积分 =====
('points', '积分获取方式',
'积分获取方式：
- 每日签到：+10积分（每天仅限1次）
- 完成预约游戏（支付成功）：+100积分
- 发表评价：基础+50积分，上传图片额外+10，内容超50字额外+10，三项评分都填额外+5分
- 收藏剧本达到里程碑（每5的倍数次）：+20积分
- 完善个人资料：+30积分（仅首次）
- VIP用户享有积分倍率加成，最终积分=基础积分×VIP倍率

退款注意：退款成功后会扣除对应预约获得的100积分',
'积分,获取积分,怎么得积分,积分来源,赚积分,签到积分', 9),

('points', '积分使用与兑换',
'积分用途：
- 积分可以在「积分中心」兑换优惠券
- 每种优惠券每天最多兑换1次
- 兑换时扣除对应积分，优惠券发放到「我的优惠券」

查看积分：
- 进入「个人中心 → 我的积分」查看积分余额和明细
- 积分明细记录每次变动的原因、数量和时间',
'积分兑换,用积分,积分换券,积分怎么用,积分余额', 8),

-- ===== 优惠券 =====
('coupon', '优惠券类型与使用',
'优惠券类型：
- 满减券：订单满X元减Y元（有最低消费门槛）
- 折扣券：按比例优惠（如九折券）
- 代金券：直接抵扣固定金额

获取方式：
- VIP会员每月自动发放月度体验券（当月有效）
- 积分兑换（在积分中心操作）
- 平台活动发放
- 生日月VIP用户自动获得生日礼券

使用规则：
- 每笔订单限用1张优惠券
- VIP折扣与优惠券可叠加（先算VIP折扣，再减优惠券）
- 优惠券有有效期，过期自动失效
- 退款时，未过期的优惠券自动退还可继续使用',
'优惠券,打折,满减,代金券,折扣券,优惠,领券,用券', 9),

-- ===== 支付 =====
('payment', '支付方式说明',
'支付方式：
- 本平台目前仅支持支付宝支付（预约和VIP购买均通过支付宝完成）
- 支付完成后支付宝页面自动跳转回平台
- 如跳转失败，可在「我的预约」中查看订单支付状态

支付超时：
- 预约订单创建后30分钟内需完成支付
- 超时未支付将自动取消订单，占用的排期名额自动释放
- 可重新预约

支付失败处理：
- 若扣款成功但订单未更新，请等待1-3分钟系统自动同步
- 或联系人工客服：400-123-4567，提供支付宝交易流水号',
'支付,付款,支付宝,支付方式,怎么付,超时,支付失败', 9),

-- ===== 拼单 =====
('group', '拼单/拼团功能',
'拼单功能说明：
- 当参与人数不足剧本最低人数要求时，系统自动发起拼团
- 也可以在「拼单大厅」加入其他玩家已发起的拼单房间
- 拼单成功后，系统为各参与者自动生成正式预约订单
- 各参与者需在30分钟内各自完成支付

拼单规则：
- 拼单超过24小时未成团自动关闭
- 距游戏开始不足2小时的场次无法参与拼单
- 拼单失败（未成团）：如已支付则自动退款，未支付则直接取消

查看我的拼单：进入「个人中心 → 我的预约」查看拼单状态',
'拼单,拼团,凑人,人不够,拼车,一起玩,组队', 8),

-- ===== DM主持人 =====
('dm', 'DM主持人说明',
'DM（Dungeon Master/主持人）是剧本杀游戏的引导者和主持人。

DM职责：
- 主持整个剧本杀游戏流程
- 引导玩家入戏，维持游戏节奏
- 解答规则疑问，控制游戏时长

DM评分：
- 游戏结束后，用户可在评价中给DM打分（1-5星）
- DM的综合评分会显示在排期和门店页面供参考
- 可在预约详情中查看本次游戏的DM信息',
'DM,主持人,主持,游戏引导,DM评分,DM是什么', 7),

-- ===== 门店 =====
('store', '门店与房间说明',
'门店信息：
- 可在「门店列表」查看平台所有合作门店
- 门店详情包含地址、电话、营业时间、评分和房间信息
- 每个门店有多个主题房间，不同房间适合不同剧本

预约房间：
- 预约时系统根据场次自动分配房间
- 不同房间容纳人数不同，请按人数选择合适场次

联系门店：
- 在预约详情页面可查看门店联系方式
- 有特殊需求请提前联系门店',
'门店,房间,地址,营业时间,联系门店,门店电话', 7),

-- ===== 通知 =====
('system', '通知消息说明',
'平台通知类型：
- 预约通知：预约成功、门店确认、游戏提醒（开始前2小时）
- 支付通知：支付成功（含核销码）、退款到账
- 系统通知：平台公告、活动信息
- 优惠活动：优惠券到账、VIP到期提醒

查看通知：
- 顶部导航栏铃铛图标显示未读消息数
- 进入「消息通知」页面查看全部通知
- 支持按类型筛选：预约/支付/系统/优惠

实时推送：
- 平台支持WebSocket实时推送，登录状态下通知立即到达',
'通知,消息,提醒,铃铛,未读消息,推送', 6),

('system', '人工客服联系方式',
'人工客服信息：
- 客服热线：400-123-4567
- 服务时间：每天 9:00-22:00（全年无休）
- 邮箱：service@jubensha.com
- 在线客服：直接在AI客服窗口说"转人工"，即可发起在线人工客服会话

建议以下情况联系人工客服：
- 支付成功但订单未更新
- 退款申请被拒绝需要申诉
- 核销码无法使用
- 其他紧急情况',
'人工客服,客服,联系,电话,转人工,在线客服,投诉', 10);

-- 历史版本兼容：补充新增字段（兼容不支持 ADD COLUMN IF NOT EXISTS 的 MySQL）
SET @db_name = DATABASE();

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'ai_knowledge_base'
          AND COLUMN_NAME = 'is_faq'
    ),
    'SELECT 1',
    'ALTER TABLE `ai_knowledge_base` ADD COLUMN `is_faq` INT NOT NULL DEFAULT 0 COMMENT ''是否FAQ: 1是 0否'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'ai_knowledge_base'
          AND COLUMN_NAME = 'faq_question'
    ),
    'SELECT 1',
    'ALTER TABLE `ai_knowledge_base` ADD COLUMN `faq_question` VARCHAR(255) NULL COMMENT ''FAQ展示问题'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'ai_knowledge_base'
          AND COLUMN_NAME = 'hit_count'
    ),
    'SELECT 1',
    'ALTER TABLE `ai_knowledge_base` ADD COLUMN `hit_count` INT NOT NULL DEFAULT 0 COMMENT ''命中次数'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'ai_knowledge_base'
          AND COLUMN_NAME = 'last_hit_time'
    ),
    'SELECT 1',
    'ALTER TABLE `ai_knowledge_base` ADD COLUMN `last_hit_time` DATETIME NULL COMMENT ''最后命中时间'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- FAQ初始化：常见问题默认展示到用户端快捷问题中
UPDATE `ai_knowledge_base` SET `is_faq` = 1, `faq_question` = '如何预约剧本杀？' WHERE `title` = '预约剧本杀流程';
UPDATE `ai_knowledge_base` SET `is_faq` = 1, `faq_question` = '如何申请退款？' WHERE `title` = '退款规则';
UPDATE `ai_knowledge_base` SET `is_faq` = 1, `faq_question` = '支持哪些支付方式？' WHERE `title` = '支付方式说明';
UPDATE `ai_knowledge_base` SET `is_faq` = 1, `faq_question` = 'VIP会员有什么特权？' WHERE `title` = 'VIP会员等级与权益';
UPDATE `ai_knowledge_base` SET `is_faq` = 1, `faq_question` = '积分可以提现吗？' WHERE `title` = '积分规则';
UPDATE `ai_knowledge_base` SET `is_faq` = 1, `faq_question` = '人数不够可以拼团吗？' WHERE `title` = '拼团功能说明';

CREATE TABLE IF NOT EXISTS `ai_knowledge_hit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NULL COMMENT '用户ID',
    `session_id` VARCHAR(100) NULL COMMENT '会话ID',
    `query` VARCHAR(500) NOT NULL COMMENT '用户问题',
    `knowledge_id` BIGINT NOT NULL COMMENT '命中的知识ID',
    `knowledge_title` VARCHAR(200) NOT NULL COMMENT '命中的知识标题',
    `category` VARCHAR(50) NULL COMMENT '知识分类',
    `page` VARCHAR(255) NULL COMMENT '命中时页面',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_knowledge_id` (`knowledge_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库命中日志';

CREATE TABLE IF NOT EXISTS `ai_conversation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NULL COMMENT '用户ID',
    `session_id` VARCHAR(100) NULL COMMENT '会话ID',
    `question` TEXT NULL COMMENT '用户问题',
    `answer` TEXT NULL COMMENT 'AI回答',
    `page` VARCHAR(255) NULL COMMENT '页面路径',
    `is_transferred` INT NOT NULL DEFAULT 0 COMMENT '是否转人工',
    `provider` VARCHAR(50) NULL COMMENT 'AI提供商',
    `model` VARCHAR(100) NULL COMMENT '模型名称',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话日志';

-- 查询验证
SELECT COUNT(*) AS total_knowledge, category, COUNT(*) AS count_per_category
FROM ai_knowledge_base
WHERE is_deleted = 0
GROUP BY category
ORDER BY count_per_category DESC;
