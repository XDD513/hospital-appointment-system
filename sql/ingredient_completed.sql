/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : tcm_health_system

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 22/11/2025 22:33:35
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ingredient
-- ----------------------------
DROP TABLE IF EXISTS `ingredient`;
CREATE TABLE `ingredient`  (
  `id` bigint NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '食材名称',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类（中药材、蔬菜、肉类等）',
  `properties` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '性味（寒、热、温、凉、平）',
  `flavor` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '味道（甘、辛、酸、苦、咸）',
  `meridian` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '归经',
  `efficacy` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '功效',
  `suitable_constitution` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '适宜体质',
  `unsuitable_constitution` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '不宜体质',
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片文件名',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '食材库表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ingredient
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
INSERT INTO `ingredient` VALUES (1, '糯米', '谷物', '温', '甘', '脾、胃、肺', '补中益气，健脾养胃', 'QIXU,YANGXU', 'TANSHI,SHIRE', 'glutinous_rice.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (2, '小米', '谷物', '凉', '甘、咸', '脾、胃、肾', '健脾和胃，补益虚损', 'QIXU,YINXU', 'SHIRE', 'millet.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (3, '薏米', '谷物', '凉', '甘、淡', '脾、胃、肺', '健脾利湿，清热排脓', 'TANSHI,SHIRE', 'YANGXU', 'coix_seed.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (4, '黑米', '谷物', '平', '甘', '脾、胃', '滋阴补肾，健脾暖肝', 'QIXU,YINXU,XUEYU', 'TANSHI,SHIRE', 'black_rice.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (5, '燕麦', '谷物', '平', '甘', '脾、胃', '益脾养心，敛汗', 'QIXU,PINGHE', 'TANSHI,SHIRE', 'oats.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (6, '羊肉', '肉类', '温', '甘', '脾、肾', '温补脾胃，补肾壮阳', 'QIXU,YANGXU', 'YINXU,SHIRE', 'mutton.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (7, '牛肉', '肉类', '平', '甘', '脾、胃', '补脾胃，益气血', 'QIXU,XUEYU', 'TANSHI,SHIRE', 'beef.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (8, '鸡肉', '肉类', '温', '甘', '脾、胃', '温中益气，补精添髓', 'QIXU,YANGXU', 'YINXU', 'chicken.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (9, '鸭肉', '肉类', '凉', '甘、咸', '肺、肾', '滋阴养胃，利水消肿', 'YINXU,SHIRE', 'YANGXU', 'duck.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (10, '猪肉', '肉类', '平', '甘、咸', '脾、胃、肾', '滋阴润燥，补中益气', 'PINGHE,YINXU', 'TANSHI,SHIRE', 'pork.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (11, '鸽肉', '肉类', '平', '咸', '肝、肾', '滋肾益气，祛风解毒', 'QIXU,XUEYU', 'TANSHI,SHIRE', 'pigeon.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (12, '鲫鱼', '水产', '平', '甘', '脾、胃', '健脾利湿，和中开胃', 'QIXU,TANSHI', 'YINXU,SHIRE', 'crucian_carp.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (13, '鲤鱼', '水产', '平', '甘', '脾、肾', '健脾开胃，利水消肿', 'QIXU,TANSHI', 'SHIRE', 'carp.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (14, '黑鱼', '水产', '寒', '甘', '脾、胃', '补脾利水，去瘀生新', 'XUEYU,SHIRE', 'YANGXU', 'snakehead.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (15, '海参', '水产', '温', '咸', '肾、心', '补肾益精，养血润燥', 'QIXU,YANGXU,YINXU', 'TANSHI,SHIRE', 'sea_cucumber.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (16, '山药', '蔬菜', '平', '甘', '脾、肺、肾', '补脾养胃，生津益肺', 'QIXU,YINXU,PINGHE', 'TANSHI,SHIRE', 'yam.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (17, '莲藕', '蔬菜', '寒', '甘', '心、脾、胃', '清热生津，凉血止血', 'YINXU,XUEYU', 'YANGXU', 'lotus_root.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (18, '白萝卜', '蔬菜', '凉', '甘、辛', '肺、胃', '消食化痰，下气宽中', 'TANSHI,QIYU', 'QIXU', 'white_radish.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (19, '胡萝卜', '蔬菜', '平', '甘', '肺、脾', '健脾消食，补肝明目', 'PINGHE,QIXU', NULL, 'carrot.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (20, '冬瓜', '蔬菜', '凉', '甘、淡', '肺、大肠、膀胱', '利水消肿，清热解毒', 'TANSHI,SHIRE', 'YANGXU', 'winter_melon.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (21, '南瓜', '蔬菜', '温', '甘', '脾、胃', '补中益气，消炎止痛', 'QIXU,YANGXU', 'SHIRE', 'pumpkin.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (22, '芹菜', '蔬菜', '凉', '甘、苦', '肝、胃', '平肝清热，祛风利湿', 'SHIRE,QIYU', 'YANGXU', 'celery.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (23, '菠菜', '蔬菜', '凉', '甘', '肠、胃', '养血止血，润燥滑肠', 'XUEYU,YINXU', NULL, 'spinach.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (24, '西红柿', '蔬菜', '微寒', '甘、酸', '肝、脾、胃', '生津止渴，健胃消食', 'YINXU,SHIRE', 'YANGXU', 'tomato.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (25, '黄豆', '豆类', '平', '甘', '脾、大肠', '健脾宽中，润燥消水', 'QIXU,PINGHE', 'TANSHI,SHIRE', 'soybean.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (26, '黑豆', '豆类', '平', '甘', '脾、肾', '活血利水，祛风解毒', 'QIXU,XUEYU,YANGXU', 'TANSHI,SHIRE', 'black_bean.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (27, '红豆', '豆类', '平', '甘、酸', '心、小肠', '利水除湿，和血排脓', 'TANSHI,XUEYU', 'YINXU,SHIRE', 'red_bean.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (28, '绿豆', '豆类', '寒', '甘', '心、胃', '清热解毒，消暑利水', 'SHIRE,YINXU', 'YANGXU', 'mung_bean.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (29, '当归', '药材', '温', '甘、辛', '肝、心、脾', '补血活血，调经止痛', 'XUEYU,QIXU', 'SHIRE', 'angelica.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (30, '黄芪', '药材', '温', '甘', '脾、肺', '补气升阳，固表止汗', 'QIXU,YANGXU', 'YINXU', 'astragalus.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (31, '党参', '药材', '平', '甘', '脾、肺', '补中益气，健脾益肺', 'QIXU', 'SHIRE,YINXU', 'codonopsis.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (32, '枸杞', '药材', '平', '甘', '肝、肾', '滋补肝肾，益精明目', 'QIXU,YINXU,XUEYU', 'SHIRE,TANSHI', 'goji_berry.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (33, '红枣', '药材', '温', '甘', '脾、胃', '补中益气，养血安神', 'QIXU,XUEYU', 'TANSHI', 'red_date.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (34, '桂圆', '药材', '温', '甘', '心、脾', '补益心脾，养血安神', 'QIXU,XUEYU', 'TANSHI,SHIRE', 'longan.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (35, '莲子', '药材', '平', '甘、涩', '脾、肾、心', '补脾止泻，养心安神', 'QIXU,PINGHE', 'SHIRE,TANSHI', 'lotus_seed.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (36, '百合', '药材', '寒', '甘', '肺、心', '养阴润肺，清心安神', 'YINXU,QIYU', 'YANGXU', 'lily.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (37, '银耳', '药材', '平', '甘、淡', '肺、胃', '滋阴润肺，养胃生津', 'YINXU,PINGHE', 'TANSHI,SHIRE', 'tremella.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (38, '山楂', '药材', '微温', '酸、甘', '脾、胃、肝', '消食化积，活血散瘀', 'TANSHI,XUEYU,QIYU', 'YINXU,SHIRE', 'hawthorn.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (39, '陈皮', '药材', '温', '辛、苦', '脾、肺', '理气健脾，燥湿化痰', 'TANSHI,QIYU', 'YINXU,SHIRE', 'tangerine_peel.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (40, '茯苓', '药材', '平', '甘、淡', '心、脾、肾', '利水渗湿，健脾宁心', 'TANSHI,QIXU', 'YINXU,SHIRE', 'poria.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (41, '核桃', '坚果', '温', '甘', '肾、肺', '补肾温肺，润肠通便', 'QIXU,YANGXU,YINXU', 'TANSHI,SHIRE', 'walnut.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (42, '花生', '坚果', '平', '甘', '脾、肺', '健脾和胃，润肺化痰', 'QIXU,PINGHE', 'TANSHI,SHIRE', 'peanut.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (43, '芝麻', '坚果', '平', '甘', '肝、肾', '补肝肾，润五脏', 'YINXU,XUEYU', 'TANSHI,SHIRE', 'sesame.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (44, '杏仁', '坚果', '温', '苦', '肺、大肠', '止咳平喘，润肠通便', 'QIXU,YINXU', 'TANSHI,SHIRE', 'almond.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (45, '香菇', '菌菇', '平', '甘', '胃、肝', '补气血，健脾胃', 'QIXU,PINGHE', 'SHIRE,TANSHI', 'shiitake.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (46, '木耳', '菌菇', '平', '甘', '胃、大肠', '补气养血，润肺止咳', 'QIXU,XUEYU', 'TANSHI,SHIRE', 'wood_ear.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (47, '银耳', '菌菇', '平', '甘、淡', '肺、胃', '滋阴润肺，养胃生津', 'YINXU,PINGHE', 'TANSHI,SHIRE', 'white_fungus.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (48, '苹果', '水果', '凉', '甘、酸', '脾、胃', '生津润肺，健脾开胃', 'PINGHE,YINXU', 'YANGXU,SHIRE', 'apple.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (49, '梨', '水果', '寒', '甘、微酸', '肺、胃', '生津润燥，清热化痰', 'YINXU,SHIRE', 'YANGXU', 'pear.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (50, '葡萄', '水果', '平', '甘、酸', '肺、脾、肾', '补气血，强筋骨', 'QIXU,XUEYU', 'SHIRE,TANSHI', 'grape.jpg', '1', '2025-11-05 19:08:56', '2025-11-05 19:08:56');
INSERT INTO `ingredient` VALUES (51, '乌梅', '中药材', '平', '酸、涩', '肝、脾、肺、大肠', '敛肺止咳，涩肠止泻，生津止渴，安蛔止痛，用于久咳久泻，虚热消渴，蛔厥腹痛', 'QIXU,YINXU,TANSHI', 'SHIRE,XUEYU', NULL, '1', '2025-11-22 22:28:45', '2025-11-22 22:28:45');
INSERT INTO `ingredient` VALUES (52, '乌鸡', '肉类', '平', '甘', '肝、肾、脾', '补益肝肾，滋阴养血，健脾益气，适用于虚劳羸弱、产后体虚等症', 'QIXU,YINXU,PINGHE', 'TANSHI,SHIRE', NULL, '1', '2025-11-22 22:28:51', '2025-11-22 22:28:51');
INSERT INTO `ingredient` VALUES (53, '佛手', '中药材', '温', '辛、苦、酸', '肝、脾、胃、肺', '疏肝理气，和胃止痛，燥湿化痰。用于肝胃气滞，胸胁胀痛，胃脘痞满，食少呕吐，咳嗽痰多。', 'QIXU,TANSHI,QIYU', 'YINXU,SHIRE', NULL, '1', '2025-11-22 22:28:58', '2025-11-22 22:28:58');
INSERT INTO `ingredient` VALUES (54, '冰糖', '调料', '平', '甘', '脾、肺', '补中益气，和胃润肺，止咳化痰，养阴生津，调和药性', 'QIXU,YINXU,PINGHE', 'TANSHI,SHIRE', NULL, '1', '2025-11-22 22:29:05', '2025-11-22 22:29:05');
INSERT INTO `ingredient` VALUES (55, '决明子', '中药材', '热', '甘、苦、咸', '肝、肾、大肠', '清肝明目，润肠通便，降脂降压，适用于目赤肿痛、便秘等症', 'SHIRE,TANSHI', 'YANGXU,PIXU', NULL, '1', '2025-11-22 22:29:12', '2025-11-22 22:32:35');
INSERT INTO `ingredient` VALUES (56, '大枣', '中药材', '温', '甘', '脾、胃、心', '补中益气，养血安神，缓和药性。用于脾胃虚弱，食少便溏，血虚萎黄，妇人脏躁。', 'QIXU,YANGXU,PINGHE', 'SHIRE,TANSHI', NULL, '1', '2025-11-22 22:29:18', '2025-11-22 22:29:18');
INSERT INTO `ingredient` VALUES (57, '大米', '谷物', '平', '甘', '脾、胃、肺', '补中益气，健脾和胃，除烦渴，止泻痢，为日常养生主食', 'PINGHE,QIXU,YINXU', 'TANSHI', NULL, '1', '2025-11-22 22:29:25', '2025-11-22 22:29:25');
INSERT INTO `ingredient` VALUES (58, '排骨', '肉类', '平', '甘、咸', '脾、胃、肾', '补中益气，滋养脾胃，强健筋骨，滋阴润燥，改善贫血', 'QIXU,YINXU,PINGHE', 'TANSHI,SHIRE', NULL, '1', '2025-11-22 22:29:31', '2025-11-22 22:29:31');
INSERT INTO `ingredient` VALUES (59, '核桃仁', '其他', '温', '甘', '肾、肺、大肠', '补肾固精，温肺定喘，润肠通便，适用于肾虚腰痛、肺虚咳嗽等症', 'QIXU,YANGXU,PINGHE', 'YINXU,SHIRE,TANSHI', NULL, '1', '2025-11-22 22:29:37', '2025-11-22 22:29:37');
INSERT INTO `ingredient` VALUES (60, '沙参', '中药材', '温', '甘、微苦', '肺、胃', '养阴清肺，益胃生津，化痰益气。适用于肺燥干咳、阴虚劳嗽、津伤口渴等症。', 'YINXU,FEIZAO', 'SHIHAN,WEIHAN', NULL, '1', '2025-11-22 22:29:44', '2025-11-22 22:32:33');
INSERT INTO `ingredient` VALUES (61, '玉竹', '中药材', '平', '甘', '肺、胃', '养阴润燥，生津止渴，用于肺胃阴伤，燥热咳嗽，咽干口渴，内热消渴', 'YINXU,QIXU', 'TANSHI,SHIRE', NULL, '1', '2025-11-22 22:29:50', '2025-11-22 22:29:50');
INSERT INTO `ingredient` VALUES (62, '玫瑰花', '中药材', '温', '甘、微苦', '肝、脾', '行气解郁，活血止痛，疏肝和胃，美容养颜', 'QIXU,QIYU,XUEYU', 'YINXU,SHIRE', NULL, '1', '2025-11-22 22:29:57', '2025-11-22 22:29:57');
INSERT INTO `ingredient` VALUES (63, '甘草', '中药材', '平', '甘', '心、肺、脾、胃', '补脾益气，清热解毒，祛痰止咳，缓急止痛，调和诸药', 'QIXU,TANSHI', 'SHIRE,YINXU', NULL, '1', '2025-11-22 22:30:03', '2025-11-22 22:30:03');
INSERT INTO `ingredient` VALUES (64, '生姜', '调料', '温', '辛', '肺、脾、胃', '发汗解表，温中止呕，温肺止咳，解鱼蟹毒', 'QIXU,YANGXU,TANSHI', 'YINXU,SHIRE', NULL, '1', '2025-11-22 22:30:09', '2025-11-22 22:30:09');
INSERT INTO `ingredient` VALUES (65, '白术', '中药材', '温', '苦、甘', '脾、胃', '健脾益气，燥湿利水，止汗安胎。主治脾虚食少，腹胀泄泻，痰饮眩悸，水肿，自汗，胎动不安。', 'QIXU,TANSHI', 'YINXU,SHIRE', NULL, '1', '2025-11-22 22:30:16', '2025-11-22 22:30:16');
INSERT INTO `ingredient` VALUES (66, '盐', '调料', '寒', '咸', '肾、心、肺、胃', '清热解毒，软坚散结，润燥通便，引药归经，调味和中', 'TANSHI,SHIRE', 'YINXU,YANGXU,QIXU', NULL, '1', '2025-11-22 22:30:22', '2025-11-22 22:30:22');
INSERT INTO `ingredient` VALUES (67, '红糖', '调料', '温', '甘', '脾、胃、肝', '补中缓急、和血行瘀、温胃散寒，适用于虚寒腹痛、产后恶露不尽等症', 'QIXU,YANGXU,TEBING', 'YINXU,SHIRE,TANSHI', NULL, '1', '2025-11-22 22:30:29', '2025-11-22 22:30:29');
INSERT INTO `ingredient` VALUES (68, '红花', '中药材', '温', '辛', '心、肝', '活血通经，散瘀止痛，用于经闭痛经、恶露不行、跌打损伤等症', 'XUEYU,QIYU', 'YINXU,SHIRE', NULL, '1', '2025-11-22 22:30:35', '2025-11-22 22:30:35');
INSERT INTO `ingredient` VALUES (69, '荷叶', '其他', '凉', '苦、涩', '肝、脾、胃', '清热解暑，升发清阳，凉血止血，利湿消肿，降脂减肥', 'TANSHI,SHIRE,XUEYU', 'QIXU,YANGXU,YINXU', NULL, '1', '2025-11-22 22:30:41', '2025-11-22 22:30:41');
INSERT INTO `ingredient` VALUES (70, '菊花', '中药材', '凉', '甘、苦', '肺、肝', '疏散风热，平肝明目，清热解毒。用于风热感冒，头痛眩晕，目赤肿痛，眼目昏花。', 'SHIRE,PINGHE', 'YANGXU,WEIHAN', NULL, '1', '2025-11-22 22:30:48', '2025-11-22 22:30:48');
INSERT INTO `ingredient` VALUES (71, '虾仁', '肉类', '温', '甘、咸', '肝、肾', '补肾壮阳，通乳托毒，富含蛋白质，能增强体力，改善肾虚症状', 'QIXU,YANGXU,PINGHE', 'YINXU,SHIRE,TANSHI', NULL, '1', '2025-11-22 22:30:55', '2025-11-22 22:30:55');
INSERT INTO `ingredient` VALUES (72, '蜂蜜', '其他', '平', '甘', '脾、肺、大肠', '补中润燥，止痛解毒，润肺止咳，润肠通便，调和药性', 'PINGHE,QIXU,YINXU', 'TANSHI,SHIRE', NULL, '1', '2025-11-22 22:31:01', '2025-11-22 22:31:01');
INSERT INTO `ingredient` VALUES (73, '雪梨', '蔬菜', '凉', '甘、微酸', '肺、胃', '生津润燥、清热化痰、润肺止咳，适用于肺燥咳嗽、咽喉干痛等症状', 'SHIRE,YINXU', 'PISHI,YANGXU', NULL, '1', '2025-11-22 22:31:07', '2025-11-22 22:31:07');
INSERT INTO `ingredient` VALUES (74, '韭菜', '蔬菜', '温', '辛、甘', '肝、胃、肾', '温中行气，散瘀解毒，补肾助阳，主治腰膝酸软，阳痿遗精，脘腹冷痛', 'QIXU,YANGXU,HANSHI', 'YINXU,SHIRE', NULL, '1', '2025-11-22 22:31:14', '2025-11-22 22:31:14');
INSERT INTO `ingredient` VALUES (75, '食用油', '调料', '平', '甘', '脾、胃、大肠', '润燥通便，滋养肌肤，调和药性，促进药物吸收', 'PINGHE,QIXU,YINXU', 'TANSHI,SHIRE', NULL, '1', '2025-11-22 22:31:20', '2025-11-22 22:31:20');
INSERT INTO `ingredient` VALUES (76, '麦冬', '谷物', '温', '甘、微苦', '心、肺、胃', '养阴生津，润肺清心，用于肺燥干咳，阴虚劳嗽，津伤口渴，心烦失眠', 'YINXU,SHIRE', 'YANGXU,TANSHI', NULL, '1', '2025-11-22 22:31:26', '2025-11-22 22:32:26');
INSERT INTO `ingredient` VALUES (77, '黄酒', '调料', '温', '甘、辛', '肝、脾、胃', '温中散寒，活血通络，促进血液循环，增强药效，常用于药引和烹饪调味', 'QIXU,YANGXU,HANSHI', 'YINXU,SHIRE', NULL, '1', '2025-11-22 22:31:32', '2025-11-22 22:31:32');
INSERT INTO `ingredient` VALUES (78, '黑木耳', '中药材', '平', '甘', '胃、大肠', '补气养血，润肺止咳，止血活血，降压降脂，润肠通便', 'PINGHE,QIXU,YINXU,TANSHI', 'SHIRE', NULL, '1', '2025-11-22 22:31:39', '2025-11-22 22:31:39');

-- 补全时间: 2025-11-22 23:12:49
SET FOREIGN_KEY_CHECKS = 1;
