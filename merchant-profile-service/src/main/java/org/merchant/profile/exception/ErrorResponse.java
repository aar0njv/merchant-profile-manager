


package org.merchant.profile.exception;
import java.time.Instant;

public record ErrorResponse(
    int status,
    String title,
    String detail,
    Instant timestamp
) {
        public ErrorResponse(int status, String title, String detail) {
            this(status, title, detail, Instant.now());
        }
}