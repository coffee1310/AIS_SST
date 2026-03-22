package org.example.ais_sst;

import org.example.ais_sst.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import lombok.Data;
import java.util.Collection;
import java.util.Collections;

@Data
public class CustomUserDetails implements UserDetails {

    private Long id;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private Boolean isActive;
    private Boolean isBanned;
    private String name;
    private String surname;

    public static CustomUserDetails fromUser(User user) {
        CustomUserDetails details = new CustomUserDetails();
        details.setId(user.getId());
        details.setEmail(user.getStudentEmail());
        details.setPassword(user.getPassword());
        details.setIsActive(user.getIsActive());
        details.setIsBanned(user.getIsBanned());
        details.setName(user.getName());
        details.setSurname(user.getSurname());

        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().getTitle().toUpperCase());
        details.setAuthorities(Collections.singletonList(authority));

        return details;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
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
        return !isBanned;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive && !isBanned;
    }
}