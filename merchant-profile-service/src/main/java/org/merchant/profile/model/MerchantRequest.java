
package org.merchant.profile.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MerchantRequest(
        @NotNull(message="Merchant ID cannot be blank") String id,
        @NotBlank(message="Name cannot be blank") String name,
        
        @Email(message="Must be valid email address")
        @NotBlank(message="Email cannot be blank") String email,
        
        MerchantStatus status
) {}