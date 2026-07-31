package com.financepulse.engine.adapters.out.security;

import com.financepulse.engine.application.ports.TokenService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenServiceAdapter implements TokenService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    private final SecretKey key;

    public JwtTokenServiceAdapter(@Value("${financepulse.jwt.secret}") String secret) {
        // O segredo configurado pode ter qualquer tamanho; deriva-se uma chave de
        // 256 bits via SHA-256 para satisfazer o comprimento mínimo exigido pelo
        // HS256 (jjwt rejeita chaves fracas por padrão), sem impor formato ao operador.
        this.key = Keys.hmacShaKeyFor(sha256(secret));
    }

    private static byte[] sha256(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível na JVM.", e);
        }
    }

    @Override
    public String issue(String userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ACCESS_TOKEN_TTL)))
                .signWith(key)
                .compact();
    }

    @Override
    public Optional<String> verify(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Optional.ofNullable(subject);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
