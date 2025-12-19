package dto.expert;

import java.util.List;

public class AppointmentCreateRequestDTO {
    private String farmer_phone;
    private String mode;
    private List<Long> expert_ids;
    private String message;

    public String getFarmer_phone() { return farmer_phone; }
    public void setFarmer_phone(String farmer_phone) { this.farmer_phone = farmer_phone; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public List<Long> getExpert_ids() { return expert_ids; }
    public void setExpert_ids(List<Long> expert_ids) { this.expert_ids = expert_ids; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}