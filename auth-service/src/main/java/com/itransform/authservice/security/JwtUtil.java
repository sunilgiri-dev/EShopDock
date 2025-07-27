package com.itransform.authservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class JwtUtil {
    private static final Logger logger = Logger.getLogger(JwtUtil.class.getName());
    
    @Value("${jwt.secret:verySecureRandomKeyForDevelopmentOnlyThisKeyMustBeAtLeast64BytesLongForHS512AlgorithmToWorkProperly}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:43200000}")
    private long jwtExpirationMs; // Default: 12 hours

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            logger.log(Level.WARNING, "Invalid JWT signature: {0}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.log(Level.WARNING, "Invalid JWT token: {0}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.log(Level.WARNING, "JWT token is expired: {0}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.log(Level.WARNING, "JWT token is unsupported: {0}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "JWT claims string is empty: {0}", e.getMessage());
        }
        return false;
    }

    public String getUsernameFromToken(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                    .parseClaimsJws(token).getBody().getSubject();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error extracting username from token: {0}", e.getMessage());
            throw e;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                    .parseClaimsJws(token).getBody().get("role", String.class);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error extracting role from token: {0}", e.getMessage());
            throw e;
        }
    }
}
