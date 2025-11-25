package com.hospital.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.common.result.Result;
import com.hospital.dto.request.LoginRequest;
import com.hospital.dto.request.RegisterRequest;
import com.hospital.dto.response.LoginResponse;
import com.hospital.service.OssService;
import com.hospital.service.UserService;
import com.hospital.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private OssService ossService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void login_shouldReturnSuccessResult() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .token("token-123")
                .id(1L)
                .username("zhangsan")
                .roleType(1)
                .phone("138****5678")
                .build();
        when(userService.login(any(LoginRequest.class))).thenReturn(Result.success("登录成功", response));

        LoginRequest request = new LoginRequest();
        request.setUsername("zhangsan");
        request.setPassword("123456");

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.username", is("zhangsan")))
                .andExpect(jsonPath("$.data.token", is("token-123")));
    }

    @Test
    void register_shouldReturnServiceResult() throws Exception {
        when(userService.register(any(RegisterRequest.class))).thenReturn(Result.success("注册成功"));

        RegisterRequest request = new RegisterRequest();
        request.setUsername("lisi");
        request.setPassword("Pwd@1234");
        request.setConfirmPassword("Pwd@1234");
        request.setRealName("李四");
        request.setPhone("13800000000");

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("注册成功")));
    }
}


