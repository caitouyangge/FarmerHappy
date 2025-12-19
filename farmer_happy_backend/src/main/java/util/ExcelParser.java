// src/main/java/util/ExcelParser.java
package util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Excel文件解析器
 * 用于解析包含价格-时间数据的Excel文件
 */
public class ExcelParser {
    
    /**
     * 数据点类，表示一个日期-价格对
     */
    public static class DataPoint {
        private Date date;
        private double price;
        
        public DataPoint(Date date, double price) {
            this.date = date;
            this.price = price;
        }
        
        public Date getDate() {
            return date;
        }
        
        public double getPrice() {
            return price;
        }
    }
    
    /**
     * 解析Excel文件
     * @param inputStream Excel文件输入流
     * @param fileName 文件名（用于判断格式）
     * @return 解析后的数据点列表
     * @throws Exception 解析异常
     */
    public List<DataPoint> parse(InputStream inputStream, String fileName) throws Exception {
        Workbook workbook;
        
        // 根据文件扩展名选择不同的Workbook实现
        if (fileName.endsWith(".xlsx")) {
            workbook = new XSSFWorkbook(inputStream);
        } else if (fileName.endsWith(".xls")) {
            workbook = new HSSFWorkbook(inputStream);
        } else {
            throw new IllegalArgumentException("不支持的文件格式，仅支持.xls和.xlsx");
        }
        
        try {
            Sheet sheet = workbook.getSheetAt(0); // 读取第一个工作表
            
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                throw new IllegalArgumentException("Excel文件至少需要2行数据（表头+数据）");
            }
            
            List<DataPoint> dataPoints = new ArrayList<>();
            
            // 跳过表头，从第二行开始读取
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                
                // 读取日期（第一列）
                Cell dateCell = row.getCell(0);
                if (dateCell == null) {
                    continue;
                }
                
                Date date = parseDate(dateCell);
                if (date == null) {
                    continue;
                }
                
                // 读取价格（第二列）
                Cell priceCell = row.getCell(1);
                if (priceCell == null) {
                    continue;
                }
                
                double price = parsePrice(priceCell);
                if (price < 0) {
                    continue; // 跳过负数价格
                }
                
                dataPoints.add(new DataPoint(date, price));
            }
            
            if (dataPoints.size() < 2) {
                throw new IllegalArgumentException("数据量不足，至少需要2条有效数据");
            }
            
            // 按日期排序
            dataPoints.sort(Comparator.comparing(DataPoint::getDate));
            
            return dataPoints;
            
        } finally {
            workbook.close();
        }
    }
    
    /**
     * 解析日期单元格
     */
    private Date parseDate(Cell cell) {
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                // Excel日期格式
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
            } else if (cell.getCellType() == CellType.STRING) {
                // 字符串格式的日期
                String dateStr = cell.getStringCellValue().trim();
                return parseDateString(dateStr);
            }
        } catch (Exception e) {
            // 解析失败，返回null
        }
        return null;
    }
    
    /**
     * 解析字符串格式的日期
     */
    private Date parseDateString(String dateStr) {
        // 支持的日期格式
        String[] patterns = {
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "MM/dd/yyyy",
            "dd/MM/yyyy"
        };
        
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                sdf.setLenient(false);
                return sdf.parse(dateStr);
            } catch (Exception e) {
                // 继续尝试下一个格式
            }
        }
        
        return null;
    }
    
    /**
     * 解析价格单元格
     */
    private double parsePrice(Cell cell) {
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String priceStr = cell.getStringCellValue().trim();
                // 移除可能的货币符号和空格
                priceStr = priceStr.replaceAll("[¥$€£,，\\s]", "");
                return Double.parseDouble(priceStr);
            } else if (cell.getCellType() == CellType.FORMULA) {
                // 公式单元格，获取计算结果
                return cell.getNumericCellValue();
            }
        } catch (Exception e) {
            // 解析失败
        }
        return -1;
    }
}



