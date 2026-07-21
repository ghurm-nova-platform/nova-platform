package ai.nova.platform.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedUser implements UserDetails {

    private final UUID userId;
    private final UUID organizationId;
    private final String email;
    private final String displayName;
    private final List<String> roles;
    private final List<String> permissions;
    private final boolean enabled;

    public AuthenticatedUser(
            UUID userId,
            UUID organizationId,
            String email,
            String displayName,
            List<String> roles,
            List<String> permissions,
            boolean enabled) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.email = email;
        this.displayName = displayName;
        this.roles = List.copyOf(roles);
        this.permissions = List.copyOf(permissions);
        this.enabled = enabled;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Stream<GrantedAuthority> roleAuthorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role));
        Stream<GrantedAuthority> permissionAuthorities = permissions.stream()
                .map(SimpleGrantedAuthority::new);
        return Stream.concat(roleAuthorities, permissionAuthorities).collect(Collectors.toUnmodifiableList());
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
