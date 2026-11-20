# 校园快递点导航与空闲度查询平台

基于 Spring Boot 3、Vue 3 和百度地图 GL API 的毕业设计项目，面向校园快递场景打通“包裹到站、学生查件、地图导航、到场取件、拥挤度反馈、统计分析”的完整业务闭环。

## 当前状态

- P1-P3：数据库、后端核心业务、鉴权、包裹、空闲度、核销、统计、审核、告警和定时任务已完成。
- P4：学生端、站长端和超管端共 19 个页面路由已完成，三角色端到端验收通过。
- P5：浏览器端步行/驾车路线、地址解析、地图选点及演示脚本已完成。
- 下一步：论文正文、架构图、测试章节、答辩 PPT 和演示视频。

项目协作与实现约束见 [AGENTS.md](AGENTS.md)，阶段记录见 [docs/logs](docs/logs/README.md)。

## 主要功能

### 学生端

- 手机号注册、登录和个人中心。
- 手机号或取件码查询待取包裹。
- 查看本人全部、待取、已取和已撤销包裹。
- 在百度地图查看 5 个真实快递站点和拥挤度。
- 查看步行/驾车路线、距离和预计时间。
- 提交站点空闲、一般或拥挤反馈。

### 站长端

- 今日包裹统计、近 7 天趋势和拥挤度校准。
- 单件包裹上架和 Excel 批量导入。
- 包裹筛选、分页、详情、撤销和导出。
- 取件码查询与核销、已取列表和超时列表。
- 站点地址、营业时间、联系电话和地图坐标维护。

### 超管端

- 站点新增、编辑、停用和代理快递公司维护。
- 站点吞吐、操作员工作量和拥挤度统计。
- 站长入驻审核、告警处理和操作日志查询。
- 地址解析、地图选点和统计时间范围切换。

## 技术栈

| 层级 | 技术 |
|---|---|
| 后端 | Spring Boot 3.5.16、JDK 21、MyBatis-Plus 3.5.18 |
| 数据库 | MySQL 8.4.4 |
| 鉴权 | JJWT 0.13.0、JWT 拦截器 |
| Excel | EasyExcel 4.0.3 |
| 接口文档 | SpringDoc OpenAPI 2.10.0 |
| 前端 | Vue 3.5、Vite 8、Element Plus 2.14 |
| 状态与路由 | Pinia 4、Vue Router 4 |
| 图表 | ECharts 6 |
| 地图 | 百度地图 JS API GL（BMapGL） |

## 目录结构

```text
.
├─ campus-express/          Spring Boot 后端
├─ campus-express-web/      Vue 3 前端
├─ docs/                    计划书、验收报告、演示脚本和过程日志
├─ scripts/                 环境检查与启停脚本
├─ AGENTS.md                项目权威说明与接口契约
└─ README.md
```

`toolchain/`、`node_modules/`、`target/`、`dist/`、运行日志和本地环境文件不纳入 Git。

## 本地运行

### 1. 环境要求

- JDK 21
- Maven 3.9+
- Node.js 22 / npm 10
- MySQL 8.4

### 2. 初始化数据库

依次执行：

```text
campus-express/src/main/resources/db/schema.sql
campus-express/src/main/resources/db/seed.sql
campus-express/src/main/resources/db/seed_p3_demo.sql
```

数据库连接信息通过本地配置或环境变量提供，请勿把真实密码提交到仓库。

### 3. 配置地图 AK

复制前端环境变量模板：

```powershell
Copy-Item campus-express-web/.env.example campus-express-web/.env.local
```

在 `.env.local` 中填写百度地图浏览器端 AK。该文件已被 Git 忽略。

### 4. 启动服务

```powershell
.\scripts\check-env.bat
.\scripts\start-all.bat
```

- 后端：http://localhost:8080
- 健康检查：http://localhost:8080/api/health
- Swagger：http://localhost:8080/swagger-ui.html
- 前端：http://127.0.0.1:5173

## 构建与验收

```powershell
cd campus-express
mvn test
mvn -DskipTests package

cd ..\campus-express-web
npm run build
```

当前验收结果：

- 后端单元测试：11 个，全部通过。
- Swagger：48 个业务接口加健康检查。
- 前端 19 个页面路由均已实现。
- 三角色登录、权限隔离和主要业务流程通过浏览器回归。

## 地图说明

当前百度 AK 支持浏览器端 BMapGL，可完成地图展示、步行/驾车路线和地理编码。百度 Web 服务接口返回状态码 `240：APP 服务被禁用`，因此运行架构采用前端直连 BMapGL。

## 相关文档

- [P4 验收报告](docs/P4验收报告.md)
- [P5 演示脚本](docs/P5演示脚本.md)
- [开发过程日志](docs/logs/README.md)
