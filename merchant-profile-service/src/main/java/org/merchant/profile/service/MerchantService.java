
package org.merchant.profile.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.merchant.profile.model.Merchant;
import org.merchant.profile.model.MerchantStatus;

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
        if(merchant == null) {
            throw new IllegalArgumentException("Merchant not Found");
        }
        return merchant;
    }

    public Merchant createMerchant(String id, String name, String email) {

        if (database.containsKey(id)) {
            throw new IllegalArgumentException("Merchant with ID " + id + " already exists");
        }

        Instant now = Instant.now();
        Merchant merchant = new Merchant(id, name, email, MerchantStatus.PENDING, now, now);
        database.put(id, merchant);
        LOG.infof("Order %s saved", merchant.id());
        return merchant;
    }

    public Merchant updateMerchant(String id, String name, String email, MerchantStatus status) {
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
        LOG.infof("Merchant $s updated", id);
        return updatedMerchant;
    }

    public Merchant deleteMerchant(String id) {
        Merchant existingMerchant = database.get(id);
        if (existingMerchant == null) {
            throw new IllegalArgumentException("Merchant not found");
        }
        database.remove(id);
        LOG.infof("Merchant %s deleted", id);
        return existingMerchant;
    }

    public void deleteAllMerchants() {
        int count = database.size();
        database.clear();
        LOG.infof("All %s merchants have been deleted", count);
    }

    public MerchantStatus getMerchantStatus(String id) {
        Merchant merchant = database.get(id);
        if (merchant == null) {
            throw new IllegalArgumentException("Merchant not found");
        }
        return merchant.status();
    }


}