package com.umeaevents.admin;

import com.umeaevents.common.exception.ResourceNotFoundException;
import com.umeaevents.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(AdminUserResponse::from);
    }

    @Transactional
    public AdminUserResponse changeRole(UUID targetId, ChangeRoleRequest request, String adminEmail) {
        var admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));
        if (admin.getId().equals(targetId)) {
            throw new IllegalArgumentException("Cannot change your own role");
        }
        var target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        target.setRole(request.role());
        return AdminUserResponse.from(userRepository.save(target));
    }
}
