package com.hospital.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.common.result.ResultCode;
import com.hospital.dto.request.LoginRequest;
import com.hospital.dto.request.RegisterRequest;
import com.hospital.entity.User;
import com.hospital.config.SystemSettingManager;
import com.hospital.mapper.AppointmentMapper;
import com.hospital.mapper.DoctorMapper;
import com.hospital.mapper.UserMapper;
import com.hospital.service.OssService;
import com.hospital.service.SystemService;
import com.hospital.util.JwtUtil;
import com.hospital.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.profiles.active=test")
class UserAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private DoctorMapper doctorMapper;

    @MockBean
    private AppointmentMapper appointmentMapper;

    @MockBean
    private RedisUtil redisUtil;

    @MockBean
    private OssService ossService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private SystemSettingManager systemSettingManager;

    @MockBean
    private SystemService systemService;

    @BeforeEach
    void setupMocks() {
        Mockito.lenient().when(redisUtil.get(anyString())).thenReturn(null);
        Mockito.lenient().doNothing().when(redisUtil).set(anyString(), any());
        Mockito.lenient().doNothing().when(redisUtil).set(anyString(), any(), anyLong(), any());
        Mockito.lenient().when(redisUtil.delete(anyString())).thenReturn(true);
        Mockito.lenient().when(redisUtil.deleteByPattern(anyString())).thenReturn(0L);
        Mockito.lenient().when(systemSettingManager.getBoolean(any(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(1));
        Mockito.lenient().when(systemSettingManager.getInteger(any(), anyInt())).thenAnswer(invocation -> invocation.getArgument(1));
        Mockito.lenient().doNothing().when(systemService).recordOperationLog(any());
    }

    @Test
    void loginSuccessFlow() throws Exception {
        User user = buildUserEntity(1L, "zhangsan", passwordEncoder.encode("123456"));
        when(userMapper.selectByUsername("zhangsan")).thenReturn(user);
        when(userMapper.update(any(), any())).thenReturn(1);
        when(jwtUtil.generateToken(eq(1L), eq("zhangsan"), eq(1))).thenReturn("token-123");

        LoginRequest request = new LoginRequest();
        request.setUsername("zhangsan");
        request.setPassword("123456");

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.username").value("zhangsan"))
                .andExpect(jsonPath("$.data.token").value("token-123"));
    }

    @Test
    void loginShouldFailWithWrongPassword() throws Exception {
        User user = buildUserEntity(2L, "lisi", passwordEncoder.encode("correctPwd"));
        when(userMapper.selectByUsername("lisi")).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("lisi");
        request.setPassword("wrongPwd");

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.USERNAME_OR_PASSWORD_ERROR.getCode()));
    }

    @Test
    void registerSuccessFlow() throws Exception {
        when(userMapper.selectByUsername("newuser")).thenReturn(null);
        when(userMapper.selectByPhone("13812345678")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("Pwd@1234");
        request.setConfirmPassword("Pwd@1234");
        request.setRealName("新用户");
        request.setPhone("13812345678");

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.message").value("注册成功"));
    }

    @Test
    void registerShouldFailWhenUsernameExists() throws Exception {
        User existing = buildUserEntity(5L, "duplicate", passwordEncoder.encode("Pwd@1234"));
        when(userMapper.selectByUsername("duplicate")).thenReturn(existing);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("duplicate");
        request.setPassword("Pwd@1234");
        request.setConfirmPassword("Pwd@1234");
        request.setRealName("重复用户");
        request.setPhone("13899998888");

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.USER_ALREADY_EXISTS.getCode()));
    }

    private User buildUserEntity(Long id, String username, String encodedPassword) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setStatus(1);
        user.setRoleType(1);
        user.setRealName("Test");
        user.setPhone("13800000000");
        return user;
    }
}


