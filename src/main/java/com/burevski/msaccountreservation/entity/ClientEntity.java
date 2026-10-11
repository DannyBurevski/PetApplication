package com.burevski.msaccountreservation.entity;

import com.burevski.msaccountreservation.entity.enums.ClientStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "client")
public class ClientEntity extends AuditableEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(nullable = false, length = 100)
    private String citizenship;

    @Column(name = "client_type", nullable = false, length = 50)
    private String clientType;

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @Column(name = "document_series", nullable = false, length = 20)
    private String documentSeries;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(name = "mdm_code", nullable = false)
    private Long mdmId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClientStatus status = ClientStatus.ACTIVE;
}
