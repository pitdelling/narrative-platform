package com.narrativeplatform.app.auth.services;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.auth.models.requests.ChangePasswordRequest;
import com.narrativeplatform.app.auth.models.requests.LoginRequest;
import com.narrativeplatform.app.auth.models.requests.RegisterFromInviteRequest;
import com.narrativeplatform.app.auth.models.requests.RegisterNarratorRequest;
import com.narrativeplatform.app.auth.models.responses.AuthResponse;
import com.narrativeplatform.app.auth.models.responses.CurrentUserResponse;
import com.narrativeplatform.app.auth.repositories.UserRepository;
import com.narrativeplatform.app.invitation.services.InvitationService;
import com.narrativeplatform.security.CurrentUserService;
import com.narrativeplatform.security.JwtService;
import com.narrativeplatform.shared.exceptions.ConflictException;
import com.narrativeplatform.shared.exceptions.ForbiddenException;
import com.narrativeplatform.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;
    private final InvitationService invitationService;

    @Transactional
    public AuthResponse registerNarrator(final RegisterNarratorRequest request) {
        final var normalizedUsername = request.username().trim().toLowerCase(Locale.ROOT);
        ensureUsernameAvailable(normalizedUsername);
        final var user = new UserEntity(
                normalizedUsername,
                request.displayName().trim(),
                passwordEncoder.encode(request.password())
        );
        final var saved = userRepository.save(user);
        return saved.toAuthResponse(jwtService.create(saved.getId(), saved.getUsername()));
    }

    @Transactional
    public AuthResponse registerFromInvite(final RegisterFromInviteRequest request) {
        final var normalizedUsername = request.username().trim().toLowerCase(Locale.ROOT);
        ensureUsernameAvailable(normalizedUsername);
        invitationService.validateForRegistration(request.token());
        final var user = userRepository.save(new UserEntity(
                normalizedUsername,
                request.displayName().trim(),
                passwordEncoder.encode(request.password())
        ));
        invitationService.acceptForUser(request.token(), user.getId());
        return user.toAuthResponse(jwtService.create(user.getId(), user.getUsername()));
    }

    public AuthResponse login(final LoginRequest request) {
        final var user = userRepository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> new ForbiddenException("Invalid username or password."));
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ForbiddenException("Invalid username or password.");
        }
        return user.toAuthResponse(jwtService.create(user.getId(), user.getUsername()));
    }

    public CurrentUserResponse currentUser() {
        final var authenticatedUser = currentUserService.require();
        final var user = userRepository.findById(authenticatedUser.id())
                .orElseThrow(() -> new NotFoundException("User not found."));
        return user.toCurrentUserResponse();
    }

    @Transactional
    public void changePassword(final ChangePasswordRequest request) {
        final var authenticatedUser = currentUserService.require();
        final var user = userRepository.findById(authenticatedUser.id())
                .orElseThrow(() -> new NotFoundException("User not found."));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ForbiddenException("The current password is incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ConflictException("The new password must be different from the current password.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private void ensureUsernameAvailable(final String username) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username already exists.");
        }
    }
}
