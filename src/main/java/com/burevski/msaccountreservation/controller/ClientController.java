package com.burevski.msaccountreservation.controller;

import com.burevski.msaccountreservation.generated.api.ClientsApi;
import com.burevski.msaccountreservation.generated.dto.*;
import com.burevski.msaccountreservation.mapper.ClientMapper;
import com.burevski.msaccountreservation.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ClientController implements ClientsApi {

    private final ClientService clientService;
    private final ClientMapper clientMapper;

    @Override
    public ResponseEntity<ClientResponse> getClientById(UUID clientId) {
        var client = clientService.getClientById(clientId);
        var response = clientMapper.toResponse(client);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClientPageResponse> searchClients(Integer page, Integer size,
            @Nullable String lastName,
            @Nullable Long mdmId) {

        var clients = clientService.searchClients(page, size, lastName, mdmId);
        var metadata = new PageMetadata();
        metadata.setPageNumber(clients.getNumber());
        metadata.setPageSize(clients.getSize());
        metadata.setTotalPages(clients.getTotalPages());
        metadata.setTotalElements(clients.getTotalElements());

        var response = new ClientPageResponse();
        response.setContent(clients.map(clientMapper::toListItem).getContent());
        response.setPageable(metadata);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClientResponse> updateClient(UUID clientId, UpdateClientRequest request) {
        var client = clientService.updateClient(clientId, request);
        return ResponseEntity.ok(clientMapper.toResponse(client));
    }

    @Override
    public ResponseEntity<ClientExistsResponse> checkClientExists(UUID clientId) {
        var response = clientService.findNonDeletedClient(clientId)
                .map(clientMapper::toExistsResponse)
                .orElseGet(() -> {
                    var result = new ClientExistsResponse();
                    result.setExists(false);
                    result.setClientId(clientId);
                    result.setStatus(null);
                    return result;});

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClientResponse> createClient(CreateClientRequest request) {
        var client = clientService.createClient(request);
        var response = clientMapper.toResponse(client);

        return ResponseEntity
                .created(URI.create("/api/v1/clients/" + client.getId()))
                .body(response);
    }

    @Override
    public ResponseEntity<Void> deleteClient(UUID clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }
}
