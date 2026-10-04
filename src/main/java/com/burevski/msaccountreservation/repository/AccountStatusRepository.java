package com.burevski.msaccountreservation.repository;

import com.burevski.msaccountreservation.entity.AccountStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountStatusRepository extends JpaRepository<AccountStatusEntity, Integer> {
}
