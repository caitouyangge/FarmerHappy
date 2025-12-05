// service/crawler/PriceCrawlerService.java
package service.crawler;

import java.util.Map;

public interface PriceCrawlerService {
    Map<String, Object> crawlAgriculturalPrices(String startTime, String endTime, String productName) throws Exception;
}
