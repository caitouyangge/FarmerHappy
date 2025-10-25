// src/dto/farmer/ProductBatchActionResultDTO.java
package dto.farmer;

import java.util.List;
import java.util.Map;

public class ProductBatchActionResultDTO {
    private int success_count;
    private int failure_count;
    private List<BatchActionResultItem> results;

    // Getters and Setters
    public int getSuccess_count() {
        return success_count;
    }

    public void setSuccess_count(int success_count) {
        this.success_count = success_count;
    }

    public int getFailure_count() {
        return failure_count;
    }

    public void setFailure_count(int failure_count) {
        this.failure_count = failure_count;
    }

    public List<BatchActionResultItem> getResults() {
        return results;
    }

    public void setResults(List<BatchActionResultItem> results) {
        this.results = results;
    }

    public static class BatchActionResultItem {
        private String product_id;
        private boolean success;
        private String message;
        private Map<String, String> _links;

        // Getters and Setters
        public String getProduct_id() {
            return product_id;
        }

        public void setProduct_id(String product_id) {
            this.product_id = product_id;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Map<String, String> get_links() {
            return _links;
        }

        public void set_links(Map<String, String> _links) {
            this._links = _links;
        }
    }
}
