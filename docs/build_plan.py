"""生成《毕业设计项目计划书（修订版）》docx。

依据：原计划书 + 导师反馈（功能太少、站点应按物理站点划分、需要站点管理员角色）
+ 实地调研（校内 3 个点、校外 2 个点，多品牌混营）。
"""
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.section import WD_SECTION
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

OUT = r"E:\毕业设计\docs\毕业设计项目计划书（修订版）.docx"

BODY_FONT = "宋体"
HEAD_FONT = "黑体"
ACCENT = RGBColor(0x1F, 0x3B, 0x63)


# ---------- 工具函数 ----------
def set_run_font(run, name=BODY_FONT, size=None, bold=None, color=None):
    run.font.name = name
    run._element.rPr.rFonts.set(qn("w:eastAsia"), name)
    if size is not None:
        run.font.size = Pt(size)
    if bold is not None:
        run.font.bold = bold
    if color is not None:
        run.font.color.rgb = color


def para(doc, text="", size=10.5, bold=False, first_indent=True, space_after=4,
         align=None, color=None, font=BODY_FONT):
    p = doc.add_paragraph()
    if first_indent:
        p.paragraph_format.first_line_indent = Pt(21)
    p.paragraph_format.space_after = Pt(space_after)
    p.paragraph_format.line_spacing = 1.4
    if align is not None:
        p.alignment = align
    if text:
        run = p.add_run(text)
        set_run_font(run, font, size, bold, color)
    return p


def bullet(doc, text, size=10.5, bold_prefix=None):
    p = doc.add_paragraph(style="List Bullet")
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.line_spacing = 1.35
    if bold_prefix:
        r1 = p.add_run(bold_prefix)
        set_run_font(r1, BODY_FONT, size, True)
    r = p.add_run(text)
    set_run_font(r, BODY_FONT, size, False)
    return p


def heading(doc, text, level=1):
    h = doc.add_heading(level=level)
    run = h.add_run(text)
    size = {1: 16, 2: 13, 3: 11.5}[level]
    set_run_font(run, HEAD_FONT, size, True, ACCENT if level <= 2 else None)
    h.paragraph_format.space_before = Pt(14 if level == 1 else 10)
    h.paragraph_format.space_after = Pt(6)
    return h


def shade(cell, hex_color):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), hex_color)
    tc_pr.append(shd)


def table(doc, headers, rows, widths=None, font_size=9.5, header_fill="E8EEF7"):
    t = doc.add_table(rows=1, cols=len(headers))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    hdr = t.rows[0].cells
    for i, text in enumerate(headers):
        hdr[i].text = ""
        p = hdr[i].paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.space_before = Pt(2)
        run = p.add_run(text)
        set_run_font(run, HEAD_FONT, font_size, True)
        shade(hdr[i], header_fill)
    for row in rows:
        cells = t.add_row().cells
        for i, text in enumerate(row):
            cells[i].text = ""
            p = cells[i].paragraphs[0]
            p.paragraph_format.space_after = Pt(2)
            p.paragraph_format.space_before = Pt(2)
            p.paragraph_format.line_spacing = 1.15
            run = p.add_run(str(text))
            set_run_font(run, BODY_FONT, font_size, False)
    if widths:
        sec = doc.sections[0]
        text_width_cm = sec.page_width.cm - sec.left_margin.cm - sec.right_margin.cm
        scale = text_width_cm / sum(widths)
        for i, w in enumerate(widths):
            for row in t.rows:
                row.cells[i].width = Cm(round(w * scale, 2))
    doc.add_paragraph().paragraph_format.space_after = Pt(2)
    return t


def add_page_number_footer(section):
    footer = section.footer
    p = footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r1 = p.add_run("第 ")
    set_run_font(r1, BODY_FONT, 9)
    run = p.add_run()
    set_run_font(run, BODY_FONT, 9)
    fld_begin = OxmlElement("w:fldChar")
    fld_begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = "PAGE"
    fld_end = OxmlElement("w:fldChar")
    fld_end.set(qn("w:fldCharType"), "end")
    run._r.append(fld_begin)
    run._r.append(instr)
    run._r.append(fld_end)
    r2 = p.add_run(" 页")
    set_run_font(r2, BODY_FONT, 9)


# ---------- 文档初始化 ----------
doc = Document()

section = doc.sections[0]
section.page_width = Cm(21)
section.page_height = Cm(29.7)
section.top_margin = Cm(2.54)
section.bottom_margin = Cm(2.54)
section.left_margin = Cm(2.5)
section.right_margin = Cm(2.5)
add_page_number_footer(section)

normal = doc.styles["Normal"]
normal.font.name = BODY_FONT
normal.font.size = Pt(10.5)
normal.element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)

# ---------- 标题 ----------
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_before = Pt(30)
p.paragraph_format.space_after = Pt(6)
r = p.add_run("毕业设计项目计划书")
set_run_font(r, HEAD_FONT, 24, True, ACCENT)

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_after = Pt(14)
r = p.add_run("（第 2 版 · 按导师意见修订）")
set_run_font(r, BODY_FONT, 11, False)

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_after = Pt(4)
r = p.add_run("基于 Spring Boot 与百度地图 API 的")
set_run_font(r, HEAD_FONT, 15, True)
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.paragraph_format.space_after = Pt(20)
r = p.add_run("校园快递点导航与空闲度查询平台设计与实现")
set_run_font(r, HEAD_FONT, 15, True)

table(
    doc,
    ["项目", "内容"],
    [
        ["学生姓名", "郑俊霖"],
        ["专业方向", "计算机科学与技术"],
        ["指导教师", "（待填）"],
        ["修订日期", "2026 年 11 月 18 日"],
        ["本版修改要点", "按导师意见补充站点管理员角色；数据改为按“物理站点”划分（应对多品牌混营）；功能模块由 7 个扩展为 9 个；补充表结构、接口清单与进度计划"],
    ],
    widths=[3.6, 11.4],
    font_size=10,
)

# ---------- 一、作品名称 ----------
heading(doc, "一、作品的名称", 1)
para(doc, "基于 Spring Boot 与百度地图 API 的校园快递点导航与空闲度查询平台设计与实现", bold=True)

# ---------- 二、构思 ----------
heading(doc, "二、作品构思及整体功能描述", 1)

heading(doc, "2.1 选题背景与痛点", 2)
para(doc, "高校校园快递的“最后一公里”存在两个长期未被解决的矛盾：")
bullet(doc, "位置问题：一个学院周边往往有 5 个以上快递点，且每家只代理部分快递公司。学生收到取件短信后，经常不知道该去哪个点、在哪栋楼、走哪条路最近。")
bullet(doc, "时间问题：快递点普遍在中午 12:00—13:30、下午 17:30—19:00 形成排队高峰。学生“去了才发现排长队”，白跑一趟的情况很常见。")
para(doc, "现有的快递 App（菜鸟、丰巢等）只解决“包裹在哪个驿站”，既不提供校内步行/骑行路线，也不提供“现在挤不挤”的实时信息，更不覆盖校内自营点。本课题希望做一个面向本学院的小而完整的平台，把“包裹到站—学生查件—地图导航—到站排队—取件核销—拥挤度反馈”串成一个闭环。")

heading(doc, "2.2 实地调研结论（选题依据）", 2)
para(doc, "在确定方案前，对学院周边快递点做了一次实地走访，核心结论是：快递点不是按快递公司划分的，而是“物理站点 + 多品牌混营”。同一个站点可能同时代理 2—4 家快递，而同一家快递也可能分散在多个站点。具体调研结果如下：")
table(
    doc,
    ["编号", "站点（位置）", "代理快递公司", "备注"],
    [
        ["S1", "校内 · 邮政服务点", "中国邮政", "只收邮政，业务量小"],
        ["S2", "校内 · 申通/韵达代收点", "申通、韵达", "拼多多（PDD）模式代收"],
        ["S3", "校内 · 顺丰营业点", "顺丰", "只收顺丰，时效件为主"],
        ["S4", "校外北门 · 菜鸟驿站", "圆通、中通、申通、韵达、京东等（不收极兔、邮政）", "业务量最大，高峰排队明显"],
        ["S5", "校外岐头门 · PDD 代收点", "极兔、申通、中通、韵达", "校外，步行约 8 分钟"],
    ],
    widths=[1.3, 4.4, 5.4, 4.4],
)
para(doc, "因此，系统必须以“物理站点”为数据归属单位，站点与快递公司之间是多对多关系；学生查件时先按手机号定位到具体站点，再按站点导航。这是本次修订相对第 1 版最重要的设计变化。", bold=True)

heading(doc, "2.3 系统角色（3 类用户 + 1 类系统任务）", 2)
table(
    doc,
    ["角色", "数据范围", "核心职责"],
    [
        ["学生（普通用户）", "只读本人包裹与全校站点公开信息", "手机号查件、地图导航、查看排队指数、上报空闲度、查看取件历史"],
        ["站点管理员", "仅本站点数据（行级隔离）", "包裹录入与批量导入、撤销上架、取件核销、维护本站点营业信息、校准本站点拥挤度"],
        ["超级管理员", "全平台数据", "站点与快递品牌维护、站点管理员账号审核、全平台统计报表、超时告警处理、操作日志审计"],
        ["系统定时任务", "—", "超时未取扫描告警、拥挤度时间衰减、历史数据归档清理"],
    ],
    widths=[3.2, 5.0, 7.6],
)
para(doc, "角色差异说明：第 1 版中“快递员”与“管理员”职责重叠，且无法解释“一个站点有多家公司”的场景。本版把站点日常操作收敛到“站点管理员”（即站长/驻点人员），超级管理员只做平台治理，职责更贴近真实业务，也便于做权限隔离的答辩演示。")

heading(doc, "2.4 功能模块（9 个）", 2)
table(
    doc,
    ["编号", "模块", "功能说明"],
    [
        ["M1", "认证与权限", "注册/登录、JWT 令牌、角色鉴权拦截、站点管理员入驻申请与超管审核、账号启停"],
        ["M2", "站点与品牌管理", "站点增删改查、地图选点取经纬度、营业时间与联系电话、站点—快递公司多对多绑定、站点启用/停用"],
        ["M3", "包裹上架", "单个录入（取件码、手机号、公司、货架号）、Excel 批量导入（含模板下载、预览校验、失败行回显）、30 分钟内撤销上架"],
        ["M4", "学生查件", "按手机号或取件码查询待取包裹；手机号脱敏展示；包裹状态时间线（何时上架、何时被取）"],
        ["M5", "地图导航", "百度地图 BMapGL 打点，按拥挤度显示红/黄/绿标记；点击查看站点详情与排队指数；步行/骑行路线规划与预计耗时"],
        ["M6", "空闲度上报与聚合", "学生三档上报（空闲/一般/拥挤）；同用户同站点 10 分钟内限 1 次、每日限 5 次防刷；按时间衰减加权计算拥挤度指数（0—100）"],
        ["M7", "取件核销", "站点管理员输入取件码核销，包裹状态由“待取”转为“已取”并记录时间；重复核销、跨站点核销均拒绝"],
        ["M8", "统计与告警", "站点日/周吞吐量排名、站点管理员录入工作量、48 小时超时未取清单与标红告警、图表展示（ECharts）"],
        ["M9", "系统运维", "操作日志（AOP 切面记录）、数据导出、Swagger 接口文档、定时清理 90 天前上报明细"],
    ],
    widths=[1.2, 4.2, 10.4],
)

heading(doc, "2.5 核心业务流程（包裹闭环）", 2)
para(doc, "整个系统围绕一条主链路运转，这也是答辩演示的主线：")
steps = [
    ("① 入驻", "站点管理员注册并提交站点信息 → 超级管理员审核通过后账号生效，此时只能看到自己站点数据。"),
    ("② 上架", "快递到站后，站点管理员录入或 Excel 批量导入包裹（取件码、收件人手机号、快递公司、货架号）→ 包裹进入“待取”状态，学生端立即可查。"),
    ("③ 查件", "学生输入手机号 → 系统匹配其名下待取包裹 → 显示所在站点、货架号、取件码与上架时间。"),
    ("④ 导航", "学生在地图查看各站点红/黄/绿拥挤度与排队指数，选择站点后规划步行/骑行路线并显示预计时间。"),
    ("⑤ 取件", "到站出示取件码 → 站点管理员核销 → 状态变为“已取”并记录取件时间，同时微调该站点拥挤度。"),
    ("⑥ 反馈", "学生离场时可顺手上报“空闲/一般/拥挤”，作为拥挤度聚合的输入数据。"),
    ("⑦ 治理", "系统定时扫描超 48 小时未取的包裹生成告警；管理员在后台处理并查看统计报表。"),
]
for name, text in steps:
    bullet(doc, text, bold_prefix=name + "：")

para(doc, "包裹状态机：待取（0）→ 已取（1）；上架后 30 分钟内可撤销为“已撤销”（2）；待取超过 48 小时自动标记为超时并在后台告警，但不改变状态值，避免与撤销逻辑冲突。")

heading(doc, "2.6 本项目的技术亮点", 2)
bullet(doc, "按物理站点建模：站点与快递公司多对多，真实还原“一个站点代理多家快递”的校园现状，数据按站点做行级隔离。")
bullet(doc, "拥挤度指数算法：不是简单投票计数，而是按上报时间做指数衰减加权（近 30 分钟权重最高），并结合核销行为做负向修正。")
bullet(doc, "包裹全生命周期状态机 + 30 分钟撤销窗口，配合幂等核销与并发控制，避免重复上架、重复取件。")
bullet(doc, "Excel 批量导入：模板下载、预览校验（手机号格式、重复取件码）、失败行定位回显，解决站长批量录入的真实痛点。")
bullet(doc, "地图与业务联动：标记颜色直接取自拥挤度聚合结果，路线规划使用百度地图步行/骑行检索接口。")

# ---------- 三、实际工作内容 ----------
heading(doc, "三、本人实际工作内容与工作量", 1)
para(doc, "本项目的需求调研、数据库设计、后端接口、前端页面、数据准备与测试全部由本人独立完成，无第三方代做。按模块估算的工作量如下：")
table(
    doc,
    ["序号", "模块", "本人承担的主要技术工作", "接口数", "预估代码行"],
    [
        ["1", "认证与权限", "JWT 生成与校验、登录拦截器、角色注解鉴权、入驻审核流程", "6", "400"],
        ["2", "站点与品牌", "站点 CRUD、坐标选点回填、多对多品牌绑定、启用停用", "7", "350"],
        ["3", "包裹上架", "单个录入、EasyExcel 批量导入与校验、30 分钟撤销、事务控制", "8", "550"],
        ["4", "学生查件", "条件查询、手机号脱敏、状态时间线", "3", "200"],
        ["5", "地图导航", "BMapGL 初始化、自定义红黄绿图标、信息窗口、步行/骑行路线", "4", "650（前端）"],
        ["6", "空闲度", "上报防刷、时间衰减加权算法、定时衰减任务", "5", "450"],
        ["7", "取件核销", "状态机、幂等与并发控制、核销流水", "4", "300"],
        ["8", "统计告警", "聚合 SQL、ECharts 图表、48 小时超时扫描与告警处理", "6", "500"],
        ["9", "系统运维", "AOP 操作日志、数据导出、Swagger 分组文档", "4", "250"],
        ["—", "合计", "9 个模块、47 个 RESTful 接口", "47", "约 3650"],
    ],
    widths=[1.2, 3.0, 8.0, 1.8, 2.4],
)
para(doc, "此外还有前端 3 端共 18 个页面（学生端 7、站点端 6、管理端 5），预估前端代码 2500—3000 行；数据库脚本、模拟数据生成脚本、测试用例另计。全项目合计约 6500—7000 行有效代码。")

# ---------- 四、技术组成 ----------
heading(doc, "四、计算机技术组成、功能框架与软硬件运行环境", 1)

heading(doc, "4.1 技术选型", 2)
table(
    doc,
    ["层级", "技术选型", "版本", "说明"],
    [
        ["后端框架", "Spring Boot", "3.5.16", "RESTful 接口、IoC、拦截器与全局异常处理；3.x 为当前维护主线"],
        ["运行环境", "JDK（Temurin）", "21 LTS", "Spring Boot 3.x 要求 17+，选 21 便于长期维护"],
        ["持久层", "MyBatis-Plus", "3.5.18", "条件构造器实现多条件查件、分页与逻辑删除"],
        ["数据库", "MySQL", "8.4.4", "存储用户、站点、包裹、上报、日志等数据，utf8mb4"],
        ["前端框架", "Vue 3 + Element Plus + Vite", "3.5 / 2.14 / 8", "三端页面，组件库统一风格"],
        ["地图服务", "百度地图 JS API（GL 版）", "BMapGL 1.0", "打点、信息窗口、步行/骑行路线规划"],
        ["Excel 处理", "EasyExcel", "4.0.3", "批量导入模板解析与导出"],
        ["鉴权", "JJWT", "0.13.0", "无状态令牌，配合拦截器做角色控制"],
        ["接口文档", "SpringDoc OpenAPI", "2.10.0", "Swagger UI 在线调试，便于答辩演示"],
        ["图表", "ECharts", "6.1.0", "吞吐量趋势、站点排名、拥挤度分布"],
        ["构建 / 版本", "Maven / Git", "3.9.16 / 2.56.1", "依赖管理与代码版本控制"],
    ],
    widths=[2.4, 4.6, 2.6, 6.4],
)
para(doc, "与第 1 版计划书的差异：原计划写 Spring Boot 2.x + JDK 1.8，因 2.x 已于 2023 年停止开源维护，本版升级到 3.5.16 + JDK 21；同时补充了 JWT、Swagger、ECharts 三项，分别支撑鉴权、接口调试与统计图表。")

heading(doc, "4.2 系统功能框架", 2)
para(doc, "系统采用前后端分离的三层结构：前端（Vue3 三端页面 + 百度地图）— 后端（Spring Boot 控制器 / 业务层 / 数据访问层）— 数据层（MySQL）。后端按业务域拆分为 user、station、package、idle、stats、system 六个包，公共能力（统一响应、异常、鉴权、日志、定时任务）下沉到 common 包。")
para(doc, "功能框架自下而上分为四层：基础支撑层（认证、日志、定时任务）→ 业务能力层（站点、包裹、空闲度、核销）→ 应用服务层（查件、导航、统计、告警）→ 展示层（学生端 H5、站点端 Web、管理端 Web）。")

heading(doc, "4.3 核心数据表设计（9 张表）", 2)
table(
    doc,
    ["表名", "主要字段", "说明"],
    [
        ["sys_user", "id, phone, password, real_name, role(0学生/1站点管理员/2超管), station_id, status(0待审核/1正常/2禁用), created_at", "用户与权限"],
        ["station", "id, name, address, lng, lat, business_hours, contact_phone, status, crowd_level, crowd_score, crowd_updated_at", "快递站点，拥挤度为聚合缓存"],
        ["station_company", "id, station_id, company_code, company_name", "站点与快递公司多对多"],
        ["express_package", "id, pickup_code, receiver_phone, station_id, company_code, shelf_no, status(0待取/1已取/2已撤销), operator_id, inbound_at, taken_at", "包裹主表"],
        ["package_log", "id, package_id, action(0上架/1撤销/2核销), operator_id, created_at", "包裹状态流水"],
        ["idle_report", "id, station_id, user_id, level(0空闲/1一般/2拥挤), source(0学生/1站点校准), created_at", "空闲度上报明细"],
        ["station_audit", "id, user_id, station_id, apply_reason, status(0待审/1通过/2驳回), reviewer_id, review_time", "站点管理员入驻审核"],
        ["alert_record", "id, type(0超时未取/1异常上报/2站点停用), station_id, package_id, content, status(0未处理/1已处理), created_at", "告警记录"],
        ["operation_log", "id, user_id, module, action, detail, ip, created_at", "后台操作审计"],
    ],
    widths=[3.4, 9.0, 4.4],
    font_size=8.8,
)

heading(doc, "4.4 数据集说明", 2)
para(doc, "本系统为传统 Web 信息管理系统，不涉及神经网络训练，数据采用“真实采集 + 程序模拟”结合的方式：")
bullet(doc, "站点数据：人工实地采集 5 个快递点的名称、地址、经纬度、营业时间、代理品牌（见 2.2 节表格），真实可用。")
bullet(doc, "包裹数据：脚本生成约 400 条近 30 天记录，覆盖 5 个站点、7 家快递公司，状态分布约 70% 待取、25% 已取、5% 已撤销，其中包含 10 余条超 48 小时未取件用于演示告警。")
bullet(doc, "空闲度数据：以包裹取件时间为基准，中午与傍晚高峰时段按较高概率生成“一般/拥挤”上报，其余时段偏向“空闲”，共约 300 条，保证拥挤度曲线有真实形态。")
bullet(doc, "账号数据：超级管理员 1 个、站点管理员 5 个（对应 5 个站点）、学生 20 个，密码统一加密存储。")

heading(doc, "4.5 软硬件运行环境", 2)
table(
    doc,
    ["项目", "规格"],
    [
        ["操作系统", "Windows 11（开发机）；部署可迁移至 Linux"],
        ["开发工具", "IntelliJ IDEA 2026.1、VS Code、HeidiSQL / Navicat"],
        ["后端运行环境", "JDK 21（Temurin）、Maven 3.9.16"],
        ["数据库环境", "MySQL 8.4.4"],
        ["前端运行环境", "Node.js 22 LTS、Chrome / Edge 浏览器"],
        ["地图服务依赖", "百度地图开放平台浏览器端 AK（已实名认证，Referer 白名单）"],
        ["硬件", "CPU i5 及以上、内存 8 GB 及以上、可用磁盘 10 GB"],
    ],
    widths=[3.4, 13.4],
)

# ---------- 五、进度 ----------
heading(doc, "五、进度计划", 1)
para(doc, "按 2026 年 11 月至 2027 年 5 月安排，共 18 周，划分为 6 个阶段：")
table(
    doc,
    ["阶段", "时间", "主要任务", "交付物"],
    [
        ["一、需求与设计", "第 1—2 周（11 月下旬）", "需求梳理、ER 图与表结构、接口清单、页面原型", "设计文档、建库脚本"],
        ["二、后端核心", "第 3—6 周（12 月）", "认证鉴权、站点与品牌、包裹上架与导入、学生查件", "可运行后端 + Swagger"],
        ["三、后端进阶", "第 7—9 周（1 月）", "空闲度算法、取件核销、统计告警、定时任务", "完整接口"],
        ["四、前端三端", "第 10—12 周（2—3 月）", "学生端、站点端、管理端页面与联调", "可演示系统"],
        ["五、测试与数据", "第 13—14 周（4 月上旬）", "功能测试、模拟数据、部署与演示脚本", "测试报告、演示数据"],
        ["六、论文与答辩", "第 15—18 周（4 月中—5 月）", "论文撰写与查重修改、答辩 PPT 与演练", "论文、PPT、演示视频"],
    ],
    widths=[3.2, 4.4, 6.6, 4.0],
    font_size=9.2,
)

# ---------- 六、风险 ----------
heading(doc, "六、风险与应对", 1)
table(
    doc,
    ["风险", "影响", "应对措施"],
    [
        ["百度地图 AK 配额或白名单配置错误", "地图与路线规划不可用", "开发阶段白名单设 *，上线前改为具体域名；接口调用加缓存，避免重复请求"],
        ["取件码/手机号涉及隐私", "数据泄露风险", "查询接口手机号脱敏、按手机号仅返回本人待取件；日志不记录完整手机号"],
        ["并发核销导致重复取件", "数据不一致", "核销使用条件更新（status=0 才更新），配合唯一流水校验"],
        ["真实快递点坐标采集不准", "地图标记偏移", "现场用手机地图取点并二次核对，坐标统一存 WGS84 转百度坐标"],
        ["时间不足导致功能缩水", "工作量不达标", "按模块优先级推进，P0 为查件/地图/核销，P1 为统计与告警，保证主线完整"],
    ],
    widths=[4.6, 4.0, 8.2],
    font_size=9.2,
)

# ---------- 七、预期成果 ----------
heading(doc, "七、预期成果", 1)
bullet(doc, "一套可运行的校园快递点导航与空闲度查询平台（后端 + 学生端 + 站点端 + 管理端），支持 5 个站点、3 类用户角色、47 个接口。")
bullet(doc, "完整的数据库脚本与模拟数据生成脚本，可在任意机器上 5 分钟内重建演示环境。")
bullet(doc, "《毕业设计说明书》一份（约 1.5—2 万字），包含需求分析、系统设计、数据库设计、关键算法、测试与总结。")
bullet(doc, "答辩 PPT 与 3—5 分钟系统演示视频，覆盖“查件—导航—取件核销—上报”完整闭环。")

# ---------- 八、学生信息 ----------
heading(doc, "八、学生信息", 1)
table(
    doc,
    ["项目", "内容"],
    [
        ["专业方向", "计算机科学与技术"],
        ["学生姓名", "郑俊霖"],
        ["联系电话", "13560620383 / 19925721119"],
    ],
    widths=[3.6, 11.4],
    font_size=10,
)

doc.save(OUT)
print("saved", OUT)
print("paragraphs", len(doc.paragraphs), "tables", len(doc.tables))
