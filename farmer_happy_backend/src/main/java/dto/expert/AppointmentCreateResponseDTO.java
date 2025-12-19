package dto.expert;

import java.util.List;

public class AppointmentCreateResponseDTO {
    private String group_id;
    private List<String> appointment_ids;

    public String getGroup_id() { return group_id; }
    public void setGroup_id(String group_id) { this.group_id = group_id; }

    public List<String> getAppointment_ids() { return appointment_ids; }
    public void setAppointment_ids(List<String> appointment_ids) { this.appointment_ids = appointment_ids; }
}