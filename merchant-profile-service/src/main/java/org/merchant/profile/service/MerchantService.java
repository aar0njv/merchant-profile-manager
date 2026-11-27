
package org.merchant.profile.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.merchant.profile.model.Merchant;
import org.merchant.profile.model.enums.MerchantStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

@ApplicationScoped
public class MerchantService {

    private static final Logger LOG = Logger.getLogger(MerchantService.class);
    private final Map<String, Merchant> database = new ConcurrentHashMap<>();

    public List<Merchant> getAllMerchants() {
        return new ArrayList<>(database.values());
    }

    public Merchant getMerchantById(String id) {
        Merchant merchant = database.get(id);
        if (merchant == null) {
            throw new IllegalArgumentException("Merchant Not Found");
        }
        return merchant;
    }

    public Merchant createMerchant(String id, String name, String email) {
        if (database.containsKey(id)) {
            IllegalArgumentException failure = new IllegalArgumentException(
                "Merchant with id " + id + " already exists"
            );
            LOG.errorf(failure, "Failed to create merchant with id %s", id);
            throw failure;
        }

        Instant now = Instant.now();
        Merchant merchant = new Merchant(id, name, email, MerchantStatus.PENDING, now, now);
        database.put(id, merchant);
        LOG.infof("Merchant %s saved", merchant.id());
        return merchant;
    }

    public Merchant updateMerchant(String id, String name, String email, MerchantStatus status) {
        try {
            Merchant existingMerchant = getMerchantById(id);
            Instant now = Instant.now();
            Merchant updatedMerchant = new Merchant(
                id,
                name != null ? name : existingMerchant.name(),
                email != null ? email : existingMerchant.email(),
                status != null ? status : existingMerchant.status(),
                existingMerchant.createdAt(),
                now
            );
            database.put(id, updatedMerchant);
            LOG.infof("Merchant %s updated successfully", updatedMerchant.id());
            return updatedMerchant;
        } catch (RuntimeException failure) {
            LOG.errorf(failure, "Failed to update merchant %s", id);
            throw failure;
        }
    }

    public Merchant deleteMerchant(String id) {
        Merchant existingMerchant = getMerchantById(id);
        database.remove(id);
        LOG.infof("Merchant %s deleted successfully", existingMerchant.id());
        return existingMerchant;
    }

    public void deleteAllMerchants() {
        int count = database.size();
        database.clear();
        LOG.infof("All %s merchants have been deleted", count);
    }

    public MerchantStatus getMerchantStatus(String id) {
        return getMerchantById(id).status();
    }


}