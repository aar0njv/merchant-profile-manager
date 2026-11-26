
package org.merchant.profile.model;

import java.time.Instant;
import java.util.UUID;

import org.merchant.profile.model.enums.MerchantStatus;

public record Merchant(
        String id,
        String name,
        String email,
        MerchantStatus status,
        Instant createdAt,
        Instant updatedAt
) {}

