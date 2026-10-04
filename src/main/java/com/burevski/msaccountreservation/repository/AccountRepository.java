package com.burevski.msaccountreservation.repository;

import com.burevski.msaccountreservation.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
}
