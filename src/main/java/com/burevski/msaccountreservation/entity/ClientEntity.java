package com.burevski.msaccountreservation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "client")
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

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
    private Long mdmCode;
}
