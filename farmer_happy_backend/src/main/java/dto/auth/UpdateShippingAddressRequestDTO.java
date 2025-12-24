// dto/auth/UpdateShippingAddressRequestDTO.java
package dto.auth;

public class UpdateShippingAddressRequestDTO {
    private String phone;
    private String shippingAddress;

    // Getters and Setters
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}

