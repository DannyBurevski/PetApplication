package com.burevski.msaccountreservation.mapper;

import com.burevski.msaccountreservation.entity.ClientEntity;
import com.burevski.msaccountreservation.generated.dto.ClientListItem;
import com.burevski.msaccountreservation.generated.dto.ClientResponse;
import com.burevski.msaccountreservation.generated.dto.CreateClientRequest;
import com.burevski.msaccountreservation.generated.dto.UpdateClientRequest;
import com.burevski.msaccountreservation.generated.dto.ClientExistsResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ClientMapper {

    @Mapping(target = "hasAccounts", constant = "false")
    ClientResponse toResponse(ClientEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    ClientEntity toEntity(CreateClientRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "mdmId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "fullName", ignore = true)
    void updateEntity(UpdateClientRequest request, @MappingTarget ClientEntity entity);

    @Mapping(target = "clientId", source = "id")
    @Mapping(target = "exists", constant = "true")
    ClientExistsResponse toExistsResponse(ClientEntity entity);

    ClientListItem toListItem(ClientEntity entity);

}
