package dto.expert;

public class AppointmentDecisionResponseDTO {
    private String appointment_id;
    private String status;

    public String getAppointment_id() { return appointment_id; }
    public void setAppointment_id(String appointment_id) { this.appointment_id = appointment_id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}