package com.burevski.msaccountreservation.repository.specification;

import com.burevski.msaccountreservation.entity.ClientEntity;
import com.burevski.msaccountreservation.entity.enums.ClientStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class ClientSpecification {

    private ClientSpecification() {
    }

    public static Specification<ClientEntity> withFilters(String lastName, Long mdmId) {
        Specification<ClientEntity> spec = (root, query, cb) -> cb.notEqual(
                        root.get("status"),
                        ClientStatus.DELETED);
        if (lastName != null && !lastName.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(
                    cb.lower(root.get("lastName")), lastName.strip().toLowerCase(Locale.ROOT)));
        }

        if (mdmId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("mdmId"), mdmId));
        }

        return spec;
    }
}
