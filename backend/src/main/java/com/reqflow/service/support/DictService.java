package com.reqflow.service.support;

import com.reqflow.domain.enums.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DictService {

    private static final Map<String, String> TYPES = new LinkedHashMap<>();
    private static final Map<String, String> SOURCES = new LinkedHashMap<>();
    private static final Map<String, String> PRIORITIES = new LinkedHashMap<>();

    static {
        TYPES.put("NEW_FEATURE", "功能新增");
        TYPES.put("OPTIMIZE", "功能优化");
        TYPES.put("BUG_FIX", "缺陷修复");
        TYPES.put("REPORT", "数据报表");
        TYPES.put("COMPLIANCE", "合规改造");

        SOURCES.put("BUSINESS_DEPT", "业务部门");
        SOURCES.put("REGULATION", "监管要求");
        SOURCES.put("INTERNAL", "内部优化");
        SOURCES.put("PRODUCTION", "生产问题");

        PRIORITIES.put("P0", "P0 - 最高");
        PRIORITIES.put("P1", "P1 - 高");
        PRIORITIES.put("P2", "P2 - 中");
        PRIORITIES.put("P3", "P3 - 低");
    }

    public String typeLabel(String code) {
        return TYPES.getOrDefault(code, code);
    }

    public String sourceLabel(String code) {
        return SOURCES.getOrDefault(code, code);
    }

    public String priorityLabel(String code) {
        return PRIORITIES.getOrDefault(code, code);
    }

    public String statusLabel(String code) {
        return RequirementStatus.labelOf(code);
    }

    public String taskTypeLabel(String code) {
        for (TaskType type : TaskType.values()) {
            if (type.name().equals(code)) {
                return type.label();
            }
        }
        return code;
    }

    public String taskStatusLabel(String code) {
        for (TaskStatus status : TaskStatus.values()) {
            if (status.name().equals(code)) {
                return status.label();
            }
        }
        return code;
    }

    public String reviewStatusLabel(String code) {
        for (ReviewStatus status : ReviewStatus.values()) {
            if (status.name().equals(code)) {
                return status.label();
            }
        }
        return code;
    }

    public String reviewResultLabel(String code) {
        for (ReviewResult result : ReviewResult.values()) {
            if (result.name().equals(code)) {
                return result.label();
            }
        }
        return code;
    }

    public String changeStatusLabel(String code) {
        for (ChangeStatus status : ChangeStatus.values()) {
            if (status.name().equals(code)) {
                return status.label();
            }
        }
        return code;
    }

    public String acceptanceStatusLabel(String code) {
        for (AcceptanceStatus status : AcceptanceStatus.values()) {
            if (status.name().equals(code)) {
                return status.label();
            }
        }
        return code;
    }

    public String deliverableTypeLabel(String code) {
        for (DeliverableType type : DeliverableType.values()) {
            if (type.name().equals(code)) {
                return type.label();
            }
        }
        return code;
    }

    public Map<String, Object> all() {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("requirementTypes", options(TYPES));
        dict.put("requirementSources", options(SOURCES));
        dict.put("priorities", options(PRIORITIES));
        dict.put("requirementStatuses", enumOptions(RequirementStatus.values()));
        dict.put("requirementActions", actionOptions());
        dict.put("taskTypes", enumOptions(TaskType.values()));
        dict.put("taskStatuses", enumOptions(TaskStatus.values()));
        dict.put("reviewStatuses", enumOptions(ReviewStatus.values()));
        dict.put("reviewResults", enumOptions(ReviewResult.values()));
        dict.put("changeStatuses", enumOptions(ChangeStatus.values()));
        dict.put("acceptanceStatuses", enumOptions(AcceptanceStatus.values()));
        dict.put("deliverableTypes", deliverableOptions());
        dict.put("dataScopes", enumOptions(DataScope.values()));
        return dict;
    }

    private List<Map<String, String>> options(Map<String, String> source) {
        List<Map<String, String>> list = new ArrayList<>();
        source.forEach((code, label) -> list.add(Map.of("value", code, "label", label)));
        return list;
    }

    private List<Map<String, String>> enumOptions(Enum<?>[] values) {
        List<Map<String, String>> list = new ArrayList<>();
        for (Enum<?> value : values) {
            String label;
            try {
                label = (String) value.getClass().getMethod("label").invoke(value);
            } catch (Exception e) {
                label = value.name();
            }
            list.add(Map.of("value", value.name(), "label", label));
        }
        return list;
    }

    private List<Map<String, String>> deliverableOptions() {
        List<Map<String, String>> list = new ArrayList<>();
        for (DeliverableType type : DeliverableType.values()) {
            list.add(Map.of("value", type.name(), "label", type.label()));
        }
        return list;
    }

    private List<Map<String, String>> actionOptions() {
        List<Map<String, String>> list = new ArrayList<>();
        for (RequirementAction action : RequirementAction.values()) {
            list.add(Map.of("value", action.name(), "label", action.label()));
        }
        return list;
    }
}
