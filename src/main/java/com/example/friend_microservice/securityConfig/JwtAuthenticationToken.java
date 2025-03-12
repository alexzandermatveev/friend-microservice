package com.example.friend_microservice.securityConfig;

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String userId;
    private final String token;


    public JwtAuthenticationToken(String userId, String token) {

        super(null);
        this.userId = userId;
        this.token = token;
        setAuthenticated(true);
    }


    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

}
