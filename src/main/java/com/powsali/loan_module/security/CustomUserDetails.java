package com.powsali.loan_module.security;

import com.powsali.loan_module.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

// CustomUserDetails.java
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override public String getPassword() { return user.getPassword(); }

    @Override public String getUsername() { return user.getUsername(); }

    @Override public boolean isAccountNonExpired() { return true; }

    @Override public boolean isAccountNonLocked() { return true; }

    @Override public boolean isCredentialsNonExpired() { return true; }

    @Override public boolean isEnabled() { return true; }

    public User getUser() { return user; }
}

