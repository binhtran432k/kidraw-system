package com.app.kidspainting.util;

import com.app.kidspainting.entity.Role;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AuthUtil {
    private final MessageSource messageSource;

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.header}")
    private String authHeader;
    @Value("${jwt.token.prefix}")
    private String tokenPrefix;
    @Value("${jwt.authorities_key}")
    private String authKey;
    @Value("${jwt.token.access_validity}")
    private long accessTokenDuration;
    @Value("${jwt.token.refresh_validity}")
    private long refreshTokenDuration;

    public String generateAccessToken(String username, List<String> roles)
            throws IllegalArgumentException, JWTCreationException {
        return JWT.create()
                .withSubject("User Details")
                .withClaim("username", username)
                .withClaim(authKey, roles)
                .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenDuration))
                .withIssuedAt(new Date())
                .withIssuer("kidspainting/backdend/kidspainting")
                .sign(Algorithm.HMAC256(secret));
    }

    public String generateRefreshToken(String username) throws IllegalArgumentException, JWTCreationException {
        return JWT.create()
                .withSubject("User Details")
                .withClaim("username", username)
                .withExpiresAt(new Date(System.currentTimeMillis() + refreshTokenDuration))
                .withIssuedAt(new Date())
                .withIssuer("kidspainting/backdend/kidspainting")
                .sign(Algorithm.HMAC256(secret));
    }

    public DecodedJWT validateTokenAndRetrieveDecoded(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                .withSubject("User Details")
                .withIssuer("kidspainting/backdend/kidspainting")
                .build();
        return verifier.verify(token);
    }

    public Optional<String> getJwtFromBearer(String authHeader) {
        if (authHeader != null && !authHeader.isBlank() && authHeader.startsWith(tokenPrefix)) {
            String jwt = authHeader.substring(tokenPrefix.length());
            if (jwt == null || jwt.isBlank()) {
                messageSource.getMessage("exception.jwt.bearer_invalid", null, LocaleContextHolder.getLocale());
                return Optional.empty();
            } else {
                return Optional.ofNullable(jwt);
            }
        } else {
            messageSource.getMessage("exception.jwt.bearer_invalid", null, LocaleContextHolder.getLocale());
            return Optional.empty();
        }
    }

    public UsernamePasswordAuthenticationToken getAuthenticationFromToken(final String jwt) {
        DecodedJWT decodedJWT = validateTokenAndRetrieveDecoded(jwt);
        // Get authorization info
        String username = decodedJWT.getSubject();
        String[] roles = decodedJWT.getClaim(authKey).asArray(String.class);

        Collection<SimpleGrantedAuthority> authorities = parseAuthoritiesFromRoleNames(roles);
        return new UsernamePasswordAuthenticationToken(username, "", authorities);
    }

    public Collection<SimpleGrantedAuthority> parseAuthoritiesFromRoleNames(String[] roleNames) {
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        Arrays.stream(roleNames).forEach(role -> {
            authorities.add(new SimpleGrantedAuthority(role));
        });
        return authorities;
    }

    public List<String> parseRoleNamesFromAuthorities(Collection<? extends GrantedAuthority> authorities) {
        List<String> roleNames = authorities.stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return roleNames;
    }

    public Collection<SimpleGrantedAuthority> parseAuthoritiesFromRoles(Collection<Role> roles) {
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> {
            authorities.add(new SimpleGrantedAuthority(role.getName()));
        });
        return authorities;
    }
}