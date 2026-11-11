package com.reqflow.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class LoginUser implements UserDetails {

    private Long id;
    private String username;
    private String password;
    private String realName;
    private Long deptId;
    private Integer status;
    private Integer mustChangePwd;
    private List<String> roles;
    private Set<String> permissions;
    private String dataScope;
    private LocalDateTime lockUntil;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<String> authorities = new LinkedHashSet<>();
        if (permissions != null) {
            authorities.addAll(permissions);
        }
        if (roles != null) {
            authorities.addAll(roles.stream().map(r -> "ROLE_" + r).collect(Collectors.toSet()));
        }
        return authorities.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockUntil == null || lockUntil.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
