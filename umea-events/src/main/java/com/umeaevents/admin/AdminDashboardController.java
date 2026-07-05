package com.umeaevents.admin;

import com.umeaevents.event.EventStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminStatsService statsService;
    private final AdminEventService eventService;
    private final AdminUserService userService;

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return statsService.stats();
    }

    @GetMapping("/events")
    public Page<AdminEventResponse> listEvents(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) UUID venueId,
            @RequestParam(required = false) UUID categoryId,
            Pageable pageable) {
        return eventService.listEvents(status, venueId, categoryId, pageable);
    }

    @GetMapping("/users")
    public Page<AdminUserResponse> listUsers(Pageable pageable) {
        return userService.listUsers(pageable);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        return userService.createUser(request);
    }

    @PatchMapping("/users/{id}/role")
    public AdminUserResponse changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request,
            Principal principal) {
        return userService.changeRole(id, request, principal.getName());
    }
}
