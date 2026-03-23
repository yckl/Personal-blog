package com.blog.server.security;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator(32);
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

    /**
     * Generate a new TOTP secret for a user.
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * Generate the otpauth:// URI for QR code generation.
     */
    public String generateQrCodeUri(String secret, String account, String issuer) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                issuer, account, secret, issuer);
    }

    /**
     * Verify a TOTP code against a secret.
     */
    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }
}
