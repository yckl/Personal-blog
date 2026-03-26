package com.blog.server.service;

import com.blog.server.dto.request.LoginRequest;
import com.blog.server.entity.SysUser;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.SysUserMapper;
import com.blog.server.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // Setup Redis mock to return operations correctly
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void login_TooManyFailedAttempts_ThrowsExceptionAndLocksAccount() {
        // Arrange
        String username = "attacker";
        String errKey = "login:err:" + username;
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword("wrongpassword");

        // Mock Redis to simulate 5 failed attempts already existing
        when(valueOperations.get(errKey)).thenReturn("5");

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(request, "127.0.0.1");
        });

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("Account locked for 15 minutes"));
        
        // Verify mapper was never even called because it was blocked at Redis bounds check
        verify(sysUserMapper, never()).selectOne(any());
    }

    @Test
    void login_InvalidPassword_IncrementsCounter() {
        // Arrange
        String username = "user1";
        String password = "wrongpass";
        String errKey = "login:err:" + username;
        
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        SysUser mockUser = new SysUser();
        mockUser.setId(1L);
        mockUser.setUsername(username);
        mockUser.setPassword("encoded_correct_pass");
        
        // Redis has 0 failed attempts
        when(valueOperations.get(errKey)).thenReturn(null);
        
        // User exists in DB
        when(sysUserMapper.selectOne(any())).thenReturn(mockUser);
        
        // Passwords don't match
        when(passwordEncoder.matches(eq(password), anyString())).thenReturn(false);
        
        // Expire key returns -1 (no expiration set yet)
        when(redisTemplate.getExpire(errKey)).thenReturn(-1L);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(request, "127.0.0.1");
        });
        
        assertEquals(401, exception.getCode());
        
        // Verify Redis incremented the counter and set the 15-minute TTL
        verify(valueOperations).increment(errKey);
        verify(redisTemplate).expire(errKey, 15, TimeUnit.MINUTES);
    }
}
