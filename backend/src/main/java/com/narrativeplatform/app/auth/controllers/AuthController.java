package com.narrativeplatform.app.auth.controllers;

import com.narrativeplatform.app.auth.models.requests.ChangePasswordRequest;
import com.narrativeplatform.app.auth.models.requests.LoginRequest;
import com.narrativeplatform.app.auth.models.requests.RegisterFromInviteRequest;
import com.narrativeplatform.app.auth.models.requests.RegisterNarratorRequest;
import com.narrativeplatform.app.auth.models.responses.AuthResponse;
import com.narrativeplatform.app.auth.models.responses.CurrentUserResponse;
import com.narrativeplatform.app.auth.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register-narrator")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse registerNarrator(@Valid @RequestBody final RegisterNarratorRequest request) {
        return authService.registerNarrator(request);
    }

    @PostMapping("/register-from-invite")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse registerFromInvite(@Valid @RequestBody final RegisterFromInviteRequest request) {
        return authService.registerFromInvite(request);
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody final LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    CurrentUserResponse currentUser() {
        return authService.currentUser();
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@Valid @RequestBody final ChangePasswordRequest request) {
        authService.changePassword(request);
    }
}
