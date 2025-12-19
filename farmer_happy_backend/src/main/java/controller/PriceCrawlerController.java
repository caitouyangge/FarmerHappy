// controller/PriceCrawlerController.java
package controller;

import service.crawler.PriceCrawlerService;
import service.crawler.PriceCrawlerServiceImpl;
import service.crawler.CsvSplitterService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PriceCrawlerController {
    private PriceCrawlerService priceCrawlerService;
    private final CsvSplitterService csvSplitterService;

    public PriceCrawlerController() {
        this.priceCrawlerService = new PriceCrawlerServiceImpl();
        this.csvSplitterService = new CsvSplitterService();
    }

    public PriceCrawlerController(PriceCrawlerService priceCrawlerService) {
        this.priceCrawlerService = priceCrawlerService;
        this.csvSplitterService = new CsvSplitterService();
    }

    public Map<String, Object> crawlAgriculturalPrices(Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 参数验证
            String startTime = (String) requestBody.get("start_time");
            String endTime = (String) requestBody.get("end_time");
            String productName = (String) requestBody.get("product_name");

            if (startTime == null || startTime.isEmpty()) {
                response.put("code", 400);
                response.put("message", "参数验证失败");
                Map<String, String> error = new HashMap<>();
                error.put("field", "start_time");
                error.put("message", "开始时间不能为空");
                response.put("errors", new Object[]{error});
                return response;
            }

            if (endTime == null || endTime.isEmpty()) {
                response.put("code", 400);
                response.put("message", "参数验证失败");
                Map<String, String> error = new HashMap<>();
                error.put("field", "end_time");
                error.put("message", "结束时间不能为空");
                response.put("errors", new Object[]{error});
                return response;
            }

            if (productName == null || productName.isEmpty()) {
                response.put("code", 400);
                response.put("message", "参数验证失败");
                Map<String, String> error = new HashMap<>();
                error.put("field", "product_name");
                error.put("message", "农产品名称不能为空");
                response.put("errors", new Object[]{error});
                return response;
            }

            // 调用爬虫服务
            Map<String, Object> result = priceCrawlerService.crawlAgriculturalPrices(startTime, endTime, productName);

            response.put("code", 200);
            response.put("message", "数据获取成功");
            response.put("data", result);

        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "数据获取失败，请稍后重试");
            e.printStackTrace();
        }

        return response;
    }

    /**
     * 获取 result/split 下的可选品种/文件列表（供前端动态勾选）
     */
    public Map<String, Object> listSplitFiles(Map<String, String> queryParams) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> files = csvSplitterService.listSplitFiles();
            response.put("code", 200);
            response.put("message", "获取split列表成功");
            Map<String, Object> data = new HashMap<>();
            data.put("split_dir", "result/split");
            data.put("files", files);
            data.put("count", files.size());
            response.put("data", data);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "获取split列表失败");
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 用户勾选品种/文件并选择位置后，执行放置（复制到 result/placed/{location}）
     *
     * requestBody:
     * - target_dir: string (必填，建议传相对项目根目录的路径，如 result/selected/xxx)
     * - location: string (兼容旧参数；会映射到 result/placed/{location})
     * - file_names: string[] (可选，与 varieties 二选一)
     * - varieties: string[] (可选，与 file_names 二选一；会自动取该品种最新文件)
     */
    public Map<String, Object> placeSplitFiles(Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        try {
            String targetDir = requestBody != null ? (String) requestBody.get("target_dir") : null;
            String location = requestBody != null ? (String) requestBody.get("location") : null; // 兼容旧参数
            Object fileNamesObj = requestBody != null ? requestBody.get("file_names") : null;
            Object varietiesObj = requestBody != null ? requestBody.get("varieties") : null;

            // target_dir 优先；否则兼容 location -> result/placed/{location}
            if ((targetDir == null || targetDir.trim().isEmpty()) && (location == null || location.trim().isEmpty())) {
                response.put("code", 400);
                response.put("message", "参数验证失败");
                Map<String, String> error = new HashMap<>();
                error.put("field", "target_dir");
                error.put("message", "目标文件夹(target_dir)不能为空（或使用旧参数 location）");
                response.put("errors", new Object[]{error});
                return response;
            }
            if (targetDir == null || targetDir.trim().isEmpty()) {
                targetDir = "result/placed/" + location.trim();
            }

            List<String> fileNames = null;
            if (fileNamesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> raw = (List<Object>) fileNamesObj;
                fileNames = new java.util.ArrayList<>();
                for (Object o : raw) {
                    if (o != null) fileNames.add(o.toString());
                }
            }

            List<String> varieties = null;
            if (varietiesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> raw = (List<Object>) varietiesObj;
                varieties = new java.util.ArrayList<>();
                for (Object o : raw) {
                    if (o != null) varieties.add(o.toString());
                }
            }

            Map<String, Object> result = csvSplitterService.placeSplitFiles(fileNames, varieties, targetDir.trim());
            response.put("code", 200);
            response.put("message", "放置成功");
            response.put("data", result);
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "放置失败");
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 将 result/split 下已有的 CSV 批量导出为同名 XLSX（不需要重新爬取）
     *
     * POST /api/v1/agriculture/price/split/export_xlsx
     */
    public Map<String, Object> exportSplitXlsx(Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = csvSplitterService.exportExistingSplitCsvToXlsx();
            response.put("code", 200);
            response.put("message", "导出xlsx成功");
            response.put("data", result);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "导出xlsx失败");
            e.printStackTrace();
        }
        return response;
    }
}
