# 手机号登录注册 API — 设计文档

日期：2026-11-24
状态：已批准

## 意图与动机

一个 Java 后端 REST API，让用户用手机号注册、登录，登录后返回 JWT 令牌，用于练手学习后端鉴权的完整闭环。

## 目标

- `POST /api/auth/register`（手机号 + 密码）→ 创建用户，密码 BCrypt 加密存储
- `POST /api/auth/login`（手机号 + 密码）→ 校验通过返回 JWT
- `GET /api/users/me`（带 JWT）→ 返回当前用户信息，演示鉴权
- 手机号格式校验 + 唯一性校验
- 统一错误返回（手机号已存在 / 密码错误 / 手机号格式非法 / 参数缺失等）

## 非目标（练手阶段不做）

- 短信验证码
- 找回 / 修改密码
- 刷新令牌
- 邮箱、第三方登录
- 前端页面

## 约束

- 语言：Java 17 (LTS)
- 框架：Spring Boot 3.x
- 构建：Maven
- 数据库：H2（内存 / 文件模式），结构上可切 MySQL
- 鉴权：手写 JWT 过滤器 + BCrypt（spring-security-crypto），不上整套 Spring Security

## 方案（Approaches）

- **方案 A（选定）**：Spring Boot 3 + Maven + H2 + JWT + BCrypt。零安装、可直接跑，学习价值高。
- **方案 B**：Spring Boot 3 + Maven + MySQL + JWT。更贴近真实生产，但需装 MySQL 服务，练手偏重。
- **方案 C**：轻量框架 Javalin。更简单，但非主流、学习价值低，不利于后续迁移到真实项目。

选定方案 A：练手阶段零安装最省事，且分层结构完整，后续可平滑替换数据库、接入 Spring Security。

## 已决定的技术细节

- 密码用 BCrypt 哈希存储（`spring-security-crypto` 的 `BCryptPasswordEncoder`）
- JWT 用 `jjwt` 库，`HS256`，密钥放配置文件
- 分层：`controller / service / repository / entity / dto / config / security`
- 旧 Python 演示文件 `main.py` 删除，仓库定位为 Java 项目

## 留给实现者

- 具体字段命名、DTO 结构
- 异常处理细节与错误响应格式
- 一个可 `mvn test` 跑通的冒烟测试
