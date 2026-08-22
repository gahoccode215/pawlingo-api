package com.pawlingo.api.auth.service.impl;

import com.pawlingo.api.auth.entity.RefreshToken;
import com.pawlingo.api.auth.repository.RefreshTokenRepository;
import com.pawlingo.api.auth.service.RefreshTokenIssuance;
import com.pawlingo.api.auth.service.RefreshTokenService;
import com.pawlingo.api.auth.service.RotatedRefreshToken;
import com.pawlingo.api.common.exception.BusinessException;
import com.pawlingo.api.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirationSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-expiration-seconds}") long refreshExpirationSeconds) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    @Override
    @Transactional
    public RefreshTokenIssuance issue(UUID userId) {
        Issued issued = createAndPersistToken(userId);
        return new RefreshTokenIssuance(issued.plaintext(), refreshExpirationSeconds);
    }

    @Override
    @Transactional
    public RotatedRefreshToken rotate(String presentedToken) {
        RefreshToken existing = refreshTokenRepository
                .findByTokenHash(hashToken(presentedToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (existing.getRevokedAt() != null) {
            // Already rotated away once before - presenting it again signals theft.
            revokeAllActiveTokensFor(existing.getUserId());
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            existing.setRevokedAt(Instant.now());
            refreshTokenRepository.save(existing);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Issued rotated = createAndPersistToken(existing.getUserId());
        existing.setRevokedAt(Instant.now());
        existing.setReplacedByTokenId(rotated.entity().getId());
        refreshTokenRepository.save(existing);

        return new RotatedRefreshToken(
                existing.getUserId(), new RefreshTokenIssuance(rotated.plaintext(), refreshExpirationSeconds));
    }

    @Override
    @Transactional
    public void revoke(String presentedToken) {
        refreshTokenRepository.findByTokenHash(hashToken(presentedToken)).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    private void revokeAllActiveTokensFor(UUID userId) {
        Instant now = Instant.now();
        List<RefreshToken> active = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId);
        active.forEach(token -> token.setRevokedAt(now));
        refreshTokenRepository.saveAll(active);
    }

    private Issued createAndPersistToken(UUID userId) {
        String plaintext = generatePlaintextToken();
        Instant now = Instant.now();
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hashToken(plaintext))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(refreshExpirationSeconds))
                .build();
        return new Issued(refreshTokenRepository.save(token), plaintext);
    }

    private String generatePlaintextToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String plaintext) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private record Issued(RefreshToken entity, String plaintext) {}
}
