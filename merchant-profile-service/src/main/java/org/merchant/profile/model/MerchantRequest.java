
package org.merchant.profile.model;

public record MerchantRequest(
        String name,
        String email,
        MerchantStatus status
) {}