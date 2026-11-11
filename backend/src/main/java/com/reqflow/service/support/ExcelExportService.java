package com.reqflow.service.support;

import com.reqflow.vo.RequirementVO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    private static final String[] HEADERS = {
            "需求编号", "需求标题", "需求类型", "需求来源", "业务部门", "提出人", "分析人", "项目经理",
            "优先级", "状态", "版本号", "变更次数", "期望交付", "计划上线", "更新时间"
    };

    public byte[] exportRequirements(List<RequirementVO> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("需求清单");
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (RequirementVO r : rows) {
                Row row = sheet.createRow(rowIndex++);
                int col = 0;
                row.createCell(col++).setCellValue(nvl(r.getReqNo()));
                row.createCell(col++).setCellValue(nvl(r.getTitle()));
                row.createCell(col++).setCellValue(nvl(r.getTypeLabel()));
                row.createCell(col++).setCellValue(nvl(r.getSourceLabel()));
                row.createCell(col++).setCellValue(nvl(r.getDeptName()));
                row.createCell(col++).setCellValue(nvl(r.getProposerName()));
                row.createCell(col++).setCellValue(nvl(r.getAnalystName()));
                row.createCell(col++).setCellValue(nvl(r.getPmName()));
                row.createCell(col++).setCellValue(nvl(r.getPriority()));
                row.createCell(col++).setCellValue(nvl(r.getStatusLabel()));
                row.createCell(col++).setCellValue(nvl(r.getVersion()));
                row.createCell(col++).setCellValue(r.getChangeCount() == null ? 0 : r.getChangeCount());
                row.createCell(col++).setCellValue(r.getExpectDate() == null ? "" : r.getExpectDate().toString());
                row.createCell(col++).setCellValue(r.getPlanDate() == null ? "" : r.getPlanDate().toString());
                row.createCell(col++).setCellValue(r.getUpdatedAt() == null ? "" : r.getUpdatedAt().toString());
            }

            int[] widths = {18, 42, 14, 14, 16, 12, 12, 12, 10, 14, 10, 10, 14, 14, 22};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(0, 1);
            if (rows.size() > 0) {
                sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, rows.size(), 0, HEADERS.length - 1));
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("导出 Excel 失败", e);
        }
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
