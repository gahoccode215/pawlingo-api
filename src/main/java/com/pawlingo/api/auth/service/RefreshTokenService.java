package com.pawlingo.api.auth.service;

import java.util.UUID;

public interface RefreshTokenService {

    /** Issues a brand-new refresh token for a user (register/login/google) with no prior lineage. */
    RefreshTokenIssuance issue(UUID userId);

    /**
     * Validates a presented refresh token and rotates it: the old token is revoked and a new one
     * issued in its place. Reusing an already-rotated token revokes the user's entire active token
     * set (signals theft) before failing.
     */
    RotatedRefreshToken rotate(String presentedToken);

    /** Revokes a refresh token if it exists. Idempotent - never throws for an unknown/expired token. */
    void revoke(String presentedToken);
}
