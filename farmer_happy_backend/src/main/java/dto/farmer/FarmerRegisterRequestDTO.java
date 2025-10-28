package dto.farmer;

import dto.auth.RegisterRequestDTO;

public class FarmerRegisterRequestDTO extends RegisterRequestDTO {
    private String farmName;
    private String farmAddress;
    private Double farmSize;

    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }

    public String getFarmAddress() {
        return farmAddress;
    }

    public void setFarmAddress(String farmAddress) {
        this.farmAddress = farmAddress;
    }

    public Double getFarmSize() {
        return farmSize;
    }

    public void setFarmSize(Double farmSize) {
        this.farmSize = farmSize;
    }
}
