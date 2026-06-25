package com.umeaevents.auth;

import com.umeaevents.user.Role;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AdminBootstrap bootstrap;

    private void config(String email, String password) {
        ReflectionTestUtils.setField(bootstrap, "email", email);
        ReflectionTestUtils.setField(bootstrap, "password", password);
    }

    @Test
    void unsetConfig_createsNothing() {
        config("", "");
        bootstrap.run(null);
        verify(userRepository, never()).save(any());
    }

    @Test
    void newEmail_createsAdminWithEncodedPassword() {
        config("boss@umea.se", "secret123");
        when(userRepository.existsByEmail("boss@umea.se")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");

        bootstrap.run(null);

        var saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("boss@umea.se");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void existingEmail_isSkipped() {
        config("boss@umea.se", "secret123");
        when(userRepository.existsByEmail("boss@umea.se")).thenReturn(true);

        bootstrap.run(null);

        verify(userRepository, never()).save(any());
    }
}
