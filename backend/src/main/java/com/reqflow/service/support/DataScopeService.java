package com.reqflow.service.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.reqflow.common.BizException;
import com.reqflow.common.ErrorCode;
import com.reqflow.common.Permissions;
import com.reqflow.domain.entity.Requirement;
import com.reqflow.mapper.RequirementMapper;
import com.reqflow.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 数据权限唯一实现。
 * <p>
 * 列表查询、详情校验、跨表过滤全部复用同一段谓词，避免“SQL 一套、Java 一套”导致的语义漂移。
 * 范围由角色决定，多角色取最宽的一档；无论哪一档，需求提出人始终可见自己提出的需求。
 */
@Service
@RequiredArgsConstructor
public class DataScopeService {

    private static final Set<String> ALL_SCOPES = Set.of("ALL", "DEPT", "PROJECT", "SELF");

    private final RequirementMapper requirementMapper;

    /** 在需求查询上叠加数据范围 */
    public void apply(QueryWrapper<Requirement> wrapper, LoginUser user) {
        ScopeSql scope = scopeSql(user, null);
        wrapper.apply(scope.sql(), scope.params());
    }

    /**
     * 在其他业务表（评审、变更、任务、验收等）上按需求可见性过滤。
     *
     * @param requirementIdColumn 业务表中指向 requirement.id 的列名
     */
    public <T> void applyByRequirement(QueryWrapper<T> wrapper, String requirementIdColumn, LoginUser user) {
        ScopeSql scope = scopeSql(user, "r");
        wrapper.apply(requirementIdColumn
                        + " in (select r.id from requirement r where r.deleted = 0 and (" + scope.sql() + "))",
                scope.params());
    }

    /** 详情/单条数据的归属校验，与列表使用同一谓词 */
    public boolean canView(Requirement requirement, LoginUser user) {
        return canView(requirement.getId(), user);
    }

    public boolean canView(Long requirementId, LoginUser user) {
        if (requirementId == null) {
            return false;
        }
        QueryWrapper<Requirement> wrapper = new QueryWrapper<>();
        wrapper.eq("id", requirementId);
        apply(wrapper, user);
        return requirementMapper.selectCount(wrapper) > 0;
    }

    public void checkView(Requirement requirement, LoginUser user) {
        if (!canView(requirement, user)) {
            throw BizException.of(ErrorCode.DATA_FORBIDDEN);
        }
    }

    private ScopeSql scopeSql(LoginUser user, String alias) {
        String prefix = alias == null || alias.isBlank() ? "" : alias + ".";
        String scope = user.getDataScope() == null || !ALL_SCOPES.contains(user.getDataScope())
                ? "SELF" : user.getDataScope();
        boolean canAccept = hasPermission(user, Permissions.REQUIREMENT_ACCEPT);
        return switch (scope) {
            case "ALL" -> new ScopeSql("1 = 1", new Object[0]);
            case "DEPT" -> new ScopeSql(
                    "(" + prefix + "proposer_id = {0}"
                            + " or " + prefix + "dept_id = {1}"
                            + " or " + prefix + "analyst_id = {0}"
                            + " or (" + prefix + "status = 'PENDING_ACCEPT' and {2} = 1))",
                    new Object[]{user.getId(), user.getDeptId(), canAccept ? 1 : 0});
            case "PROJECT" -> new ScopeSql(
                    "(" + prefix + "proposer_id = {0}"
                            + " or " + prefix + "pm_id = {0}"
                            + " or " + prefix + "analyst_id = {0}"
                            + " or " + prefix + "id in (select t.requirement_id from task t"
                            + " where t.deleted = 0 and t.assignee_id = {0})"
                            + " or " + prefix + "id in (select a.requirement_id from acceptance a"
                            + " where a.acceptor_id = {0})"
                            + " or " + prefix + "id in (select rv.requirement_id from review rv"
                            + " where rv.host_id = {0} or rv.id in (select rp.review_id from review_participant rp"
                            + " where rp.user_id = {0}))"
                            + " or " + prefix + "project_id in (select pj.id from project pj"
                            + " where pj.deleted = 0 and pj.pm_id = {0}))",
                    new Object[]{user.getId()});
            default -> new ScopeSql("(" + prefix + "proposer_id = {0})", new Object[]{user.getId()});
        };
    }

    private boolean hasPermission(LoginUser user, String permission) {
        if (user.getPermissions() == null) {
            return false;
        }
        return user.getPermissions().contains(Permissions.ADMIN) || user.getPermissions().contains(permission);
    }

    private record ScopeSql(String sql, Object[] params) {
    }
}
