
package org.merchant.profile.model;

import java.time.Instant;
import java.util.UUID;

public record Merchant(
        String id,
        String name,
        String email,
        MerchantStatus status,
        Instant createdAt,
        Instant updatedAt
) {}

