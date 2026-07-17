package ai.nova.platform.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedUser implements UserDetails {

    private final UUID userId;
    private final UUID organizationId;
    private final String email;
    private final String displayName;
    private final List<String> roles;
    private final boolean enabled;

    public AuthenticatedUser(
            UUID userId,
            UUID organizationId,
            String email,
            String displayName,
            List<String> roles,
            boolean enabled) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.email = email;
        this.displayName = displayName;
        this.roles = List.copyOf(roles);
        this.enabled = enabled;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
