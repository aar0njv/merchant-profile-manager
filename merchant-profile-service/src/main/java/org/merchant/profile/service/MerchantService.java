
package org.merchant.profile.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.merchant.profile.model.Merchant;
import org.merchant.profile.model.enums.MerchantStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.mutiny.Uni;

import org.jboss.logging.Logger;

@ApplicationScoped
public class MerchantService {

    private static final Logger LOG = Logger.getLogger(MerchantService.class);
    private final Map<String, Merchant> database = new ConcurrentHashMap<>();

    public Uni<List<Merchant>> getAllMerchants() {
        return Uni.createFrom().item(() -> 
            new ArrayList<>(database.values())
        );
    }


    public Uni<Merchant> getMerchantById(String id) {
        return Uni.createFrom().item(() -> database.get(id)).onItem().ifNull()
                            .failWith(() -> new IllegalArgumentException("Merchant Not Found"));
    }

    public Uni<Merchant> createMerchant(String id, String name, String email) {

        return Uni.createFrom().item(() -> {
            if (database.containsKey(id)) {
                throw new IllegalArgumentException(
                    "Merchant with id " + id + " already exists"
                );
            }

            Instant now = Instant.now();
            Merchant merchant = new Merchant(id, name, email, MerchantStatus.PENDING, now, now);
            database.put(id, merchant);
            return merchant;
        }).onItem().invoke(merchant ->
            LOG.infof("Merchant %s saved", merchant.id())
        ).onFailure().invoke(failure ->
            LOG.errorf(failure, "Failed to create merchant with id %s", id)
        );
    }

    public Uni<Merchant> updateMerchant(String id, String name, String email, MerchantStatus status) {
        return getMerchantById(id)
                .map(existingMerchant -> {
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
                return updatedMerchant;
                }).onItem().invoke(merchant ->
                    LOG.infof("Merchant %s updated successfully", merchant.id())
                ).onFailure().invoke(failure ->
                    LOG.errorf(failure,"Failed to update merchant %s", id)
                );
    }

    public Uni<Merchant> deleteMerchant(String id) {
        return getMerchantById(id)
                .map(existingMerchant -> {
                    database.remove(id);
                    return existingMerchant;
                }).onItem().invoke(merchant ->
                    LOG.infof("Merchant %s deleted successfully", merchant.id())
                );
    }

    public Uni<Void> deleteAllMerchants() {
        return Uni.createFrom().item(() -> {
            int count = database.size();
            database.clear();
            LOG.infof("All %s merchants have been deleted", count);
            return null;
        });
    }

    public Uni<MerchantStatus> getMerchantStatus(String id) {
        return getMerchantById(id)
            .map(Merchant::status);
    }


}