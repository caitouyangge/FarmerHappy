// dto/auth/UserProfileResponseDTO.java
package dto.auth;

import java.math.BigDecimal;
import java.util.Map;

public class UserProfileResponseDTO {
    private String uid;
    private String phone;
    private String nickname;
    private String userType;
    private BigDecimal money;
    
    // 买家特有信息
    private String shippingAddress;
    private String memberLevel;
    
    // 农户特有信息
    private String farmName;
    private String farmAddress;
    private BigDecimal farmSize;
    
    // 专家特有信息
    private String expertName;
    private String expertTitle;
    private String expertInstitution;
    private String expertise;
    
    // 银行特有信息
    private String bankName;
    private String branchName;
    private String contactPerson;
    private String contactPhone;

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getMemberLevel() {
        return memberLevel;
    }

    public void setMemberLevel(String memberLevel) {
        this.memberLevel = memberLevel;
    }

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

    public BigDecimal getFarmSize() {
        return farmSize;
    }

    public void setFarmSize(BigDecimal farmSize) {
        this.farmSize = farmSize;
    }

    public String getExpertName() {
        return expertName;
    }

    public void setExpertName(String expertName) {
        this.expertName = expertName;
    }

    public String getExpertTitle() {
        return expertTitle;
    }

    public void setExpertTitle(String expertTitle) {
        this.expertTitle = expertTitle;
    }

    public String getExpertInstitution() {
        return expertInstitution;
    }

    public void setExpertInstitution(String expertInstitution) {
        this.expertInstitution = expertInstitution;
    }

    public String getExpertise() {
        return expertise;
    }

    public void setExpertise(String expertise) {
        this.expertise = expertise;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}

