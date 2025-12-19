package service.expert;

import dto.expert.*;
import repository.DatabaseManager;
import service.auth.AuthService;
import service.auth.AuthServiceImpl;

import java.sql.Timestamp;
import java.sql.SQLException;
import java.util.*;

public class ExpertAppointmentService {
    private final DatabaseManager dbManager;
    private final AuthService authService;

    public ExpertAppointmentService() {
        this.dbManager = DatabaseManager.getInstance();
        this.authService = new AuthServiceImpl();
    }

    public Map<String, Object> listExperts() throws SQLException {
        List<Map<String, Object>> experts = dbManager.listActiveExperts();
        Map<String, Object> data = new HashMap<>();
        data.put("list", experts);
        return data;
    }

    public AppointmentCreateResponseDTO apply(AppointmentCreateRequestDTO request) throws SQLException {
        if (request.getFarmer_phone() == null || request.getFarmer_phone().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (request.getMode() == null || (!"online".equals(request.getMode()) && !"offline".equals(request.getMode()))) {
            throw new IllegalArgumentException("预约方式不正确");
        }
        if (request.getExpert_ids() == null || request.getExpert_ids().isEmpty()) {
            throw new IllegalArgumentException("必须选择至少一位专家");
        }

        entity.User user = authService.findUserByPhone(request.getFarmer_phone());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        try {
            if (!authService.checkUserTypeExists(user.getUid(), "farmer")) {
                throw new IllegalArgumentException("只有农户可以发起预约");
            }
        } catch (SQLException e) {
            throw e;
        }

        Long farmerId = dbManager.getFarmerIdByPhone(request.getFarmer_phone());
        if (farmerId == null) {
            throw new IllegalArgumentException("农户身份未启用");
        }

        String groupId = UUID.randomUUID().toString();
        List<String> appointmentIds = dbManager.createExpertAppointments(groupId, farmerId, request.getExpert_ids(), request.getMode(), request.getMessage());

        AppointmentCreateResponseDTO resp = new AppointmentCreateResponseDTO();
        resp.setGroup_id(groupId);
        resp.setAppointment_ids(appointmentIds);
        return resp;
    }

    public Map<String, Object> getExpertAppointments(String expertPhone) throws SQLException {
        if (expertPhone == null || expertPhone.isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        entity.User user = authService.findUserByPhone(expertPhone);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        try {
            if (!authService.checkUserTypeExists(user.getUid(), "expert")) {
                throw new IllegalArgumentException("只有专家可查看预约请求");
            }
        } catch (SQLException e) {
            throw e;
        }

        List<Map<String, Object>> list = dbManager.getExpertAppointmentsByExpertPhone(expertPhone);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        return data;
    }

    public Map<String, Object> getFarmerAppointments(String farmerPhone) throws SQLException {
        if (farmerPhone == null || farmerPhone.isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        entity.User user = authService.findUserByPhone(farmerPhone);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        try {
            if (!authService.checkUserTypeExists(user.getUid(), "farmer")) {
                throw new IllegalArgumentException("只有农户可查看预约结果");
            }
        } catch (SQLException e) {
            throw e;
        }

        List<Map<String, Object>> list = dbManager.getFarmerAppointmentsByFarmerPhone(farmerPhone);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        return data;
    }

    public AppointmentDecisionResponseDTO decide(String appointmentId, AppointmentDecisionRequestDTO request) throws SQLException {
        if (request.getExpert_phone() == null || request.getExpert_phone().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (request.getAction() == null || (!"accepted".equals(request.getAction()) && !"rejected".equals(request.getAction()))) {
            throw new IllegalArgumentException("操作不合法");
        }
        Map<String, Object> appt = dbManager.getAppointmentByAppointmentId(appointmentId);
        if (appt == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        String expertPhoneInRecord = (String) appt.get("expert_phone");
        if (expertPhoneInRecord == null || !expertPhoneInRecord.equals(request.getExpert_phone())) {
            throw new IllegalArgumentException("无权限处理该预约");
        }

        Timestamp ts = request.getScheduled_time();
        dbManager.updateAppointmentDecision(appointmentId, request.getAction(), request.getExpert_note(), ts, request.getLocation());

        AppointmentDecisionResponseDTO resp = new AppointmentDecisionResponseDTO();
        resp.setAppointment_id(appointmentId);
        resp.setStatus(request.getAction());
        return resp;
    }

    public Map<String, Object> getDetail(String appointmentId) throws SQLException {
        Map<String, Object> appt = dbManager.getAppointmentByAppointmentId(appointmentId);
        if (appt == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("detail", appt);
        return data;
    }
}