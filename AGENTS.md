# AGENTS.md · 校园快递点导航与空闲度查询平台

> **这是本项目唯一的权威入口文档。** 任何 AI（或人）接手前必须先读完本文件，再动手。
> 最后更新：2026-11-19 ｜ 维护人：郑俊霖 ｜ 当前阶段：准备阶段结束，进入 P1 设计落地

---

## 0. 怎么用这份文档

### 0.1 给 AI 的开场白（复制即用）

```
你在 E:\毕业设计 目录下工作，这是一个本科毕业设计项目。
请先完整阅读 AGENTS.md，再阅读 docs/毕业设计项目计划书（修订版）.docx 的要点。
严格遵守 AGENTS.md 中的：技术栈锁定、目录规范、接口契约、验收命令、任务边界。
不要擅自升级依赖版本、不要改动 toolchain 目录、不要修改已冻结的数据库设计。
开工前先用 scripts\check-env.bat 确认环境可用。
```

### 0.2 三条铁律

1. **契约优先**：数据库表结构与接口清单（本文第 6、7 节）是并行开发的契约，改动必须先改本文并说明原因。
2. **只动自己的文件**：多 AI 并行时，每个 AI 只在分配到自己的包/目录内写代码（见第 11 节）。
3. **改完必须自测**：每次提交前跑第 13 节的验收命令，并把结果贴出来。

---

## 1. 项目是什么

| 项 | 内容 |
|---|---|
| 题目 | 基于 Spring Boot 与百度地图 API 的校园快递点导航与空闲度查询平台设计与实现 |
| 学生 | 郑俊霖（计算机科学与技术） |
| 目标 | 解决校园快递"找不到点、去了排长队"两个痛点，打通"包裹到站 → 学生查件 → 地图导航 → 到站取件 → 拥挤度反馈"闭环 |
| 交付 | 可运行系统（后端 + 学生端 + 站点端 + 管理端）+ 论文 + 演示视频 |
| 答辩要求 | 老师明确要求：**功能工作量要够**、**数据按物理站点划分**（快递点多品牌混营）、**需求要自己梳理** |

### 1.1 不可违背的设计基线（老师已认可的方向）

1. **站点模型**：`station`（物理站点）与 `station_company`（代理快递公司）多对多。**不按快递公司划分数据**。
2. **角色**：学生、站点管理员（只能看本站数据）、超级管理员（全平台）。数据按站点行级隔离。
3. **包裹状态机**：待取 → 已取；上架 30 分钟内可撤销；待取超 48 小时标记超时并告警（不改状态值）。
4. **拥挤度**：三档上报 + 时间衰减加权 → 0–100 指数，缓存进 `station.crowd_level / crowd_score`，地图标记颜色由它决定。
5. **地图**：百度地图 JS API（GL 版，`BMapGL`），浏览器端 AK，坐标系 **BD-09**。

---

## 2. 当前进度（截至 2026-11-19）

| 事项 | 状态 | 说明 |
|---|---|---|
| 本机开发环境 | ✅ 完成 | JDK21 / Maven3.9.16 / Node22 / Git / MySQL8.4.4，详见第 3 节 |
| 后端工程骨架 | ✅ 完成 | Spring Boot 3.5.16 可编译、可启动，`/api/health` 返回 dbOk=true |
| 前端工程骨架 | ✅ 完成 | Vue3 + Vite8 + Element Plus，`npm run build` 通过 |
| 计划书 v2 | ✅ 完成 | `docs/毕业设计项目计划书（修订版）.docx`（7 页、9 模块、9 表、47 接口、18 周进度） |
| 数据库 | ⚠️ 空库 | `campus_express` 已建，**尚无任何表** |
| 接口清单 | ⚠️ 文档级 | 见本文第 7 节，尚未落成代码 |
| 百度地图 AK | ❌ 未接入 | 实名认证已完成；需创建"浏览器端"应用拿 32 位 AK（白名单填 `*`） |
| 校园站点坐标 | ❌ 未采集 | 5 个站点（校内 3、北门 1、岐头门 1）需实地取经纬度 |
| 设计冻结 | ❌ 未确认 | 计划书 v2 需导师确认后再冻结 |

### 2.1 下一步该干什么（按顺序）

1. **P1 设计落地（当前）**：执行 `db/schema.sql` 建 9 张表 → 生成实体/Mapper → 落地接口骨架（先返回假数据也行）。
2. **P2 后端核心**：认证鉴权 → 站点/品牌 → 包裹上架（含 Excel）→ 学生查件。
3. **P3 后端进阶**：空闲度算法、核销、统计告警、定时任务。
4. **P4 前端三端**：学生端 / 站点端 / 管理端。
5. **P5 联调演示**：模拟数据、地图接入、录屏。
6. **P6 论文**：架构图、时序图、测试用例、答辩 PPT。

---

## 3. 环境事实（照抄即可）

### 3.1 工具链（全部便携版，非管理员安装）

| 工具 | 版本 | 路径 |
|---|---|---|
| JDK | 21.0.13 (Temurin) | `E:\毕业设计\toolchain\jdk-21.0.13` |
| Maven | 3.9.16 | `E:\毕业设计\toolchain\apache-maven-3.9.16` |
| Node.js / npm | 22.23.5 / 10.9.9 | `E:\毕业设计\toolchain\node-v22.23.5-win-x64` |
| Git | 2.56.1 | `E:\毕业设计\toolchain\PortableGit` |
| MySQL | 8.4.4 | `E:\mysql`（数据目录 `E:\mysql\data`，**禁止改动**） |
| IDEA | Ultimate 2026.1.3 | `E:\IntelliJ IDEA 2026.1.3` |
| VS Code | 1.92.2 + Java 扩展包 + Volar | `E:\Microsoft VS Code` |

环境变量（用户级，已配置）：`JAVA_HOME`、`MAVEN_HOME`、`M2_HOME`、`NODE_HOME`、`MYSQL_HOME` + PATH。
**新开终端**才生效；当前终端若找不到命令，先执行 `scripts\check-env.bat`。

### 3.2 数据库

| 项 | 值 |
|---|---|
| 地址 | `127.0.0.1:3306` |
| root | `root` / `123456` |
| 应用账号 | `campus` / `campus123456`（仅 `campus_express` 库） |
| 库 | `campus_express`（utf8mb4） |
| 启动 | `scripts\start-mysql.bat`（已在运行则跳过；登录自启由启动目录 VBS 负责） |
| 停止 | `scripts\stop-mysql.bat` |

### 3.3 启动 / 构建 / 自检

| 操作 | 命令 |
|---|---|
| 环境自检 | `scripts\check-env.bat` |
| 启 MySQL | `scripts\start-mysql.bat` |
| 跑后端 | `scripts\run-backend.bat` → http://localhost:8080 |
| 打包后端 | `scripts\build-backend.bat` → `campus-express\target\campus-express-1.0.0.jar` |
| 跑前端 | `scripts\run-frontend.bat` → http://localhost:5173 |
| 一键全套 | `scripts\start-all.bat` |
| 健康检查 | `GET http://localhost:8080/api/health` |
| 接口文档 | http://localhost:8080/swagger-ui.html |

### 3.4 禁区

- ❌ 不要删改 `E:\毕业设计\toolchain`、`E:\mysql`、`C:\Users\lqxhhbp\.m2\settings.xml`
- ❌ 不要用 `E:\vendor\git-for-windows`（已损坏，报 "fork bomb"），用 `toolchain\PortableGit\cmd\git.exe`
- ❌ 不要安装需要管理员权限的软件（当前账户非管理员，UAC 会卡住）
- ❌ 不要把 `node_modules/`、`target/`、`dist/`、`toolchain/` 提交进 Git
- ❌ 不要改 MySQL root 密码、不要删 `room` 库（那是另一门课的数据）

---

## 4. 技术栈（已锁定，禁止擅自升级）

| 层级 | 技术 | 版本 | 备注 |
|---|---|---|---|
| 后端框架 | Spring Boot | 3.5.16 | 用 `spring-boot-starter-web`，不要引入 Spring Security（用 JWT 拦截器） |
| 运行环境 | JDK | 21 | 不要写 `var` 以外的新特性也要兼容；不要降到 17 |
| 持久层 | MyBatis-Plus | 3.5.18 | starter 必须用 `mybatis-plus-spring-boot3-starter` |
| 数据库 | MySQL | 8.4.4 | 驱动 `mysql-connector-j`（由 BOM 管理版本） |
| Excel | EasyExcel | 4.0.3 | 不要换成 POI 手写 |
| 鉴权 | JJWT | 0.13.0 | HS256，密钥放 `application.yml` |
| 接口文档 | SpringDoc OpenAPI | 2.10.0 | 注解 `@Tag` / `@Operation` |
| 工具类 | Hutool | 5.8.48 | 加密、日期、Bean 拷贝 |
| 前端 | Vue 3.5 + Element Plus 2.14 + Vite 8 | — | 不要升级到 Vue4/ElementPlus3 |
| 路由/状态 | vue-router 4.6 + Pinia 4.1 | — | |
| 图表 | ECharts 6.1 | — | 统计页 |
| 地图 | 百度地图 JS API（GL） | `BMapGL` | `https://api.map.baidu.com/api?v=1.0&type=webgl&ak=AK` |

后端统一规范：
- 统一响应 `Result<T>`（`code/message/data`，200 成功）
- 分页返回 `PageResult<T>`（`total/records`）
- 接口路径统一 `/api/{模块}/...`
- 参数校验用 `@Valid` + `jakarta.validation`
- 异常统一由 `GlobalExceptionHandler` 转 `Result`

前端统一规范：
- 请求统一走 `src/api/request.js`（axios 实例，baseURL `/api`，token 拦截）
- 页面放 `src/views/{student|station|admin}/`，组件放 `src/components/`
- 路由 meta 标记 `role`，路由守卫按角色拦截
- 地图统一封装在 `src/components/CampusMap.vue`（只此一处初始化 BMapGL）

---

## 5. 目录结构（目标态）

```
E:\毕业设计\
├─ campus-express\                    后端
│  ├─ pom.xml
│  └─ src\main\
│     ├─ java\com\campus\express\
│     │  ├─ CampusExpressApplication.java
│     │  ├─ common\        Result / PageResult / 异常 / 常量 / 工具
│     │  ├─ config\        MybatisPlusConfig / WebMvcConfig / SwaggerConfig
│     │  ├─ security\      JwtUtil / AuthInterceptor / RequireRole 注解
│     │  ├─ controller\    
│     │  ├─ service\       (含 impl)
│     │  ├─ mapper\        
│     │  ├─ entity\        
│     │  ├─ dto\           入参
│     │  ├─ vo\            出参
│     │  └─ task\          定时任务
│     └─ resources\
│        ├─ application.yml / application-dev.yml
│        ├─ mapper\        XML（复杂 SQL）
│        └─ db\            schema.sql / seed.sql
├─ campus-express-web\                 前端
│  ├─ vite.config.js
│  └─ src\
│     ├─ api\              request.js + 各模块 api
│     ├─ views\            student\ station\ admin\
│     ├─ components\       CampusMap.vue / CrowdTag.vue ...
│     ├─ router\           index.js（含守卫）
│     ├─ stores\           user.js
│     └─ utils\
├─ docs\                               计划书、设计文档、生成脚本
├─ scripts\                            启停与自检脚本
└─ toolchain\                          工具链（不入库）
```

---

## 6. 数据库设计（9 张表，冻结前需用户确认）

建库脚本：`campus-express/src/main/resources/db/schema.sql`（**待创建**）。

| 表 | 字段（简） | 说明 |
|---|---|---|
| `sys_user` | id, phone, password, real_name, role(0学生/1站长/2超管), station_id, status(0待审/1正常/2禁用), created_at, updated_at, deleted | 用户；站长必须绑定 station_id |
| `station` | id, name, address, lng, lat, business_hours, contact_phone, status, crowd_level(0/1/2), crowd_score(0-100), crowd_updated_at, created_at | 站点；拥挤度为聚合缓存 |
| `station_company` | id, station_id, company_code, company_name | 站点×快递公司 多对多 |
| `express_package` | id, pickup_code(唯一), receiver_phone, station_id, company_code, shelf_no, status(0待取/1已取/2已撤销), operator_id, inbound_at, taken_at, remark | 包裹主表 |
| `package_log` | id, package_id, action(0上架/1撤销/2核销), operator_id, created_at | 状态流水 |
| `idle_report` | id, station_id, user_id, level(0/1/2), source(0学生/1站长校准), ip, created_at | 上报明细 |
| `station_audit` | id, user_id, station_id, apply_reason, status(0待审/1通过/2驳回), reviewer_id, review_time, created_at | 站长入驻审核 |
| `alert_record` | id, type(0超时/1异常上报/2站点停用), station_id, package_id, content, status(0未处理/1已处理), created_at, handled_at | 告警 |
| `operation_log` | id, user_id, module, action, detail, ip, created_at | 操作审计 |

索引要求：`express_package(receiver_phone, status)`、`express_package(pickup_code)` 唯一、`idle_report(station_id, created_at)`、`express_package(station_id, status)`。

---

## 7. 接口清单（47 个，并行开发的契约）

> 前缀 `/api`；除标注"公开"外均需登录；`[S]`=学生，`[A]`=站长，`[M]`=超管。

**M1 认证（6）**：`POST /auth/login`、`POST /auth/register`、`POST /auth/logout`、`GET /auth/me`、`PUT /auth/password`、`POST /auth/station/apply [S]`
**M2 站点与品牌（7）**：`GET /stations`（公开）、`GET /stations/{id}`（公开）、`POST /stations [M]`、`PUT /stations/{id} [M/A本人]`、`DELETE /stations/{id} [M]`、`GET /companies`（公开）、`PUT /stations/{id}/companies [M]`
**M3 包裹上架（8）**：`POST /packages [A]`、`POST /packages/import [A]`、`GET /packages/import/template [A]`、`GET /packages [A]`、`GET /packages/{id} [A]`、`PUT /packages/{id}/revoke [A]`、`GET /packages/stats/today [A]`、`GET /packages/export [A]`
**M4 学生查件（3）**：`GET /packages/query?phone=` （公开+限流）、`GET /packages/query/{pickupCode}`（公开+限流）、`GET /packages/{id}/timeline [S]`
**M5 地图（4）**：`GET /map/stations`（公开）、`GET /map/route?from=&to=`（公开，走后端调百度 Web 服务或前端直连）、`GET /map/nearby?lng=&lat=`（公开）、`POST /map/geocode [M]`
**M6 空闲度（5）**：`POST /idle/reports [S]`、`GET /idle/reports/recent?stationId=`（公开）、`GET /idle/reports/trend?stationId=&hours=1`（公开）、`POST /idle/reports/calibrate [A]`、`GET /idle/summary`（公开）
**M7 核销（4）**：`POST /packages/verify [A]`、`GET /packages/verify/{pickupCode} [A]`、`GET /packages/taken [A]`、`GET /packages/overdue [A]`
**M8 统计告警（6）**：`GET /stats/station-throughput [M]`、`GET /stats/station-daily [A]`、`GET /stats/operator-workload [M]`、`GET /alerts [M]`、`PUT /alerts/{id}/handle [M]`、`GET /stats/crowd-distribution [M]`
**M9 审核与运维（4）**：`GET /audits [M]`、`PUT /audits/{id} [M]`、`GET /logs [M]`、`GET /system/info [M]`

### 7.1 关键约定
- 手机号对外统一脱敏：`138****5678`
- 取件码为 6–8 位数字/字母，全局唯一
- 核销必须用条件更新：`update ... set status=1, taken_at=now() where id=? and status=0`，影响行数=0 即失败
- 学生查件接口按 IP + 手机号限流（Hutool 或内存 Map 计数即可），防爬

---

## 8. 权限矩阵（接口级）

| 资源 | 学生 | 站长 | 超管 |
|---|---|---|---|
| 站点/品牌 读 | ✅ | ✅ | ✅ |
| 站点/品牌 写 | ❌ | 仅本站营业信息 | ✅ |
| 包裹 上架/撤销/核销 | ❌ | 仅本站 | ❌（可查） |
| 包裹 查询（本人） | ✅ | — | — |
| 空闲度上报 | ✅ | 校准本站 | ❌ |
| 统计 | 公开概览 | 本站 | 全平台 |
| 告警/日志/审核 | ❌ | ❌ | ✅ |

实现方式：`@RequireRole({Role.STATION_ADMIN})` 注解 + `AuthInterceptor` 校验；站长额外校验 `station_id` 一致（行级隔离）。

---

## 9. 关键算法

### 9.1 拥挤度指数（0–100）
```
score = 100 * Σ(w_i * level_i) / Σ(w_i)
w_i = 0.5 ^ (minutes_ago_i / 30)     // 半衰期 30 分钟
```
- `level`：空闲=0.1、一般=0.5、拥挤=0.9
- 40 分钟前的上报权重 < 0.4，自动弱化
- 结果映射：<33 空闲（绿）、33–66 一般（黄）、>66 拥挤（红）
- 每次上报后异步重算；定时任务每 10 分钟重算一次（防止数据陈旧）

### 9.2 防刷
- 同用户同站点 10 分钟内只算 1 次
- 同用户每日上限 5 次
- 同站点每分钟上报超过 10 条时，取置信区间剔除异常

### 9.3 超时判定
`status=0 and inbound_at < now() - interval 48 hour` → 生成 `alert_record`（唯一键防重复）

---

## 10. 前端页面（18 个）

| 端 | 页面 | 路由 |
|---|---|---|
| 公共 | 登录 / 注册 | `/login` `/register` |
| 学生 | 首页（查件入口） | `/` |
| 学生 | 查件结果 | `/student/query` |
| 学生 | 地图导航 | `/student/map` |
| 学生 | 上报空闲度 | `/student/report` |
| 学生 | 我的包裹/历史 | `/student/records` |
| 学生 | 个人中心 | `/student/profile` |
| 站长 | 工作台（今日数据） | `/station` |
| 站长 | 包裹录入 | `/station/packages/new` |
| 站长 | 批量导入 | `/station/packages/import` |
| 站长 | 包裹列表/撤销 | `/station/packages` |
| 站长 | 取件核销 | `/station/verify` |
| 站长 | 站点信息 | `/station/info` |
| 超管 | 站点管理 | `/admin/stations` |
| 超管 | 账号审核 | `/admin/audits` |
| 超管 | 统计看板 | `/admin/dashboard` |
| 超管 | 告警中心 | `/admin/alerts` |
| 超管 | 操作日志 | `/admin/logs` |

---

## 11. 多 AI 并行分工（推荐）

> 前提：先由 **AI-0** 完成"契约落地"（schema.sql + Result/PageResult + 全局异常 + JWT 鉴权 + 实体/Mapper 骨架 + Swagger 配置 + 前端 request.js/路由骨架），随后再并行。

| AI | 负责模块 | 独占文件/目录 | 依赖 |
|---|---|---|---|
| AI-0 | 契约与基础设施 | `common/**`、`config/**`、`security/**`、`entity/**`、`mapper/**`、`db/*.sql`、`pom.xml`、`application*.yml` | 无（第一个做） |
| AI-1 | 认证与用户 M1 | `controller/AuthController`、`service/AuthService`、`dto/auth/**` | AI-0 |
| AI-2 | 站点与地图 M2/M5 | `controller/StationController`、`MapController`、`components/CampusMap.vue` | AI-0 |
| AI-3 | 包裹 M3/M4/M7 | `controller/PackageController`、`service/PackageService`、`dto/package/**` | AI-0 |
| AI-4 | 空闲度与统计 M6/M8/M9 | `controller/IdleController`、`StatsController`、`AlertController`、`task/**` | AI-0 |
| AI-5 | 前端框架与学生端 | `campus-express-web/src/**` 下除 `CampusMap.vue` 外 | AI-0 的接口契约 |
| AI-6 | 站点端与管理端页面 | `views/station/**`、`views/admin/**` | AI-5 的框架 |

**冲突规避规则**：
- `pom.xml`、`application.yml`、`schema.sql`、`Result`、`AuthInterceptor` **只允许 AI-0 修改**；其他人需要改时，输出"建议 diff"交给用户合并。
- 每个 AI 只在自己表格列出的范围内新增文件；跨模块调用先写接口再实现。
- 合并顺序：AI-0 →（AI-1 ∥ AI-2 ∥ AI-3 ∥ AI-4）→ AI-5 → AI-6。
- 前端与后端可真正并行：前端在接口未就绪时用 `src/api/mock` 里的假数据，接口就绪后切换。

---

## 12. 工作约定

- **分支**：小改动直接 `main`；大功能开 `feat/xxx` 分支，完成后合并。
- **提交信息**：`feat(模块): 说明` / `fix(模块): 说明` / `docs: 说明`。
- **注释**：公共类与方法写中文 Javadoc；复杂 SQL 注明业务意图。
- **不要**：不写死端口以外的配置；不在代码里写死 AK（走 `import.meta.env.VITE_BMAP_AK`）；不把 token 打印到日志。
- **每完成一个模块必须**：编译通过 → 起服务 → 用 curl/Swagger 打一遍接口 → 截图或贴返回结果。

---

## 13. 验收命令（每次提交前跑）

```powershell
# 0. 环境
E:\毕业设计\scripts\check-env.bat

# 1. 后端编译（约 20s，增量）
cd E:\毕业设计\campus-express
mvn -q -DskipTests package

# 2. 后端启动与健康检查
cd E:\毕业设计\campus-express
start /b java -jar target\campus-express-1.0.0.jar
curl http://localhost:8080/api/health
# 期望：{"code":200,...,"dbOk":true}

# 3. 接口文档
curl -o nul -w "%{http_code}" http://localhost:8080/v3/api-docs
# 期望：200

# 4. 前端构建
cd E:\毕业设计\campus-express-web
npm run build
# 期望：built in xxx ms，无报错

# 5. 数据库
mysql -u campus -pcampus123456 -e "use campus_express; show tables;"
```

---

## 14. 已知坑（踩过的，别再踩）

| 坑 | 现象 | 解法 |
|---|---|---|
| PowerShell 5.1 + UTF-8 无 BOM 脚本 | 中文路径变乱码、脚本写到错误目录 | 脚本用 ASCII 注释；或命令内联执行 |
| `.bat` 里写中文注释 | `'，已...' 不是内部命令` | bat 只用英文注释 |
| JDBC URL 写 `characterEncoding=utf8mb4` | `Failed to obtain JDBC Connection` | 必须写 `characterEncoding=UTF-8` |
| MySQL 8.4 认证插件 | 密码连接被拒 | URL 加 `allowPublicKeyRetrieval=true&useSSL=false` |
| 百度地图坐标系 | 标记偏移几百米 | 统一 BD-09；GPS/高德坐标先转 |
| AK 白名单 | 地图 401/白屏 | 开发期白名单 `*`，上线改域名 |
| VS Code 扩展市场 | ECONNRESET | 用 Open VSX 下 VSIX 离线安装 `code --install-extension x.vsix` |
| `E:\vendor\git-for-windows` | 报 `BUG (fork bomb)` | 用 `toolchain\PortableGit\cmd\git.exe` |
| Maven 阿里云偶发 403 | 下载失败 | 换 `dlcdn.apache.org` 或 `repo.maven.apache.org` |
| `start /b mysqld` 占住控制台 | 脚本不返回 | 用 `powershell Start-Process -WindowStyle Hidden`（已在脚本中修正） |

---

## 15. 当前阻塞项（需要用户提供）

| 阻塞 | 需要什么 | 影响 |
|---|---|---|
| 百度地图 AK | 控制台创建"浏览器端"应用（白名单 `*`）后的 32 位 AK | 地图、路线规划无法联调 |
| 站点坐标 | 5 个站点的经纬度（实地取点或地址） | 地图标记位置 |
| 导师确认 | 计划书 v2 是否通过 | 设计冻结，影响后续返工成本 |
| 指导教师姓名 | 填进计划书 | 文档完整性 |
| 是否要移动端 | 学生端是否要做 H5 适配（响应式即可，不必小程序） | 前端工作量 |

---

## 16. 变更记录

| 日期 | 变更 | 变更人 |
|---|---|---|
| 2026-11-18 | 环境搭建完成；工程骨架初始化 | DSH 会话 session-abf593c6 |
| 2026-11-18 | 计划书 v2（9 模块 / 9 表 / 47 接口） | DSH 会话 session-abf593c6 |
| 2026-11-19 | 本文档创建；MySQL 启动脚本修复 | DSH 会话 session-abf593c6 |

> 修改本文档时请在第 16 节追加一行，并说明改了哪一节、为什么改。
