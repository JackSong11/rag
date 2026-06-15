-- ----------------------------
-- Table structure for document
-- ----------------------------
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `thumbnail`        text,
    `kb_id`            varchar(256) NOT NULL,
    `parser_id`        varchar(32)  NOT NULL,
    `pipeline_id`      varchar(32)           DEFAULT NULL,
    `parser_config`    longtext     NOT NULL,
    `source_type`      varchar(128) NOT NULL,
    `type`             varchar(32)  NOT NULL COMMENT '文件类型',
    `name`             varchar(255)          DEFAULT NULL,
    `location`         varchar(255)          DEFAULT NULL,
    `size`             bigint       NOT NULL,
    `token_num`        int          NOT NULL,
    `chunk_num`        int          NOT NULL,
    `progress`         float        NOT NULL,
    `progress_msg`     text,
    `process_begin_at` datetime              DEFAULT NULL,
    `process_duration` float        NOT NULL,
    `suffix`           varchar(32)  NOT NULL,
    `content_hash`     varchar(32)           DEFAULT NULL,
    `run`              varchar(1)            DEFAULT NULL,
    `status`           varchar(1)            DEFAULT NULL,
    `delete_flag`      tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除字段 0:代表有效， 1:代表逻辑删除',
    `creator`          varchar(20)           DEFAULT NULL COMMENT '创建人',
    `editor`           varchar(20)           DEFAULT NULL COMMENT '修改人',
    `created_time`     datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_time`    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT = 'document' ROW_FORMAT = Dynamic;