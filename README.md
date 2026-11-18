# 校园快递点导航与空闲度查询平台

> 毕业设计 · 郑俊霖 · 计算机科学与技术
> 本机开发环境搭建完成：2026-11-18

---

## 一、技术栈（实际落地版本）

| 层级 | 计划书原定 | 实际安装/使用 | 说明 |
| --- | --- | --- | --- |
| 后端框架 | Spring Boot 2.x | **Spring Boot 3.5.16** | 2.x 已停止开源维护，3.5 是当前 3.x 稳定线，答辩时更站得住 |
| 运行环境 | JDK 1.8 | **JDK 21.0.13 (Temurin)** | Spring Boot 3.x 要求 JDK 17+，21 为 LTS |
| 持久层 | MyBatis-Plus | **MyBatis-Plus 3.5.18**（spring-boot3-starter） | 分页插件含 mybatis-plus-jsqlparser |
| 数据库 | MySQL 8.0 | **MySQL 8.4.4** | 本机原有免安装版，兼容 8.0 语法与驱动 |
| 前端框架 | Vue 3 + Element Plus | **Vue 3.5.45 + Element Plus 2.14.8 + Vite 8** | Node 22 LTS 下构建 |
| 地图服务 | 百度地图 JS API | 待接入 | 需本人注册 AK，见「待办」 |
| Excel | EasyExcel | **EasyExcel 4.0.3** | 快递员批量导入包裹 |
| 构建/版本 | Maven / Git | **Maven 3.9.16 / Git 2.56.1** | 阿里云镜像加速 |

其他已加入的依赖：SpringDoc OpenAPI（Swagger UI）、Hutool、JJWT（登录鉴权用）、Lombok、Spring Validation。

---

## 二、目录结构

```
E:\毕业设计\
├─ campus-express\          Spring Boot 3.5 后端（Maven 工程）
│  ├─ pom.xml
│  └─ src\main\java\com\campus\express\
│     ├─ CampusExpressApplication.java
│     ├─ common\Result.java
│     └─ controller\HealthController.java
├─ campus-express-web\      Vue3 + Vite 前端
│  ├─ package.json / vite.config.js
│  └─ src\{main.js,App.vue,router,views,api}
├─ scripts\                 启动、构建、环境自检脚本
├─ toolchain\               便携版 JDK/Maven/Node/Git（不纳入 Git）
├─ README.md                本文件
└─ .gitignore
```

---

## 三、已安装软件清单

| 软件 | 版本 | 位置 |
| --- | --- | --- |
| JDK（Temurin） | 21.0.13 LTS | `E:\毕业设计\toolchain\jdk-21.0.13` |
| Maven | 3.9.16 | `E:\毕业设计\toolchain\apache-maven-3.9.16` |
| Node.js / npm | 22.23.5 / 10.9.9 | `E:\毕业设计\toolchain\node-v22.23.5-win-x64` |
| Git | 2.56.1 | `E:\毕业设计\toolchain\PortableGit` |
| MySQL | 8.4.4（原有） | `E:\mysql`（数据目录 `E:\mysql\data`） |
| IntelliJ IDEA | Ultimate 2026.1.3（原有） | `E:\IntelliJ IDEA 2026.1.3` |
| VS Code | 1.92.2（原有） | `E:\Microsoft VS Code` |
| HeidiSQL 便携版（看数据库用，原有） | 12.10 | `E:\HeidiSQL_12.10_64_Portable` |

> 旧版 `E:\jdk-17.0.2` 保留未动，可作备用；当前 JAVA_HOME 指向 JDK 21。

### 环境变量（用户级，无需管理员）
- `JAVA_HOME` = `E:\毕业设计\toolchain\jdk-21.0.13`
- `MAVEN_HOME` / `M2_HOME` = `E:\毕业设计\toolchain\apache-maven-3.9.16`
- `NODE_HOME` = `E:\毕业设计\toolchain\node-v22.23.5-win-x64`
- `MYSQL_HOME` = `E:\mysql`
- `PATH` 已追加上述 `bin` 目录，**新开终端**即可直接使用 `java` / `mvn` / `node` / `npm` / `mysql` / `git`

### 镜像加速（已配置）
- Maven：`C:\Users\lqxhhbp\.m2\settings.xml` → 阿里云仓库（依赖已预热到 `C:\Users\lqxhhbp\.m2\repository`）
- npm：`registry=https://registry.npmmirror.com`

---

## 四、数据库

| 项目 | 值 |
| --- | --- |
| 地址 | `127.0.0.1:3306`（MySQL 8.4.4，免安装版） |
| root 账号 | `root` / `123456` |
| 应用账号 | `campus` / `campus123456`（仅授权 `campus_express` 库） |
| 业务库 | `campus_express`（utf8mb4 / utf8mb4_0900_ai_ci） |

- 原有 `room` 等数据库**未改动**。
- MySQL 已设为**登录自启动**：启动目录下的 `start-mysql.vbs` 会调用 `scripts\start-mysql.bat`（已在运行则跳过）。
- 手动启停：`scripts\start-mysql.bat` / `scripts\stop-mysql.bat`。

---

## 五、怎么跑起来

### 一键启动（推荐）
```
E:\毕业设计\scripts\start-all.bat
```
会依次：启动 MySQL → 新窗口跑后端（8080）→ 新窗口跑前端（5173）。

### 分别启动
| 操作 | 命令 / 脚本 |
| --- | --- |
| 启动 MySQL | `scripts\start-mysql.bat` |
| 跑后端 | `scripts\run-backend.bat`（等价 `cd campus-express && mvn spring-boot:run`） |
| 打包后端 | `scripts\build-backend.bat` → `campus-express\target\campus-express-1.0.0.jar` |
| 跑前端 | `scripts\run-frontend.bat`（等价 `cd campus-express-web && npm run dev`） |
| 构建前端 | `cd campus-express-web && npm run build` |
| 环境自检 | `scripts\check-env.bat` |

### 地址
- 后端健康检查：http://localhost:8080/api/health （应返回 `"dbOk": true`）
- 接口文档 Swagger：http://localhost:8080/swagger-ui.html
- 前端页面：http://localhost:5173 （`/api` 已代理到 8080）

---

## 六、IDE 使用

### IntelliJ IDEA（主力）
1. 桌面快捷方式「IntelliJ IDEA」→ `Open` → 选择 `E:\毕业设计\campus-express`（注意选后端目录，不是根目录）。
2. 等 Maven 自动导入完成；若提示 Project SDK 未设置：`File → Project Structure → SDK → Add JDK`，选 `E:\毕业设计\toolchain\jdk-21.0.13`。
3. 运行 `CampusExpressApplication` 即可（需先启动 MySQL）。

> IDEA 为 Ultimate 版，需登录 JetBrains 账号授权（学生可申请免费教育许可）。

### VS Code（可选，主要编辑 Vue）
`E:\Microsoft VS Code\Code.exe`，桌面快捷方式已创建。已装 VS Code 的 Java 扩展包：
`vscjava.vscode-java-pack`、`redhat.java`、`vscjava.vscode-maven`、`vscjava.vscode-java-debug`、
`vscjava.vscode-java-test`、`vscjava.vscode-java-dependency`、`vscjava.vscode-gradle`。

Vue 的 Volar 扩展两次安装均因扩展市场连接被重置（ECONNRESET）失败，网络稳定后执行：
```
code --install-extension Vue.volar
```

---

## 七、已验证通过（本次安装的验收结果）

| 检查项 | 结果 |
| --- | --- |
| `mvn -v` | Apache Maven 3.9.16 + Java 21.0.13 |
| 后端 `mvn clean package` | BUILD SUCCESS（首次含依赖下载 3m46s） |
| 后端启动 + `/api/health` | `{"code":200,...,"database":"8.4.4","dbOk":true}` |
| Swagger `/v3/api-docs` | HTTP 200 |
| `npm install` | 105 个包，2 分钟（npmmirror） |
| `npm run build` | 生产构建成功（618ms） |
| Vite dev server | http://localhost:5173 返回 200 |
| MySQL 连接 | `campus@localhost` 连 `campus_express` 正常 |
| `scripts\check-env.bat`（模拟新开终端） | java/mvn/node/mysql 全部可调用，mysqld is alive |
| VS Code Java 扩展 | 7 个扩展安装成功 |

---

## 八、待办事项

1. **百度地图 AK**：到 https://lbsyun.baidu.com 注册（需手机号实名），创建「浏览器端」应用，拿到 AK 后填入前端（`BMapGL` 脚本引入 + `index.html`）。这一步只能本人在线完成。
2. **需求再梳理**：老师上次的意见是「站点应按物理站点划分、三类角色、包裹要形成闭环」。下一步做：ER 图 / 表结构 / 接口清单（`campus_express` 库目前还是空库）。
3. **Git 身份**：仓库已初始化并完成首次提交，提交身份是占位值，可自行修改：
   ```
   git config user.name  "你的名字"
   git config user.email "你的邮箱"
   ```
4. 若要用 IDEA 直接运行，记得先 `scripts\start-mysql.bat`。

---

## 九、密码/端口速查

| 项目 | 值 |
| --- | --- |
| MySQL root | 123456 |
| 应用库账号 | campus / campus123456 |
| 数据库名 | campus_express |
| 后端端口 | 8080 |
| 前端端口 | 5173 |
| MySQL 端口 | 3306 |
