package controller;

import dto.expert.*;
import service.expert.ExpertAppointmentService;

import java.util.*;

public class ExpertAppointmentController {
    private final ExpertAppointmentService service;

    public ExpertAppointmentController() {
        this.service = new ExpertAppointmentService();
    }

    public Map<String, Object> listExperts() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = service.listExperts();
            response.put("code", 200);
            response.put("message", "成功");
            response.put("data", data);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
        }
        return response;
    }

    public Map<String, Object> apply(AppointmentCreateRequestDTO request) {
        Map<String, Object> response = new HashMap<>();
        try {
            AppointmentCreateResponseDTO result = service.apply(request);
            response.put("code", 201);
            response.put("message", "预约申请已提交");
            response.put("data", result);
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
        }
        return response;
    }

    public Map<String, Object> getExpertAppointments(String expertPhone) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = service.getExpertAppointments(expertPhone);
            response.put("code", 200);
            response.put("message", "成功");
            response.put("data", data);
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
        }
        return response;
    }

    public Map<String, Object> getFarmerAppointments(String farmerPhone) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = service.getFarmerAppointments(farmerPhone);
            response.put("code", 200);
            response.put("message", "成功");
            response.put("data", data);
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
        }
        return response;
    }

    public Map<String, Object> decide(String appointmentId, AppointmentDecisionRequestDTO request) {
        Map<String, Object> response = new HashMap<>();
        try {
            AppointmentDecisionResponseDTO result = service.decide(appointmentId, request);
            response.put("code", 200);
            response.put("message", "处理成功");
            response.put("data", result);
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
        }
        return response;
    }

    public Map<String, Object> getDetail(String appointmentId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = service.getDetail(appointmentId);
            response.put("code", 200);
            response.put("message", "成功");
            response.put("data", data);
        } catch (IllegalArgumentException e) {
            response.put("code", 404);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
        }
        return response;
    }
}