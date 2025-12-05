// controller/PriceCrawlerController.java
package controller;

import service.crawler.PriceCrawlerService;
import service.crawler.PriceCrawlerServiceImpl;

import java.util.HashMap;
import java.util.Map;

public class PriceCrawlerController {
    private PriceCrawlerService priceCrawlerService;

    public PriceCrawlerController() {
        this.priceCrawlerService = new PriceCrawlerServiceImpl();
    }

    public PriceCrawlerController(PriceCrawlerService priceCrawlerService) {
        this.priceCrawlerService = priceCrawlerService;
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
}
