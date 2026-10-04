package com.burevski.msaccountreservation.repository;

import com.burevski.msaccountreservation.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {
}
