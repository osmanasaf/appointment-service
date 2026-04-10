package com.asaf.appointment_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret:defaultSecretKeyThatShouldBeChangedInProduction123456}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    public String generateToken(Long userId, String email, String role, Long businessId, Long employeeId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .claim("businessId", businessId)
            .claim("employeeId", employeeId)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("email", String.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    public Long getBusinessIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object businessId = claims.get("businessId");
        return businessId != null ? Long.parseLong(businessId.toString()) : null;
    }

    public Long getEmployeeIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object employeeId = claims.get("employeeId");
        return employeeId != null ? Long.parseLong(employeeId.toString()) : null;
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Geçersiz JWT token");
        } catch (ExpiredJwtException e) {
            log.error("JWT token süresi dolmuş");
        } catch (UnsupportedJwtException e) {
            log.error("Desteklenmeyen JWT token");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims boş");
        }
        return false;
    }

    public long getExpirationInSeconds() {
        return jwtExpiration / 1000;
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
