package com.narrativeplatform.app.auth.models.entities;

import com.narrativeplatform.app.auth.models.responses.AuthResponse;
import com.narrativeplatform.app.auth.models.responses.CurrentUserResponse;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 40)
    private String username;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "password_hash", nullable = false)
    @ToString.Exclude
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AuthResponse toAuthResponse(final String token) {
        return new AuthResponse(token, id, username, displayName);
    }

    public CurrentUserResponse toCurrentUserResponse() {
        return new CurrentUserResponse(id, username, displayName);
    }

    public UserEntity(final String username, final String displayName, final String passwordHash) {
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
    }
}
