package com.burevski.msaccountreservation.service;

import com.burevski.msaccountreservation.entity.ClientEntity;
import com.burevski.msaccountreservation.entity.enums.ClientStatus;
import com.burevski.msaccountreservation.exception.ClientAlreadyExistsException;
import com.burevski.msaccountreservation.exception.ClientHasActiveAccountsException;
import com.burevski.msaccountreservation.exception.ClientNotFoundException;
import com.burevski.msaccountreservation.generated.dto.CreateClientRequest;
import com.burevski.msaccountreservation.mapper.ClientMapper;
import com.burevski.msaccountreservation.repository.AccountRepository;
import com.burevski.msaccountreservation.repository.ClientRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientService clientService;

    @Test
    void deleteClientWhenHasActiveAccountsShouldThrowAndKeepStatus() {
        // Arrange: клиент существует, у него есть блокирующие удаление счета
        UUID clientId = UUID.randomUUID();
        var client = new ClientEntity();
        client.setId(clientId);
        client.setStatus(ClientStatus.ACTIVE);
        when(clientRepository.findByIdAndStatusNot(
                clientId, ClientStatus.DELETED))
                .thenReturn(Optional.of(client));
        when(accountRepository.existsByClientIdAndStatusNameIn(
                eq(clientId), anyCollection()))
                .thenReturn(true);
        // Act + Assert: удаление запрещено
        assertThatThrownBy(() -> clientService.deleteClient(clientId))
                .isInstanceOf(ClientHasActiveAccountsException.class);
        // Статус клиента не изменился
        assertThat(client.getStatus()).isEqualTo(ClientStatus.ACTIVE);
    }

    @Test
    void deleteClientWhenNoActiveAccountsShouldMarkDeleted() {
        UUID clientId = UUID.randomUUID();
        var client = new ClientEntity();
        client.setId(clientId);
        client.setStatus(ClientStatus.ACTIVE);
        when(clientRepository.findByIdAndStatusNot(
                clientId, ClientStatus.DELETED))
                .thenReturn(Optional.of(client));
        when(accountRepository.existsByClientIdAndStatusNameIn(
                eq(clientId), anyCollection()))
                .thenReturn(false);
        clientService.deleteClient(clientId);
        assertThat(client.getStatus()).isEqualTo(ClientStatus.DELETED);
    }

    @Test
    void deleteClientWhenClientNotFoundShouldThrow() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.findByIdAndStatusNot(
                clientId, ClientStatus.DELETED))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> clientService.deleteClient(clientId))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    void createClientWhenDuplicateMdmIdShouldThrowClientAlreadyExists() {
        var request = new CreateClientRequest();
        var client = new ClientEntity();
        client.setFirstName("Иван");
        client.setLastName("Петров");

        var constraintViolation = new ConstraintViolationException(
                "Duplicate mdmId",
                new SQLException("Unique constraint violation", "23505"),
                "uq_client_mdm_code"
        );

        var databaseException = new DataIntegrityViolationException(
                "Cannot insert client", constraintViolation);

        when(clientMapper.toEntity(request)).thenReturn(client);
        when(clientRepository.saveAndFlush(client))
                .thenThrow(databaseException);

        assertThatThrownBy(() -> clientService.createClient(request))
                .isInstanceOf(ClientAlreadyExistsException.class)
                .hasCause(databaseException);
    }
}

