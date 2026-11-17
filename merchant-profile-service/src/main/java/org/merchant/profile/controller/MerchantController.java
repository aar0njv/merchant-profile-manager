
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

@Path("/merchants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GET
    public List<Merchant> getAllMerchants() {
        return merchantService.getAllMerchants();
    }

    @GET
    @Path("/{id}")
    public Response getMerchantById(@PathParam("id") String id) {
        Merchant merchant = merchantService.getMerchantById(id);
        return Response.ok(merchant).build();
    }

    @POST
    public Response createMerchant(@Valid MerchantRequest request) {
        String id = (request.id() == null || request.id().isBlank()) ? UUID.randomUUID().toString() : request.id();
        Merchant createdMerchant = merchantService.createMerchant(id, request.name(), request.email());
        return Response.status(Response.Status.CREATED).entity(createdMerchant).build();
    }
    

    @PUT
    @Path("/{id}")
    public Response updateMerchant(@PathParam("id") String id, @Valid MerchantRequest request) {
        Merchant updatedMerchant = merchantService.updateMerchant(id, request.name(), request.email(), request.status());
        return Response.ok(updatedMerchant).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteMerchant(@PathParam("id") String id) {
        Merchant deletedMerchant = merchantService.deleteMerchant(id);
        return Response.ok(deletedMerchant).build();
    }

    @GET
    @Path("/{id}/status")
    public Response getMerchantStatus(@PathParam("id") String id) {
        MerchantStatus status = merchantService.getMerchantStatus(id);
        return Response.ok(new StatusResponse(status)).build();
    }

    public record StatusResponse(MerchantStatus status) {}
}