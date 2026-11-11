-- 演示数据：7 个内置角色、7 个账号、10 条需求与完整交付链路
INSERT INTO sys_dept (id, name, parent_id, sort, created_at) VALUES
(1, '信息科技部', 0, 1, '2026-10-01 09:00:00'),
(2, '零售金融部', 0, 2, '2026-10-01 09:00:00'),
(3, '公司金融部', 0, 3, '2026-10-01 09:00:00'),
(4, '风险管理部', 0, 4, '2026-10-01 09:00:00');

INSERT INTO sys_role (id, code, name, data_scope, builtin, created_at) VALUES
(1, 'ADMIN', '系统管理员', 'ALL', 1, '2026-10-01 09:00:00'),
(2, 'BUSINESS', '需求提出人', 'SELF', 1, '2026-10-01 09:00:00'),
(3, 'RA', '需求分析', 'DEPT', 1, '2026-10-01 09:00:00'),
(4, 'PM', '项目经理', 'PROJECT', 1, '2026-10-01 09:00:00'),
(5, 'DEV', '开发工程师', 'PROJECT', 1, '2026-10-01 09:00:00'),
(6, 'QA', '测试工程师', 'PROJECT', 1, '2026-10-01 09:00:00'),
(7, 'ACCEPT', '交付验收人', 'PROJECT', 1, '2026-10-01 09:00:00');

INSERT INTO sys_permission (id, code, name, module) VALUES
(1, 'requirement:view', '查看需求', '需求管理'),
(2, 'requirement:create', '新建需求', '需求管理'),
(3, 'requirement:edit', '编辑需求', '需求管理'),
(4, 'requirement:delete', '删除需求', '需求管理'),
(5, 'requirement:action', '需求流转', '需求管理'),
(6, 'review:view', '查看评审', '评审管理'),
(7, 'review:create', '发起评审', '评审管理'),
(8, 'review:opinion', '提交评审意见', '评审管理'),
(9, 'review:decide', '提交评审结论', '评审管理'),
(10, 'change:view', '查看变更', '变更管理'),
(11, 'change:create', '申请变更', '变更管理'),
(12, 'change:approve', '审批变更', '变更管理'),
(13, 'change:apply', '实施变更', '变更管理'),
(14, 'project:view', '查看项目', '计划任务'),
(15, 'project:manage', '管理项目', '计划任务'),
(16, 'task:view', '查看任务', '计划任务'),
(17, 'task:manage', '管理任务', '计划任务'),
(18, 'delivery:view', '查看交付', '交付验收'),
(19, 'delivery:manage', '登记交付物', '交付验收'),
(20, 'delivery:accept', '验收与归档', '交付验收'),
(21, 'report:view', '查看报表', '报表看板'),
(22, 'admin:user', '用户管理', '系统管理'),
(23, 'admin:role', '角色管理', '系统管理'),
(24, 'admin:audit', '审计日志', '系统管理');

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),(1,11),(1,12),(1,13),
(1,14),(1,15),(1,16),(1,17),(1,18),(1,19),(1,20),(1,21),(1,22),(1,23),(1,24),
(2,1),(2,2),(2,3),(2,4),(2,5),(2,6),(2,10),(2,11),(2,16),(2,18),
(3,1),(3,2),(3,3),(3,5),(3,6),(3,7),(3,8),(3,9),(3,10),(3,11),(3,12),(3,13),
(3,14),(3,16),(3,18),(3,19),(3,21),
(4,1),(4,3),(4,5),(4,6),(4,7),(4,8),(4,9),(4,10),(4,12),(4,13),(4,14),(4,15),(4,16),(4,17),(4,18),(4,19),(4,21),
(5,1),(5,5),(5,6),(5,8),(5,14),(5,16),(5,17),(5,18),
(6,1),(6,5),(6,6),(6,8),(6,14),(6,16),(6,17),(6,18),
(7,1),(7,5),(7,6),(7,18),(7,20),(7,21);

-- 所有演示账号密码：Reqflow@123
INSERT INTO sys_user (id, username, password, real_name, dept_id, email, phone, status, must_change_pwd, fail_count, created_at, updated_at, deleted) VALUES
(1, 'admin', '$2a$10$UT0rx/rmA4oFIeMaEoTqcee1pzeVOd47/IK2NYU9JjqKv2rpUOp92', '系统管理员', 1, 'admin@reqflow.cn', '13800000001', 1, 0, 0, '2026-10-01 09:00:00', '2026-10-01 09:00:00', 0),
(2, 'biz', '$2a$10$UT0rx/rmA4oFIeMaEoTqcee1pzeVOd47/IK2NYU9JjqKv2rpUOp92', '王小明', 2, 'biz@reqflow.cn', '13800000002', 1, 0, 0, '2026-10-01 09:00:00', '2026-10-01 09:00:00', 0),
(3, 'ra', '$2a$10$UT0rx/rmA4oFIeMaEoTqcee1pzeVOd47/IK2NYU9JjqKv2rpUOp92', '李静', 1, 'ra@reqflow.cn', '13800000003', 1, 0, 0, '2026-10-01 09:00:00', '2026-10-01 09:00:00', 0),
(4, 'pm', '$2a$10$UT0rx/rmA4oFIeMaEoTqcee1pzeVOd47/IK2NYU9JjqKv2rpUOp92', '张伟', 1, 'pm@reqflow.cn', '13800000004', 1, 0, 0, '2026-10-01 09:00:00', '2026-10-01 09:00:00', 0),
(5, 'dev', '$2a$10$UT0rx/rmA4oFIeMaEoTqcee1pzeVOd47/IK2NYU9JjqKv2rpUOp92', '陈晨', 1, 'dev@reqflow.cn', '13800000005', 1, 0, 0, '2026-10-01 09:00:00', '2026-10-01 09:00:00', 0),
(6, 'qa', '$2a$10$UT0rx/rmA4oFIeMaEoTqcee1pzeVOd47/IK2NYU9JjqKv2rpUOp92', '刘洋', 1, 'qa@reqflow.cn', '13800000006', 1, 0, 0, '2026-10-01 09:00:00', '2026-10-01 09:00:00', 0),
(7, 'accept', '$2a$10$UT0rx/rmA4oFIeMaEoTqcee1pzeVOd47/IK2NYU9JjqKv2rpUOp92', '赵敏', 4, 'accept@reqflow.cn', '13800000007', 1, 0, 0, '2026-10-01 09:00:00', '2026-10-01 09:00:00', 0);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1,1),(2,2),(3,3),(4,4),(5,5),(6,6),(7,7);

INSERT INTO sys_sequence (seq_key, seq_date, seq_value) VALUES
('REQ-202611', '202611', 10),
('RV-202611', '202611', 3),
('CHG-202611', '202611', 2),
('TSK-202611', '202611', 11),
('DLV-202611', '202611', 6),
('ACC-202611', '202611', 2),
('PRJ-202611', '202611', 2);

INSERT INTO project (id, project_no, name, dept_id, pm_id, status, start_date, end_date, created_at, deleted) VALUES
(1, 'PRJ-202611-0001', '手机银行 2026 四季度迭代', 2, 4, 'IN_PROGRESS', '2026-10-01', '2026-12-31', '2026-10-01 09:30:00', 0),
(2, 'PRJ-202611-0002', '对公客户服务能力提升', 3, 4, 'PLANNING', '2026-11-20', '2027-02-28', '2026-10-20 09:30:00', 0);

INSERT INTO milestone (id, project_id, name, plan_date, actual_date, status) VALUES
(1, 1, '需求分析与设计', '2026-11-15', '2026-11-10', 'DONE'),
(2, 1, '开发与联调', '2026-12-10', NULL, 'IN_PROGRESS'),
(3, 1, '验收上线', '2026-12-25', NULL, 'TODO'),
(4, 2, '需求澄清', '2026-11-30', NULL, 'TODO');

INSERT INTO requirement (id, req_no, title, req_type, req_source, dept_id, proposer_id, analyst_id, pm_id, priority,
                         expect_date, plan_date, business_background, req_description, acceptance_criteria, status,
                         hold_from_status, version_no, change_count, project_id, milestone_id, archived_at, archived_by,
                         reject_reason, created_at, updated_at, created_by, updated_by, deleted) VALUES
(1, 'REQ-202611-0001', '手机银行转账限额支持按客户等级差异化配置', 'NEW_FEATURE', 'BUSINESS_DEPT', 2, 2, 3, 4, 'P0',
 '2026-12-20', NULL,
 '当前手机银行转账限额为全行统一配置，高净值客户与普通客户使用同一额度，客户体验与风控精细化管理均受影响。',
 '支持按客户等级（私行、财富、白金、普通）配置单笔与日累计限额；支持管理端配置与生效时间；额度变更需记录操作日志。',
 '1. 四类客户等级可分别配置单笔、日累计限额；2. 配置 5 分钟内生效；3. 超限交易返回明确提示；4. 变更操作可在审计日志中追溯。',
 'REVIEW_PASSED', NULL, 'V1.0', 0, NULL, NULL, NULL, NULL, NULL,
 '2026-11-01 09:10:00', '2026-11-07 16:20:00', 2, 3, 0),
(2, 'REQ-202611-0002', '对公账户开户资料线上预审', 'OPTIMIZE', 'BUSINESS_DEPT', 3, 2, 3, NULL, 'P1',
 '2027-01-15', NULL,
 '对公客户开户需柜面提交纸质资料，资料不全导致往返，开户周期平均 3 个工作日。',
 '提供开户资料在线上传与预审能力，支持资料清单校验、预审意见反馈与预约网点办理。',
 '1. 支持 8 类开户资料在线上传；2. 预审意见 1 个工作日内反馈；3. 资料缺失项逐项提示；4. 预审通过后可预约网点。',
 'ANALYZING', NULL, 'V0.1', 0, NULL, NULL, NULL, NULL, NULL,
 '2026-11-03 10:20:00', '2026-11-08 11:05:00', 2, 3, 0),
(3, 'REQ-202611-0003', '反洗钱可疑交易监测规则升级', 'COMPLIANCE', 'REGULATION', 4, 2, 3, 4, 'P0',
 '2026-12-05', '2026-12-08',
 '监管新规要求补充可疑交易识别维度，现有规则仅覆盖大额与高频交易，存在合规缺口。',
 '新增跨境交易、夜间高频、分散转入集中转出三类规则；支持规则参数配置与命中记录查询。',
 '1. 三类新规则全部上线；2. 规则命中记录可查询与导出；3. 规则参数调整无需发版；4. 监管报送字段完整。',
 'DEVELOPING', NULL, 'V1.1', 1, 1, 2, NULL, NULL, NULL,
 '2026-10-28 14:00:00', '2026-11-09 17:30:00', 2, 3, 0),
(4, 'REQ-202611-0004', '手机银行 App 启动速度优化', 'OPTIMIZE', 'PRODUCTION', 2, 2, 3, 4, 'P1',
 '2026-12-15', '2026-12-12',
 '客户反馈 App 冷启动时间长，低端机型平均 4.2 秒，影响登录转化率。',
 '优化启动链路，减少首页同步请求，图片与脚本懒加载，目标冷启动时间降至 2 秒以内。',
 '1. 主流机型冷启动 P90 小于 2 秒；2. 启动失败率不高于 0.1%；3. 首页核心内容首屏可见时间小于 1.5 秒。',
 'TESTING', NULL, 'V1.0', 0, 1, 2, NULL, NULL, NULL,
 '2026-10-30 09:40:00', '2026-11-10 10:15:00', 2, 3, 0),
(5, 'REQ-202611-0005', '网点柜面系统适配国产浏览器', 'COMPLIANCE', 'INTERNAL', 4, 7, 3, 4, 'P2',
 '2026-11-30', '2026-11-28',
 '总行信创要求柜面系统在国产浏览器上可用，当前部分控件与样式不兼容。',
 '完成柜面系统在奇安信可信浏览器与 360 安全浏览器上的兼容改造，覆盖开户、转账、查询三类高频交易。',
 '1. 三类高频交易在两种国产浏览器可正常办理；2. 页面无错位与脚本报错；3. 打印控件可正常调用；4. 形成兼容性验证清单。',
 'PENDING_ACCEPTANCE', NULL, 'V1.0', 0, 1, 3, NULL, NULL, NULL,
 '2026-10-15 09:00:00', '2026-11-10 16:40:00', 7, 3, 0),
(6, 'REQ-202611-0006', '个人贷款申请进度短信提醒', 'NEW_FEATURE', 'BUSINESS_DEPT', 2, 2, 3, 4, 'P2',
 '2026-10-20', '2026-10-18',
 '客户提交贷款申请后无法及时获知审批进度，客服咨询量高。',
 '在贷款申请受理、审批通过、放款三个节点发送短信提醒，支持模板配置与发送记录查询。',
 '1. 三个节点均触发短信；2. 短信 5 分钟内送达；3. 发送失败可重试；4. 发送记录可查询。',
 'ARCHIVED', NULL, 'V1.1', 1, 1, 1, '2026-10-25 15:30:00', 3, NULL,
 '2026-09-20 10:00:00', '2026-10-25 15:30:00', 2, 3, 0),
(7, 'REQ-202611-0007', '信用卡账单分期手续费展示优化', 'OPTIMIZE', 'BUSINESS_DEPT', 2, 2, NULL, NULL, 'P2',
 '2027-01-31', NULL,
 '客户反馈分期手续费展示不直观，需要明确每期手续费与折算年化利率。',
 '账单分期页展示每期手续费、总手续费与折算年化利率，并在确认页二次提示。',
 '1. 展示每期与总手续费；2. 展示折算年化利率；3. 确认页二次提示；4. 与核心系统计算结果一致。',
 'DRAFT', NULL, 'V0.1', 0, NULL, NULL, NULL, NULL, NULL,
 '2026-11-10 09:20:00', '2026-11-10 09:20:00', 2, 2, 0),
(8, 'REQ-202611-0008', '客户风险评级数据报表', 'REPORT', 'BUSINESS_DEPT', 4, 7, 3, 4, 'P1',
 '2027-01-10', NULL,
 '风险条线每月需手工汇总客户评级分布，耗时且口径不统一。',
 '提供按机构、客户等级、评级结果维度的分布报表，支持按月导出。',
 '1. 三个维度可组合筛选；2. 数据与核心系统 T+1 一致；3. 支持 Excel 导出；4. 报表访问受数据权限控制。',
 'PENDING_REVIEW', NULL, 'V0.1', 0, NULL, NULL, NULL, NULL, NULL,
 '2026-11-04 15:10:00', '2026-11-09 09:30:00', 7, 3, 0),
(9, 'REQ-202611-0009', '手机银行指纹登录兼容性修复', 'BUG_FIX', 'PRODUCTION', 2, 2, 3, NULL, 'P1',
 '2026-11-25', NULL,
 '部分安卓机型升级系统后指纹登录失败，客户需改用密码登录。',
 '修复指纹登录在 Android 14 部分机型上的兼容性问题。',
 '修复后相关机型可正常登录。',
 'REJECTED', NULL, 'V0.2', 0, NULL, NULL, NULL, NULL, '验收标准不清晰，缺少受影响机型清单与回归范围',
 '2026-11-02 11:00:00', '2026-11-06 10:00:00', 2, 3, 0),
(10, 'REQ-202611-0010', '网点排队叫号数据大屏', 'REPORT', 'BUSINESS_DEPT', 2, 2, 3, NULL, 'P3',
 '2027-02-28', NULL,
 '网点希望实时展示排队与叫号情况，提升厅堂服务体验。',
 '提供网点叫号数据大屏，展示等待人数、平均等待时长与窗口状态。',
 '1. 数据延迟小于 30 秒；2. 支持网点切换；3. 大屏分辨率适配。',
 'ON_HOLD', 'ANALYZING', 'V0.1', 0, NULL, NULL, NULL, NULL, NULL,
 '2026-11-05 14:20:00', '2026-11-07 09:00:00', 2, 3, 0);

INSERT INTO requirement_version (id, requirement_id, version, reason, snapshot_json, operator_id, created_at) VALUES
(1, 1, 'V1.0', '评审通过',
 '{"title":"手机银行转账限额支持按客户等级差异化配置","priority":"P0","version":"V1.0"}', 3, '2026-11-07 16:20:00'),
(2, 3, 'V1.0', '评审通过',
 '{"title":"反洗钱可疑交易监测规则升级","priority":"P0","version":"V1.0"}', 3, '2026-10-31 10:00:00'),
(3, 3, 'V1.1', '变更生效：新增跨境交易识别维度',
 '{"title":"反洗钱可疑交易监测规则升级","priority":"P0","version":"V1.1"}', 3, '2026-11-06 14:30:00'),
(4, 6, 'V1.0', '评审通过',
 '{"title":"个人贷款申请进度短信提醒","priority":"P2","version":"V1.0"}', 3, '2026-09-28 10:00:00'),
(5, 6, 'V1.1', '变更生效：增加放款节点提醒',
 '{"title":"个人贷款申请进度短信提醒","priority":"P2","version":"V1.1"}', 3, '2026-10-08 09:30:00'),
(6, 9, 'V0.2', '评审驳回后修改',
 '{"title":"手机银行指纹登录兼容性修复","priority":"P1","version":"V0.2"}', 3, '2026-11-06 10:00:00');

INSERT INTO review (id, review_no, requirement_id, review_type, review_method, host_id, reviewer_ids, deadline,
                    status, conclusion, comment_text, snapshot_json, created_at, closed_at) VALUES
(1, 'RV-202611-0001', 8, 'REQUIREMENT', 'MEETING', 3, ',4,6,', '2026-11-15 18:00:00',
 'PENDING', NULL, NULL, '{"title":"客户风险评级数据报表","version":"V0.1"}', '2026-11-09 09:30:00', NULL),
(2, 'RV-202611-0002', 1, 'REQUIREMENT', 'MEETING', 3, ',4,6,', '2026-11-07 18:00:00',
 'PASSED', 'PASS', '评审通过，需补充风控校验规则的测试场景', '{"title":"手机银行转账限额支持按客户等级差异化配置","version":"V0.1"}',
 '2026-11-05 09:00:00', '2026-11-07 16:20:00'),
(3, 'RV-202611-0003', 9, 'REQUIREMENT', 'MEETING', 3, ',4,6,', '2026-11-06 18:00:00',
 'REJECTED', 'REJECT', '验收标准无法验证，缺少受影响机型清单', '{"title":"手机银行指纹登录兼容性修复","version":"V0.1"}',
 '2026-11-04 09:00:00', '2026-11-06 10:00:00');

INSERT INTO review_opinion (id, review_id, reviewer_id, result, content, created_at) VALUES
(1, 2, 4, 'PASS', '接口改造量可控，建议纳入本迭代', '2026-11-06 14:00:00'),
(2, 2, 6, 'NEED_CHANGE', '需补充超额交易的异常场景测试用例', '2026-11-06 15:00:00'),
(3, 3, 4, 'OBJECT', '影响范围描述不完整，无法评估工作量', '2026-11-05 10:20:00'),
(4, 3, 6, 'OBJECT', '缺少受影响机型与回归范围', '2026-11-05 11:00:00');

INSERT INTO change_request (id, change_no, requirement_id, reason, content, impact_hours, impact_desc, status,
                            applicant_id, approver_id, approve_comment, approved_at, applied_at, created_at) VALUES
(1, 'CHG-202611-0001', 3, '监管新增可疑交易识别维度',
 '新增跨境交易、夜间高频两类识别规则，调整规则参数配置方式。', 16.0, '影响 2 个开发任务、1 张规则配置表，里程碑日期不变',
 'APPLIED', 3, 4, '影响可控，同意实施', '2026-11-05 17:00:00', '2026-11-06 14:30:00', '2026-11-04 10:00:00'),
(2, 'CHG-202611-0002', 4, '性能指标调整',
 '冷启动目标从 2.5 秒收紧到 2 秒，增加低端机型专项测试。', 8.0, '影响 1 个测试任务，验收日期不变',
 'PENDING', 3, NULL, NULL, NULL, NULL, '2026-11-10 09:00:00');

INSERT INTO task (id, task_no, requirement_id, project_id, milestone_id, parent_id, name, task_type, assignee_id,
                  estimate_hours, actual_hours, plan_date, status, block_reason, finish_note, finished_at,
                  created_at, updated_at, deleted) VALUES
(1, 'TSK-202611-0001', 3, 1, 2, NULL, '可疑交易规则引擎接口开发', 'DEV', 5,
 24.0, 22.0, '2026-11-20', 'DONE', NULL, '完成三类规则接口与单元测试', '2026-11-08 18:00:00',
 '2026-11-01 09:00:00', '2026-11-08 18:00:00', 0),
(2, 'TSK-202611-0002', 3, 1, 2, NULL, '规则配置管理页面开发', 'DEV', 5,
 16.0, 10.0, '2026-11-25', 'IN_PROGRESS', NULL, NULL, NULL,
 '2026-11-01 09:05:00', '2026-11-09 17:30:00', 0),
(3, 'TSK-202611-0003', 3, 1, 2, NULL, '规则回归测试', 'TEST', 6,
 12.0, NULL, '2026-11-30', 'TODO', NULL, NULL, NULL,
 '2026-11-01 09:10:00', '2026-11-01 09:10:00', 0),
(4, 'TSK-202611-0004', 4, 1, 2, NULL, '启动链路性能分析', 'ANALYSIS', 5,
 8.0, 8.0, '2026-11-05', 'DONE', NULL, '输出启动耗时分段报告', '2026-11-04 17:00:00',
 '2026-11-01 10:00:00', '2026-11-04 17:00:00', 0),
(5, 'TSK-202611-0005', 4, 1, 2, NULL, '首页资源懒加载改造', 'DEV', 5,
 20.0, 19.0, '2026-11-18', 'DONE', NULL, '首屏同步请求由 9 个降到 3 个', '2026-11-09 18:30:00',
 '2026-11-01 10:05:00', '2026-11-09 18:30:00', 0),
(6, 'TSK-202611-0006', 4, 1, 2, NULL, '启动耗时分段测试', 'TEST', 6,
 8.0, 7.5, '2026-11-25', 'DONE', NULL, 'P90 1.8 秒，达标', '2026-11-10 10:00:00',
 '2026-11-01 10:10:00', '2026-11-10 10:00:00', 0),
(7, 'TSK-202611-0007', 4, 1, 2, NULL, '低端机型兼容性回归', 'TEST', 6,
 10.0, 4.0, '2026-11-28', 'IN_PROGRESS', NULL, NULL, NULL,
 '2026-11-01 10:15:00', '2026-11-10 10:15:00', 0),
(8, 'TSK-202611-0008', 5, 1, 3, NULL, '国产浏览器兼容改造', 'DEV', 5,
 12.0, 12.0, '2026-11-10', 'DONE', NULL, '完成样式与打印控件适配', '2026-11-08 17:00:00',
 '2026-10-20 09:00:00', '2026-11-08 17:00:00', 0),
(9, 'TSK-202611-0009', 5, 1, 3, NULL, '柜面高频交易回归测试', 'TEST', 6,
 8.0, 8.0, '2026-11-12', 'DONE', NULL, '三类交易在两种浏览器全部通过', '2026-11-10 16:00:00',
 '2026-10-20 09:05:00', '2026-11-10 16:00:00', 0),
(10, 'TSK-202611-0010', 6, 1, 1, NULL, '短信模板配置与发送服务', 'DEV', 5,
 10.0, 10.0, '2026-10-10', 'DONE', NULL, '三个节点模板配置完成', '2026-10-09 17:00:00',
 '2026-09-25 09:00:00', '2026-10-09 17:00:00', 0),
(11, 'TSK-202611-0011', 6, 1, 1, NULL, '短信发送联调与验证', 'TEST', 6,
 6.0, 6.0, '2026-10-15', 'DONE', NULL, '发送成功率 99.9%', '2026-10-14 17:00:00',
 '2026-09-25 09:05:00', '2026-10-14 17:00:00', 0);

INSERT INTO deliverable (id, deliverable_no, requirement_id, deliverable_type, name, file_name, stored_name,
                         file_size, description, uploader_id, created_at, deleted) VALUES
(1, 'DLV-202611-0001', 5, 'SPEC', '柜面系统国产浏览器适配需求规格说明书', '柜面系统国产浏览器适配需求规格说明书.docx', NULL, 245760, '含兼容范围与验收标准', 3, '2026-11-02 10:00:00', 0),
(2, 'DLV-202611-0002', 5, 'TEST_REPORT', '国产浏览器兼容性测试报告', '国产浏览器兼容性测试报告.pdf', NULL, 524288, '覆盖两类浏览器与三类交易', 6, '2026-11-10 16:10:00', 0),
(3, 'DLV-202611-0003', 5, 'DEPLOY_DOC', '柜面系统适配版本部署说明', '柜面系统适配版本部署说明.pdf', NULL, 131072, '含回退步骤', 5, '2026-11-10 16:20:00', 0),
(4, 'DLV-202611-0004', 6, 'SPEC', '贷款进度短信提醒需求规格说明书', '贷款进度短信提醒需求规格说明书.docx', NULL, 198656, '含短信模板', 3, '2026-09-28 10:00:00', 0),
(5, 'DLV-202611-0005', 6, 'TEST_REPORT', '短信提醒验收测试报告', '短信提醒验收测试报告.pdf', NULL, 307200, '三节点全部验证通过', 6, '2026-10-16 16:00:00', 0),
(6, 'DLV-202611-0006', 4, 'TEST_REPORT', '启动性能分段测试报告', '启动性能分段测试报告.xlsx', NULL, 153600, '含各机型 P90 数据', 6, '2026-11-10 10:20:00', 0);

INSERT INTO acceptance (id, acceptance_no, requirement_id, round_no, submitter_id, acceptor_id, deliverable_snapshot,
                        status, conclusion, comment_text, rework_requirement, submitted_at, confirmed_at) VALUES
(1, 'ACC-202611-0001', 5, 1, 3, 7,
 '柜面系统国产浏览器适配需求规格说明书；国产浏览器兼容性测试报告；柜面系统适配版本部署说明',
 'PENDING', NULL, NULL, NULL, '2026-11-10 16:40:00', NULL),
(2, 'ACC-202611-0002', 6, 1, 3, 7,
 '贷款进度短信提醒需求规格说明书；短信提醒验收测试报告',
 'PASSED', 'PASS', '三个节点短信均正常送达，同意验收', NULL, '2026-10-20 09:00:00', '2026-10-22 15:00:00');

INSERT INTO audit_log (id, user_id, username, role_codes, ip, uri, method, module, action, object_type, object_id,
                       result, error_msg, duration_ms, trace_id, created_at) VALUES
(1, 2, 'biz', 'BUSINESS', '127.0.0.1', '/api/requirements', 'POST', '需求管理', '新建需求', 'REQUIREMENT', '1', 'SUCCESS', NULL, 36, 'seed-0001', '2026-11-01 09:10:00'),
(2, 2, 'biz', 'BUSINESS', '127.0.0.1', '/api/requirements/1/action', 'POST', '需求管理', '提交需求', 'REQUIREMENT', '1', 'SUCCESS', NULL, 18, 'seed-0002', '2026-11-01 09:30:00'),
(3, 3, 'ra', 'RA', '127.0.0.1', '/api/requirements/1/action', 'POST', '需求管理', '受理需求', 'REQUIREMENT', '1', 'SUCCESS', NULL, 22, 'seed-0003', '2026-11-02 09:00:00'),
(4, 3, 'ra', 'RA', '127.0.0.1', '/api/reviews', 'POST', '评审管理', '发起评审', 'REQUIREMENT', '1', 'SUCCESS', NULL, 41, 'seed-0004', '2026-11-05 09:00:00'),
(5, 3, 'ra', 'RA', '127.0.0.1', '/api/reviews/2/conclusion', 'POST', '评审管理', '评审通过', 'REQUIREMENT', '1', 'SUCCESS', NULL, 55, 'seed-0005', '2026-11-07 16:20:00'),
(6, 2, 'biz', 'BUSINESS', '127.0.0.1', '/api/requirements', 'POST', '需求管理', '新建需求', 'REQUIREMENT', '3', 'SUCCESS', NULL, 33, 'seed-0006', '2026-10-28 14:00:00'),
(7, 3, 'ra', 'RA', '127.0.0.1', '/api/changes/1/apply', 'POST', '变更管理', '变更生效', 'REQUIREMENT', '3', 'SUCCESS', NULL, 61, 'seed-0007', '2026-11-06 14:30:00'),
(8, 3, 'ra', 'RA', '127.0.0.1', '/api/acceptances/2/conclusion', 'POST', '交付验收', '验收通过', 'REQUIREMENT', '5', 'SUCCESS', NULL, 48, 'seed-0008', '2026-10-22 15:00:00'),
(9, 3, 'ra', 'RA', '127.0.0.1', '/api/requirements/6/action', 'POST', '需求管理', '归档', 'REQUIREMENT', '6', 'SUCCESS', NULL, 29, 'seed-0009', '2026-10-25 15:30:00'),
(10, 7, 'accept', 'ACCEPT', '127.0.0.1', '/api/acceptances/2/conclusion', 'POST', '交付验收', '验收通过', 'REQUIREMENT', '6', 'SUCCESS', NULL, 52, 'seed-0010', '2026-10-22 15:00:00');
