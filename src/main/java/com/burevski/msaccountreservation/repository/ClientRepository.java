package com.burevski.msaccountreservation.repository;

import com.burevski.msaccountreservation.entity.ClientEntity;
import com.burevski.msaccountreservation.entity.enums.ClientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<ClientEntity, UUID>,
        JpaSpecificationExecutor<ClientEntity> {

    Optional<ClientEntity> findByIdAndStatusNot(UUID id, ClientStatus status);
}
