-- 1) 评审人从逗号串拆成关联表，恢复第一范式并支持按人索引
CREATE TABLE review_participant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    participant_type VARCHAR(20) NOT NULL,
    created_at DATETIME,
    CONSTRAINT uk_review_participant UNIQUE (review_id, user_id)
);

CREATE INDEX idx_review_participant_user ON review_participant (user_id);

INSERT INTO review_participant (review_id, user_id, participant_type, created_at)
SELECT r.id, u.id, 'REVIEWER', r.created_at
FROM review r
JOIN sys_user u ON r.reviewer_ids LIKE CONCAT('%,', CAST(u.id AS CHAR), ',%');

ALTER TABLE review DROP COLUMN reviewer_ids;

-- 2) 动作级权限点：状态机与越权判断改为权限点驱动，角色仅作为权限集合
INSERT INTO sys_permission (id, code, name, module) VALUES
(25, 'requirement:submit', '提交需求', '需求管理'),
(26, 'requirement:accept', '受理需求', '需求管理'),
(27, 'requirement:submit-review', '提交评审', '需求管理'),
(28, 'requirement:review-decide', '提交评审结论', '需求管理'),
(29, 'requirement:edit-any', '编辑他人需求', '需求管理'),
(30, 'requirement:schedule', '需求排期', '需求管理'),
(31, 'requirement:develop', '开始开发', '需求管理'),
(32, 'requirement:submit-test', '提交测试', '需求管理'),
(33, 'requirement:submit-acceptance', '提交验收', '需求管理'),
(34, 'requirement:accept-decide', '提交验收结论', '需求管理'),
(35, 'requirement:archive', '需求归档', '需求管理'),
(36, 'requirement:hold', '挂起/恢复需求', '需求管理'),
(37, 'requirement:cancel', '撤销需求', '需求管理'),
(38, 'sys:admin', '系统管理员特权', '系统管理');

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
-- 系统管理员：全部动作权限
(1,25),(1,26),(1,27),(1,28),(1,29),(1,30),(1,31),(1,32),(1,33),(1,34),(1,35),(1,36),(1,37),(1,38),
-- 需求提出人：提交、撤销
(2,25),(2,37),
-- 需求分析：受理、评审、归档、挂起、撤销、提交验收
(3,25),(3,26),(3,27),(3,28),(3,29),(3,33),(3,35),(3,36),(3,37),
-- 项目经理：排期、开发、测试、验收、归档、挂起、撤销
(4,30),(4,31),(4,32),(4,33),(4,34),(4,35),(4,36),(4,37),
-- 开发：开始开发、提交测试
(5,31),(5,32),
-- 测试：提交测试、提交验收
(6,32),(6,33),
-- 验收人：提交验收结论
(7,34);
