package dto.expert;

import java.sql.Timestamp;

public class AppointmentDecisionRequestDTO {
    private String expert_phone;
    private String action;
    private String expert_note;
    private Timestamp scheduled_time;
    private String location;

    public String getExpert_phone() { return expert_phone; }
    public void setExpert_phone(String expert_phone) { this.expert_phone = expert_phone; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getExpert_note() { return expert_note; }
    public void setExpert_note(String expert_note) { this.expert_note = expert_note; }

    public Timestamp getScheduled_time() { return scheduled_time; }
    public void setScheduled_time(Timestamp scheduled_time) { this.scheduled_time = scheduled_time; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}