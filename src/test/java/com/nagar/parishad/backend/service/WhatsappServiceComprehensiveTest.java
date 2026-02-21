package com.nagar.parishad.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nagar.parishad.backend.dto.ComplaintSubmissionDTO;
import com.nagar.parishad.backend.entity.ChatbotSession;
import com.nagar.parishad.backend.entity.Department;
import com.nagar.parishad.backend.entity.TenantTwilioConfig;
import com.nagar.parishad.backend.entity.User;
import com.nagar.parishad.backend.enums.ChatbotState;
import com.nagar.parishad.backend.repository.ChatbotSessionRepository;
import com.nagar.parishad.backend.repository.ComplaintTypeRepository;
import com.nagar.parishad.backend.repository.DepartmentRepository;
import com.nagar.parishad.backend.repository.TenantTwilioConfigRepository;
import com.nagar.parishad.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WhatsappServiceComprehensiveTest {

    @Mock
    private ChatbotSessionRepository sessionRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ComplaintTypeRepository complaintTypeRepository;

    @Mock
    private ComplaintService complaintService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantTwilioConfigRepository tenantTwilioConfigRepository;

    @Mock
    private com.nagar.parishad.backend.repository.ChatbotConfigRepository configRepository;

    @InjectMocks
    private WhatsappService whatsappService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private User admin;
    private TenantTwilioConfig tenantConfig;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@test.com");
        admin.setRole(com.nagar.parishad.backend.enums.Role.ADMIN);

        tenantConfig = new TenantTwilioConfig();
        tenantConfig.setAdmin(admin);
        tenantConfig.setPhoneNumber("whatsapp:+918888888888");

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(tenantTwilioConfigRepository.findByPhoneNumber(anyString())).thenReturn(Optional.of(tenantConfig));

        // Mock Departments
        Department d1 = new Department();
        d1.setId(1L);
        d1.setName("Sanitation");
        d1.setSubQuestions("[\"Garbage Not Picked\", \"Drainage Issue\"]");
        d1.setActive(true);
        d1.setChatbotEnabled(true);
        Department d2 = new Department();
        d2.setId(2L);
        d2.setName("Water");
        d2.setActive(true);
        d2.setChatbotEnabled(true);
        when(departmentRepository.findByAdminId(1L)).thenReturn(java.util.Arrays.asList(d1, d2));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(d1));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(d2));
    }

    // -----------------------------------------------------------------------------------------
    // TEST CASE SOURCE PROVIDERS
    // -----------------------------------------------------------------------------------------

    static Stream<Arguments> provideLanguageInputs() {
        return Stream.of(
                Arguments.of("1", "en"), // Valid English
                Arguments.of("2", "mr"), // Valid Marathi
                Arguments.of("3", "hi"), // Valid Hindi
                Arguments.of("english", "en"), // Text Input
                Arguments.of("marathi", "mr"),
                Arguments.of("invalid", null) // Invalid
        );
    }

    @ParameterizedTest
    @MethodSource("provideLanguageInputs")
    void testLanguageSelection(String input, String expectedLang) {
        String mobile = "whatsapp:+919999999999";

        // Setup Session in LANG_SELECTION state
        ChatbotSession session = new ChatbotSession();
        session.setMobileNumber(mobile);
        session.setState(ChatbotState.LANGUAGE_SELECTION);
        session.setAdmin(admin);

        when(sessionRepository.findByMobileNumberAndAdminId(mobile, 1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ChatbotSession.class))).thenAnswer(i -> i.getArgument(0));

        whatsappService.simulate(mobile, input, 0, null, 1L);

        if (expectedLang != null) {
            // Should transition to DYNAMIC_FLOW (or next state)
            assert (session.getLanguage().equals(expectedLang));
            assert (session.getState() == ChatbotState.DYNAMIC_FLOW);
        } else {
            // Should remain in LANGUAGE_SELECTION
            assert (session.getState() == ChatbotState.LANGUAGE_SELECTION);
        }
    }

    static Stream<Arguments> provideDepartmentInputs() {
        return Stream.of(
                Arguments.of("1", 1L, true), // Sanitation
                Arguments.of("2", 2L, true), // Water
                Arguments.of("99", null, false), // Invalid ID
                Arguments.of("Sanitation", 1L, true), // Name Match
                Arguments.of("Garbage", null, false) // Invalid Name
        );
    }

    @ParameterizedTest
    @MethodSource("provideDepartmentInputs")
    void testDepartmentSelection(String input, Long expectedDeptId, boolean shouldTransition) {
        String mobile = "whatsapp:+919999999999";

        ChatbotSession session = new ChatbotSession();
        session.setMobileNumber(mobile);
        session.setState(ChatbotState.DYNAMIC_FLOW);
        session.setAdmin(admin);
        session.setLanguage("en");

        // Mock tempData to simulate being at START node
        try {
            String tempDataJson = "{\"currentNodeId\":\"start\", \"waitingForInput\":true}";
            session.setTempData(tempDataJson);
        } catch (Exception e) {
        }

        when(sessionRepository.findByMobileNumberAndAdminId(mobile, 1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ChatbotSession.class))).thenAnswer(i -> i.getArgument(0));

        whatsappService.simulate(mobile, input, 0, null, 1L);

        try {
            if (shouldTransition) {
                // Verify departmentId is stored in tempData
                String json = session.getTempData();
                assert (json.contains("\"departmentId\":\"" + expectedDeptId + "\""));
            } else {
                // Should still be at start or re-prompting
                // (Implementation detail: invalid input usually re-prompts same node)
                String json = session.getTempData();
                assert (json.contains("\"currentNodeId\":\"start\""));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- SUB ISSUE TESTS ---
    static Stream<Arguments> provideSubIssueInputs() {
        return Stream.of(
                Arguments.of("1", true), // Option 1
                Arguments.of("General Issue", true), // Text
                Arguments.of("My garbage is not picked", true) // Free text is often accepted if flow allows or mapped
        );
    }

    @ParameterizedTest
    @MethodSource("provideSubIssueInputs")
    void testSubIssueSelection(String input, boolean shouldAccept) {
        String mobile = "whatsapp:+919999999999";
        ChatbotSession session = new ChatbotSession();
        session.setMobileNumber(mobile);
        session.setState(ChatbotState.DYNAMIC_FLOW);
        session.setAdmin(admin);
        session.setTempData("{\"currentNodeId\":\"dept_1\", \"waitingForInput\":true, \"departmentId\":\"1\"}"); // At
                                                                                                                 // Dept
                                                                                                                 // 1
                                                                                                                 // node

        when(sessionRepository.findByMobileNumberAndAdminId(mobile, 1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ChatbotSession.class))).thenAnswer(i -> i.getArgument(0));

        whatsappService.simulate(mobile, input, 0, null, 1L);

        if (shouldAccept) {
            assert (session.getTempData().contains("\"description\":\""));
            assert (session.getTempData().contains("\"currentNodeId\":\"ask_name\""));
        }
    }

    // --- FULL FLOW END-TO-END SIMULATION ---
    // This effectively runs multiple steps to reach completion
    static Stream<Arguments> provideFullFlows() {
        return Stream.of(
                Arguments.of("en", "1", "1", "John Doe", "Big Heap", "Skip", "Pune"),
                Arguments.of("mr", "2", "No Water", "Ramesh", "Pipe broke", "http://img.com/a.jpg", "Mumbai"),
                Arguments.of("en", "1", "General", "Anon", "Details", "Skip", "Nasik"));
    }

    @ParameterizedTest
    @MethodSource("provideFullFlows")
    void testEndToEndFlow(String lang, String dept, String subIssue, String name, String details, String photo,
            String location) {
        String mobile = "whatsapp:+917777777777";
        ChatbotSession session = new ChatbotSession();
        session.setMobileNumber(mobile);
        session.setAdmin(admin);
        // Simulate New User logic implicitly by Repo returning null first?
        // For simplicity, we assume session created and at LANG step.
        session.setState(ChatbotState.LANGUAGE_SELECTION);

        when(sessionRepository.findByMobileNumberAndAdminId(mobile, 1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ChatbotSession.class))).thenAnswer(i -> i.getArgument(0));

        // 1. Language
        whatsappService.simulate(mobile, lang, 0, null, 1L);
        assert (session.getState() == ChatbotState.DYNAMIC_FLOW);

        // 2. Department
        whatsappService.simulate(mobile, dept, 0, null, 1L);
        assert (session.getTempData().contains("\"departmentId\""));

        // 3. Sub Issue
        whatsappService.simulate(mobile, subIssue, 0, null, 1L);
        assert (session.getTempData().contains("\"description\""));

        // 4. Name
        whatsappService.simulate(mobile, name, 0, null, 1L);
        assert (session.getTempData().contains("\"citizenName\""));

        // 5. Details
        whatsappService.simulate(mobile, details, 0, null, 1L);
        assert (session.getTempData().contains("\"description_detail\""));

        // 6. Photo
        whatsappService.simulate(mobile, photo, 0, null, 1L);
        assert (session.getTempData().contains("\"photo\""));

        // 7. Location (Triggers Submit)
        whatsappService.simulate(mobile, location, 0, null, 1L);

        assert (session.getState() == ChatbotState.COMPLETED);
        verify(complaintService, times(1)).submitComplaint(any(ComplaintSubmissionDTO.class));
    }
}
