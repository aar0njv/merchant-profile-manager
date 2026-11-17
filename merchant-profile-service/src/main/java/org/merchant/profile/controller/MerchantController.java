
package org.merchant.profile.controller;

import org.merchant.profile.model.Merchant;
import org.merchant.profile.model.MerchantRequest;
import org.merchant.profile.model.MerchantStatus;
import org.merchant.profile.service.MerchantService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
        try {
            Merchant merchant = merchantService.getMerchantById(id);
            return Response.ok(merchant).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @POST
    public Response createMerchant(MerchantRequest request) {

        String id = (request.id() == null || request.id().isBlank()) ? UUID.randomUUID().toString() : request.id();

        Merchant createdMerchant = merchantService.createMerchant(id, request.name(), request.email());
        return Response.status(Response.Status.CREATED).entity(createdMerchant).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateMerchant(@PathParam("id") String id, MerchantRequest request) {
        try {
            Merchant updatedMerchant = merchantService.updateMerchant(id, request.name(), request.email(), request.status());
            return Response.ok(updatedMerchant).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteMerchant(@PathParam("id") String id) {
        try {
            Merchant deletedMerchant = merchantService.deleteMerchant(id);
            return Response.ok(deletedMerchant).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/{id}/status")
    public Response getMerchantStatus(@PathParam("id") String id) {
        try {
            MerchantStatus status = merchantService.getMerchantStatus(id);
            return Response.ok(new StatusResponse(status)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    public record StatusResponse(MerchantStatus status) {}
}