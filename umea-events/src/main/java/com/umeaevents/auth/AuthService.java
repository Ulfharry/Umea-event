package com.umeaevents.auth;

import com.umeaevents.auth.dto.LoginRequest;
import com.umeaevents.auth.dto.MeResponse;
import com.umeaevents.auth.dto.RefreshRequest;
import com.umeaevents.auth.dto.RegisterRequest;
import com.umeaevents.auth.dto.TokenResponse;
import com.umeaevents.common.exception.ResourceNotFoundException;
import com.umeaevents.user.Role;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        // Self-registration may never grant ADMIN. Admin rights are only assignable by an
        // existing admin via PATCH /api/v1/admin/users/{id}/role.
        if (request.role() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin-konton kan inte skapas via registrering");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("E-postadressen är redan registrerad");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Användare hittades inte: " + request.email()));

        refreshTokenRepository.revokeAllByUserId(user.getId());
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new ResourceNotFoundException("Refresh-token hittades inte"));

        if (stored.isRevoked() || stored.isExpired()) {
            throw new IllegalArgumentException("Refresh-token är ogiltigt eller utgånget");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser());
    }

    @Transactional(readOnly = true)
    public MeResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Användare hittades inte: " + email));
        return new MeResponse(user.getId(), user.getEmail(), user.getRole());
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshTokenValue();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(OffsetDateTime.now().plusDays(refreshTokenExpirationDays))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshTokenValue, accessTokenExpirationMs);
    }
}
