
package org.merchant.profile.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.merchant.profile.model.Merchant;
import org.merchant.profile.model.MerchantStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class MerchantService {

    private final Map<UUID, Merchant> database = new ConcurrentHashMap<>();

    public List<Merchant> getAllMerchants() {
        return new ArrayList<>(database.values());
    }

    public Merchant getMerchantById(UUID id) {
        Merchant merchant = database.get(id);
        if(merchant == null) {
            throw new IllegalArgumentException("Merchant not Found");
        }
        return merchant;
    }

    public Merchant createMerchant(String name, String email) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Merchant merchant = new Merchant(id, name, email, MerchantStatus.PENDING, now, now);
        database.put(id, merchant);
        return merchant;
    }

    public Merchant updateMerchant(UUID id, String name, String email, MerchantStatus status) {
        Merchant existingMerchant = database.get(id);
        if (existingMerchant == null) {
            throw new IllegalArgumentException("Merchant not found");
        }
        Instant now = Instant.now();
        Merchant updatedMerchant = new Merchant(id, 
            name != null ? name : existingMerchant.name(), 
            email != null ? email : existingMerchant.email(), 
            status != null ? status : existingMerchant.status(), 
            existingMerchant.createdAt(), now);
            
        database.put(id, updatedMerchant);
        return updatedMerchant;
    }

    public Merchant deleteMerchant(UUID id) {
        Merchant existingMerchant = database.get(id);
        if (existingMerchant == null) {
            throw new IllegalArgumentException("Merchant not found");
        }
        database.remove(id);
        return existingMerchant;
    }

    public MerchantStatus getMerchantStatus(UUID id) {
        Merchant merchant = database.get(id);
        if (merchant == null) {
            throw new IllegalArgumentException("Merchant not found");
        }
        return merchant.status();
    }


}