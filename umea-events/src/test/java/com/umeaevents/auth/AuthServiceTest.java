package com.umeaevents.auth;

import com.umeaevents.auth.dto.RegisterRequest;
import com.umeaevents.user.Role;
import com.umeaevents.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService service;

    @Test
    void register_adminRole_isRejectedAndNothingSaved() {
        assertThatThrownBy(() ->
                service.register(new RegisterRequest("a@b.com", "password123", Role.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_userRole_isAllowed() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(jwtService.generateRefreshTokenValue()).thenReturn("refresh");

        var result = service.register(new RegisterRequest("a@b.com", "password123", Role.USER));

        assertThat(result.accessToken()).isEqualTo("access");
        verify(userRepository).save(any());
    }

    @Test
    void register_restaurantRole_isAllowed() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(jwtService.generateRefreshTokenValue()).thenReturn("refresh");

        var result = service.register(new RegisterRequest("r@b.com", "password123", Role.RESTAURANT));

        assertThat(result.accessToken()).isEqualTo("access");
        verify(userRepository).save(any());
    }
}
