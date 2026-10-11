package com.burevski.msaccountreservation.service;

import com.burevski.msaccountreservation.entity.ClientEntity;
import com.burevski.msaccountreservation.entity.enums.ClientStatus;
import com.burevski.msaccountreservation.exception.ClientAlreadyExistsException;
import com.burevski.msaccountreservation.exception.ClientHasActiveAccountsException;
import com.burevski.msaccountreservation.exception.ClientNotFoundException;
import com.burevski.msaccountreservation.generated.dto.CreateClientRequest;
import com.burevski.msaccountreservation.generated.dto.UpdateClientRequest;
import com.burevski.msaccountreservation.mapper.ClientMapper;
import com.burevski.msaccountreservation.repository.AccountRepository;
import com.burevski.msaccountreservation.repository.ClientRepository;
import com.burevski.msaccountreservation.repository.specification.ClientSpecification;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ClientService {

    private static final Set<String> NON_TERMINAL_ACCOUNT_STATUSES = Set.of("NEW", "IN_CREATION", "CREATED");

    private final AccountRepository accountRepository;

    private final ClientMapper clientMapper;

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public ClientEntity getClientById(UUID clientId) {
        return findNonDeletedClientOrThrow(clientId);
    }

    @Transactional
    public ClientEntity createClient(CreateClientRequest request) {
        var client = clientMapper.toEntity(request);
        updateFullName(client);
        try {
            return clientRepository.saveAndFlush(client);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateMdmId(exception)) {
                throw new ClientAlreadyExistsException(exception);
            }

            throw exception;
        }
    }

    @Transactional
    public ClientEntity updateClient(UUID clientId, UpdateClientRequest request) {
        var client = findNonDeletedClientOrThrow(clientId);
        clientMapper.updateEntity(request, client);
        updateFullName(client);
        return clientRepository.saveAndFlush(client);
    }

    @Transactional(readOnly = true)
    public Page<ClientEntity> searchClients(int page, int size, String lastName, Long mdmId) {
        var specification = ClientSpecification.withFilters(lastName, mdmId);
        var pageable = PageRequest.of(page, size, Sort.by("createdAt", "id"));

        return clientRepository.findAll(specification, pageable);
    }

    @Transactional
    public void deleteClient(UUID clientId) {
        var client = findNonDeletedClientOrThrow(clientId);
        if (accountRepository.existsByClientIdAndStatusNameIn(
                clientId, NON_TERMINAL_ACCOUNT_STATUSES)) {
            throw new ClientHasActiveAccountsException(clientId);
        }

        client.setStatus(ClientStatus.DELETED);
    }

    @Transactional(readOnly = true)
    public Optional<ClientEntity> findNonDeletedClient(UUID clientId) {
        return clientRepository.findByIdAndStatusNot(
                clientId, ClientStatus.DELETED);
    }

    private ClientEntity findNonDeletedClientOrThrow(UUID clientId) {
        return clientRepository.findByIdAndStatusNot(
                        clientId, ClientStatus.DELETED)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
    }

    private void updateFullName(ClientEntity client) {
        String fullName = Stream.of(client.getLastName(), client.getFirstName(), client.getMiddleName())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        client.setFullName(fullName);
    }

    private boolean isDuplicateMdmId(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation && "uq_client_mdm_code"
                    .equals(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }
}
