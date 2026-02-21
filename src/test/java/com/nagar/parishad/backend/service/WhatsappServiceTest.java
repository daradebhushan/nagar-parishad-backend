package com.nagar.parishad.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nagar.parishad.backend.dto.ComplaintDTO;
import com.nagar.parishad.backend.dto.ComplaintSubmissionDTO;
import com.nagar.parishad.backend.dto.ComplaintTypeDTO;
import com.nagar.parishad.backend.entity.ChatbotConfig;
import com.nagar.parishad.backend.entity.ChatbotSession;
import com.nagar.parishad.backend.entity.Department;
import com.nagar.parishad.backend.entity.TenantTwilioConfig;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.enums.ChatbotState;
import com.nagar.parishad.backend.repository.ChatbotConfigRepository;
import com.nagar.parishad.backend.repository.ChatbotSessionRepository;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import com.nagar.parishad.backend.repository.TenantTwilioConfigRepository;
import com.nagar.parishad.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class WhatsappServiceTest {

    @InjectMocks
    private WhatsappService whatsappService;

    @Mock
    private ChatbotSessionRepository sessionRepository;

    @Mock
    private ChatbotConfigRepository configRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ComplaintTypeService complaintTypeService;

    @Mock
    private ComplaintService complaintService;

    @Mock
    private TenantTwilioConfigRepository tenantTwilioConfigRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private User mockAdmin;
    private TenantTwilioConfig mockConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockAdmin = new User();
        mockAdmin.setId(1L);
        mockAdmin.setEmail("admin@test.com");

        mockConfig = new TenantTwilioConfig();
        mockConfig.setId(1L);
        mockConfig.setAdmin(mockAdmin);
        mockConfig.setPhoneNumber("whatsapp:+918888888888");
        mockConfig.setAccountSid("testsid");
        mockConfig.setAuthToken("testtoken");

        // Default Mock: Tenant Config Found
        when(tenantTwilioConfigRepository.findByPhoneNumber(anyString())).thenReturn(Optional.of(mockConfig));
    }

    @Test
    void testNewUserStartsAtLanguageSelection() {
        String mobile = "9999999999"; // raw mobile
        String from = "whatsapp:+91" + mobile;
        String to = "whatsapp:+918888888888";

        when(sessionRepository.findByMobileNumberAndAdminId(anyString(), anyLong())).thenReturn(Optional.empty());
        when(sessionRepository.save(any(ChatbotSession.class))).thenAnswer(i -> {
            ChatbotSession s = (ChatbotSession) i.getArguments()[0];
            s.setLastUpdated(LocalDateTime.now());
            return s;
        });

        whatsappService.processMessage(from, to, "Hi", 0, null);

        verify(sessionRepository, atLeast(1))
                .save(argThat(session -> session.getState() == ChatbotState.LANGUAGE_SELECTION &&
                        session.getMobileNumber().equals(from) &&
                        session.getAdmin().getId().equals(1L)));
    }

    @Test
    void testLanguageSelection_Marathi() {
        ChatbotSession session = new ChatbotSession();
        session.setMobileNumber("+919999999999");
        session.setAdmin(mockAdmin);
        session.setState(ChatbotState.LANGUAGE_SELECTION);
        session.setLastUpdated(LocalDateTime.now());

        when(departmentRepository.findByAdminId(1L)).thenReturn(List.of(createDept(1L, "Dept1", "विभाग १")));
        when(sessionRepository.findByMobileNumberAndAdminId(anyString(), anyLong())).thenReturn(Optional.of(session));

        whatsappService.processMessage("whatsapp:+919999999999", "whatsapp:+918888888888", "2", 0, null);

        assert "mr".equals(session.getLanguage());
        assert session.getState() == ChatbotState.DEPT_SELECTION;
    }

    @Test
    void testDepartmentSelection() {
        ChatbotSession session = new ChatbotSession();
        session.setMobileNumber("+919999999999");
        session.setAdmin(mockAdmin);
        session.setState(ChatbotState.DEPT_SELECTION);
        session.setLanguage("mr");
        session.setLastUpdated(LocalDateTime.now());

        Department d1 = createDept(1L, "Sanitation", "स्वच्छता");
        when(departmentRepository.findByAdminId(1L)).thenReturn(List.of(d1));
        when(complaintTypeService.getComplaintTypesByDepartment(1L))
                .thenReturn(List.of(createType(1L, "Garbage", "कचरा")));
        when(sessionRepository.findByMobileNumberAndAdminId(anyString(), anyLong())).thenReturn(Optional.of(session));

        whatsappService.processMessage("whatsapp:+919999999999", "whatsapp:+918888888888", "1", 0, null);

        assert session.getState() == ChatbotState.TYPE_SELECTION;
        assert session.getTempData().contains("deptId");
    }

    @Test
    void testFullFlowSubmission() throws Exception {
        ChatbotSession session = new ChatbotSession();
        session.setMobileNumber("+919999999999");
        session.setAdmin(mockAdmin);
        session.setState(ChatbotState.LOCATION_INPUT);
        session.setLanguage("mr");
        session.setLastUpdated(LocalDateTime.now());
        session.setTempData("{\"deptId\":1,\"typeId\":1,\"name\":\"John\",\"desc\":\"Issue\",\"photo\":\"url\"}");

        when(sessionRepository.findByMobileNumberAndAdminId(anyString(), anyLong())).thenReturn(Optional.of(session));

        ComplaintDTO mockCreated = new ComplaintDTO();
        mockCreated.setComplaintNo("CMP123");
        when(complaintService.submitComplaint(any(ComplaintSubmissionDTO.class))).thenReturn(mockCreated);

        whatsappService.processMessage("whatsapp:+919999999999", "whatsapp:+918888888888", "Nashik Road", 0, null);

        verify(complaintService, times(1)).submitComplaint(any(ComplaintSubmissionDTO.class));
        assert session.getState() == ChatbotState.LANGUAGE_SELECTION; // Should reset
    }

    // Helper: Pass Tenant Config explicitly to simulate context if needed,
    // but processMessage does lookup internally.

    private Department createDept(Long id, String name, String nameMr) {
        Department d = new Department();
        d.setId(id);
        d.setName(name);
        d.setNameMr(nameMr);
        d.setActive(true);
        d.setChatbotEnabled(true);
        return d;
    }

    private ComplaintTypeDTO createType(Long id, String nameEn, String nameMr) {
        ComplaintTypeDTO t = new ComplaintTypeDTO();
        t.setId(id);
        t.setNameEn(nameEn);
        t.setNameMr(nameMr);
        return t;
    }
}
