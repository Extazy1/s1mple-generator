# 数据库初始化

-- 创建库
create database if not exists my_db;

-- 切换库
use my_db;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_userAccount (userAccount)
    ) comment '用户' collate = utf8mb4_unicode_ci;

-- 代码生成器表
create table if not exists generator
(
    id          bigint auto_increment comment 'id' primary key,
    name        varchar(128)                       null comment '名称',
    description text                               null comment '描述',
    basePackage varchar(128)                       null comment '基础包',
    version     varchar(128)                       null comment '版本',
    author      varchar(128)                       null comment '作者',
    tags        varchar(1024)                      null comment '标签列表（json 数组）',
    picture     varchar(256)                       null comment '图片',
    fileConfig  text                               null comment '文件配置（json字符串）',
    modelConfig text                               null comment '模型配置（json字符串）',
    distPath    text                               null comment '代码生成器产物路径',
    status      int      default 0                 not null comment '状态',
    userId      bigint                             not null comment '创建用户 id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId)
    ) comment '代码生成器' collate = utf8mb4_unicode_ci;

INSERT INTO user (userAccount, userPassword, userName, userAvatar, userProfile, userRole, createTime, updateTime, isDelete) VALUES
                ('user1', 'password1', '张三', 'avatar_url1', '这是一个简介', 'user', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
                ('user2', 'password2', '李四', 'avatar_url2', '这是另一个简介', 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
                ('user3', 'password3', '王五', 'avatar_url3', '简介三', 'user', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
                ('user4', 'password4', '赵六', 'avatar_url4', '简介四', 'ban', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO generator (name, description, basePackage, version, author, tags, picture, fileConfig, modelConfig, distPath, status, userId, createTime, updateTime, isDelete) VALUES
                ('Generator1', '描述1', 'com.example.base', '1.0.0', 'Author1', '["tag1", "tag2"]', 'picture_url1', '{"config1": "value1"}', '{"model1": "value1"}', '/path/to/dist1', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
                ('Generator2', '描述2', 'com.example.base', '1.0.1', 'Author2', '["tag3", "tag4"]', 'picture_url2', '{"config2": "value2"}', '{"model2": "value2"}', '/path/to/dist2', 0, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
                ('Generator3', '描述3', 'com.example.base', '1.0.2', 'Author3', '["tag5", "tag6"]', 'picture_url3', '{"config3": "value3"}', '{"model3": "value3"}', '/path/to/dist3', 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
                ('Generator4', '描述4', 'com.example.base', '1.0.3', 'Author4', '["tag7", "tag8"]', 'picture_url4', '{"config4": "value4"}', '{"model4": "value4"}', '/path/to/dist4', 0, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
