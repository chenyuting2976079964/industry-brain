package com.industrybrain.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Excel 导入工具类
 * 通用 Excel 读取，第一行为表头，返回 List&lt;Map&gt;
 */
public class ExcelImportUtils {

    private static final Logger log = LoggerFactory.getLogger(ExcelImportUtils.class);

    /**
     * 读取 Excel 文件，第一行为表头，后续行为数据
     *
     * @param file Excel 文件
     * @return 数据列表，每个 Map 的 key 为表头列名
     */
    public static List<Map<String, Object>> readExcelData(File file) {
        List<Map<String, Object>> data = new ArrayList<>();
        try (InputStream is = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) return data;

            // 读取表头行
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return data;
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                headers.add(cell != null ? cell.getStringCellValue().trim() : "COL_" + c);
            }

            // 读取数据行
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, Object> rowData = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c);
                    rowData.put(headers.get(c), getCellValue(cell));
                }
                data.add(rowData);
            }
        } catch (IOException e) {
            log.error("读取Excel文件失败: {}", file.getAbsolutePath(), e);
        }
        return data;
    }

    /**
     * 获取单元格值（自动识别类型）
     */
    public static Object getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    return (long) val;
                }
                return val;
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try { return cell.getStringCellValue().trim(); } catch (Exception e) { return cell.getNumericCellValue(); }
            default:
                return null;
        }
    }
}
