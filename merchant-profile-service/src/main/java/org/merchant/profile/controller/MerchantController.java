
package org.merchant.profile.controller;

import org.merchant.profile.model.Merchant;
import org.merchant.profile.model.MerchantRequest;
import org.merchant.profile.model.MerchantStatus;
import org.merchant.profile.service.MerchantService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

@Path("/merchants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GET
    public Uni<Response> getAllMerchants() {
        return merchantService.getAllMerchants()
            .map(merchants -> Response.ok(merchants).build());
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getMerchantById(@PathParam("id") String id) {
        return merchantService.getMerchantById(id).map(merchant -> Response.ok(merchant).build());
    }

    @POST
    public Uni<Response> createMerchant(@Valid MerchantRequest request) {
        String id = (request.id() == null || request.id().isBlank()) ? UUID.randomUUID().toString() : request.id();
        return merchantService.createMerchant(id, request.name(), request.email())
            .map(createdMerchant -> Response.status(Response.Status.CREATED).entity(createdMerchant).build());
    }

    @PUT
    @Path("/{id}")
    public Uni<Response> updateMerchant(@PathParam("id") String id, @Valid MerchantRequest request) {
        return merchantService.updateMerchant(id, request.name(),request.email(), request.status())
            .map(updatedMerchant -> Response.ok(updatedMerchant).build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> deleteMerchant(@PathParam("id") String id) {
        return merchantService.deleteMerchant(id)
            .map(deletedMerchant -> Response.ok(deletedMerchant).build());
    }

    @DELETE
    public Uni<Response> deleteAllMerchants() {
        return merchantService.deleteAllMerchants()
            .replaceWith(Response.noContent().build());
    }

    @GET
    @Path("/{id}/status")
    public Uni<Response> getMerchantStatus(@PathParam("id") String id) {
        return merchantService.getMerchantStatus(id)
            .map(StatusResponse::new)
            .map(statusResponse ->
                Response.ok(statusResponse).build()
            );
    }

    public record StatusResponse(MerchantStatus status) {}
}