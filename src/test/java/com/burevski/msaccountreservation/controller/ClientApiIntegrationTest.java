package com.burevski.msaccountreservation.controller;

import com.burevski.msaccountreservation.config.PostgresTestConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class ClientApiIntegrationTest {

    private static final String CLIENTS = "/api/v1/clients";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTestData() {
        // Проверяем подключение до удаления каких-либо данных.
        assertThat(jdbcTemplate.queryForObject("SELECT current_database()", String.class))
                .isEqualTo("account_reservation_test");
        // Справочник account_status, заполненный Liquibase, сохраняем.
        //noinspection SqlWithoutWhere
        jdbcTemplate.update("DELETE FROM account");
        //noinspection SqlWithoutWhere
        jdbcTemplate.update("DELETE FROM client");
    }

    @Test
    void createClientShouldReturn201AndPersistClient() throws Exception {
        var result = mockMvc.perform(post(CLIENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(createRequest(9000000001L, "Петров").toString()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.mdmId").value(9000000001L))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.lastName").value("Петров"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.hasAccounts").value(false))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn();

        var response = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        UUID clientId = UUID.fromString(response.get("id").asText());
        assertThat(result.getResponse().getHeader("Location"))
                .isEqualTo(CLIENTS + "/" + clientId);

        var stored = jdbcTemplate.queryForMap("SELECT * FROM client WHERE id = ?", clientId);
        assertThat(stored.get("mdm_code")).isEqualTo(9000000001L);
        assertThat(stored.get("first_name")).isEqualTo("Иван");
        assertThat(stored.get("last_name")).isEqualTo("Петров");
        assertThat(stored.get("full_name")).isEqualTo("Петров Иван");
        assertThat(stored.get("status")).isEqualTo("ACTIVE");
        assertThat(stored.get("created_at")).isNotNull();
        assertThat(stored.get("updated_at")).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"mdmId", "firstName", "lastName", "citizenship", "clientType",
            "documentNumber", "documentSeries", "documentType"})
    void createClientWhenRequiredFieldMissingShouldReturn400(String field) throws Exception {
        var request = createRequest(1001L, "Петров");
        request.remove(field);
        mockMvc.perform(post(CLIENTS).contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(400));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM client", Long.class))
                .isZero();
    }

    @Test
    void createClientWhenBlankNameShouldReturn400() throws Exception {
        var request = createRequest(1001L, "Петров");
        request.put("firstName", "   ");
        mockMvc.perform(post(CLIENTS).contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.statusCode").value(400));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM client", Long.class))
                .isZero();
    }

    @Test
    void createClientWhenMalformedJsonShouldReturn400() throws Exception {
        mockMvc.perform(post(CLIENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new byte[] {'{', ']'}))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void createClientWhenDuplicateMdmIdShouldReturn409() throws Exception {
        createClient(1001L, "Петров");
        mockMvc.perform(post(CLIENTS).contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(1001L, "Сидоров").toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(409));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM client", Long.class))
                .isEqualTo(1L);
    }

    @Test
    void getClientShouldReturnStoredClient() throws Exception {
        UUID clientId = createClient(1001L, "Петров");
        mockMvc.perform(get(CLIENTS + "/{id}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clientId.toString()))
                .andExpect(jsonPath("$.mdmId").value(1001))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.lastName").value("Петров"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.hasAccounts").value(false));
    }

    @Test
    void getClientWhenMissingShouldReturn404() throws Exception {
        mockMvc.perform(get(CLIENTS + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/clients/abc", "/api/v1/clients/abc/exists"})
    void getClientWhenInvalidUuidShouldReturn400(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void searchClientsShouldApplyDefaultsAndPagination() throws Exception {
        createClient(1001L, "Петров");
        createClient(1002L, "Сидоров");
        mockMvc.perform(get(CLIENTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.pageable.totalPages").value(1))
                .andExpect(jsonPath("$.pageable.totalElements").value(2));
        mockMvc.perform(get(CLIENTS).param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.pageable.pageNumber").value(1))
                .andExpect(jsonPath("$.pageable.pageSize").value(1))
                .andExpect(jsonPath("$.pageable.totalPages").value(2))
                .andExpect(jsonPath("$.pageable.totalElements").value(2));
    }

    @Test
    void searchClientsShouldCombineFiltersAndExcludeDeletedFromCounts() throws Exception {
        UUID activeId = createClient(1001L, "Петров");
        createClient(1002L, "Сидоров");
        UUID deletedId = createClient(1003L, "Петров");
        jdbcTemplate.update("UPDATE client SET status = 'DELETED' WHERE id = ?", deletedId);

        mockMvc.perform(get(CLIENTS).param("lastName", "петров"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(activeId.toString()))
                .andExpect(jsonPath("$.pageable.totalElements").value(1));
        mockMvc.perform(get(CLIENTS).param("mdmId", "1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].lastName").value("Сидоров"));
        mockMvc.perform(get(CLIENTS).param("lastName", "петров").param("mdmId", "1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.pageable.totalPages").value(0))
                .andExpect(jsonPath("$.pageable.totalElements").value(0));
        mockMvc.perform(get(CLIENTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.pageable.totalElements").value(2));
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "size,0", "size,101", "mdmId,abc"})
    void searchClientsWhenInvalidParameterShouldReturn400(String parameter, String value)
            throws Exception {
        mockMvc.perform(get(CLIENTS).param(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void updateClientShouldPersistNamesAndPreserveImmutableFields() throws Exception {
        UUID clientId = createClient(1001L, "Петров");
        Timestamp createdAt = jdbcTemplate.queryForObject(
                "SELECT created_at FROM client WHERE id = ?", Timestamp.class, clientId);
        jdbcTemplate.update("""
                UPDATE client SET status = 'BLOCKED',
                    updated_at = TIMESTAMPTZ '2020-01-01 00:00:00+00'
                WHERE id = ?
                """, clientId);

        var result = mockMvc.perform(put(CLIENTS + "/{id}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clientId.toString()))
                .andExpect(jsonPath("$.mdmId").value(1001))
                .andExpect(jsonPath("$.lastName").value("Сидоров"))
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andReturn();

        var response = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(Instant.parse(response.get("updatedAt").asText()))
                .isAfter(Instant.parse("2020-01-01T00:00:00Z"));
        var stored = jdbcTemplate.queryForMap("SELECT * FROM client WHERE id = ?", clientId);
        assertThat(stored.get("last_name")).isEqualTo("Сидоров");
        assertThat(stored.get("full_name")).isEqualTo("Сидоров Иван");
        assertThat(stored.get("mdm_code")).isEqualTo(1001L);
        assertThat(stored.get("status")).isEqualTo("BLOCKED");
        assertThat(stored.get("created_at")).isEqualTo(createdAt);
        assertThat(((Timestamp) stored.get("updated_at")).toInstant())
                .isAfter(Instant.parse("2020-01-01T00:00:00Z"));
    }

    @Test
    void updateClientWhenInvalidBodyShouldReturn400AndKeepData() throws Exception {
        UUID clientId = createClient(1001L, "Петров");
        var request = updateRequest();
        request.put("firstName", "   ");
        mockMvc.perform(put(CLIENTS + "/{id}", clientId)
                        .contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.statusCode").value(400));
        assertThat(jdbcTemplate.queryForObject("SELECT last_name FROM client WHERE id = ?",
                String.class, clientId)).isEqualTo("Петров");
    }

    @Test
    void updateClientWhenMissingShouldReturn404() throws Exception {
        mockMvc.perform(put(CLIENTS + "/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEW", "IN_CREATION", "CREATED"})
    void deleteClientWhenBlockingAccountShouldReturn409AndKeepData(String accountStatus)
            throws Exception {
        UUID clientId = createClient(1001L, "Петров");
        addAccount(clientId, accountStatus);
        mockMvc.perform(delete(CLIENTS + "/{id}", clientId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_HAS_ACTIVE_ACCOUNTS"))
                .andExpect(jsonPath("$.errorDescription").isNotEmpty())
                .andExpect(jsonPath("$.statusCode").value(409));
        assertThat(storedStatus(clientId)).isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM account", Long.class))
                .isEqualTo(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CANCELLED", "CLOSED"})
    void deleteClientWhenTerminalAccountShouldReturn204AndKeepRows(String accountStatus)
            throws Exception {
        UUID clientId = createClient(1001L, "Петров");
        addAccount(clientId, accountStatus);
        mockMvc.perform(delete(CLIENTS + "/{id}", clientId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertThat(storedStatus(clientId)).isEqualTo("DELETED");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM client", Long.class))
                .isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM account", Long.class))
                .isEqualTo(1L);
    }

    @Test
    void deletedClientShouldRemainInDatabaseAndBeUnavailableThroughApi() throws Exception {
        UUID clientId = createClient(1001L, "Петров");
        mockMvc.perform(delete(CLIENTS + "/{id}", clientId))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(storedStatus(clientId)).isEqualTo("DELETED");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM client", Long.class))
                .isEqualTo(1L);
        mockMvc.perform(get(CLIENTS + "/{id}", clientId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"));
        mockMvc.perform(put(CLIENTS + "/{id}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"));
        mockMvc.perform(delete(CLIENTS + "/{id}", clientId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"));
        mockMvc.perform(get(CLIENTS).param("mdmId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.pageable.totalElements").value(0));
        mockMvc.perform(get(CLIENTS + "/{id}/exists", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.status").hasJsonPath())
                .andExpect(jsonPath("$.status").value(nullValue()));
        assertThat(storedStatus(clientId)).isEqualTo("DELETED");
    }

    @Test
    void deleteClientWhenMissingShouldReturn404() throws Exception {
        mockMvc.perform(delete(CLIENTS + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "BLOCKED"})
    void existsWhenNonDeletedClientShouldReturnTrueAndStatus(String clientStatus)
            throws Exception {
        UUID clientId = createClient(1001L, "Петров");
        jdbcTemplate.update("UPDATE client SET status = ? WHERE id = ?", clientStatus, clientId);
        mockMvc.perform(get(CLIENTS + "/{id}/exists", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.status").value(clientStatus));
    }

    @Test
    void existsWhenMissingShouldReturnFalseAndNullStatus() throws Exception {
        UUID clientId = UUID.randomUUID();
        mockMvc.perform(get(CLIENTS + "/{id}/exists", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.status").hasJsonPath())
                .andExpect(jsonPath("$.status").value(nullValue()));
    }

    private UUID createClient(long mdmId, String lastName) throws Exception {
        var response = mockMvc.perform(post(CLIENTS).contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(mdmId, lastName).toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private ObjectNode createRequest(long mdmId, String lastName) {
        var request = objectMapper.createObjectNode();
        request.put("mdmId", mdmId);
        request.put("firstName", "Иван");
        request.put("lastName", lastName);
        request.putNull("middleName");
        request.put("citizenship", "РФ");
        request.put("clientType", "INDIVIDUAL");
        request.put("documentNumber", "123456");
        request.put("documentSeries", "1234");
        request.put("documentType", "PASSPORT");
        return request;
    }

    private ObjectNode updateRequest() {
        var request = createRequest(1001L, "Сидоров");
        request.remove("mdmId");
        return request;
    }

    private void addAccount(UUID clientId, String accountStatus) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO account (id, status_id, client_id, account_type,
                                     currency_code, created_at, updated_at)
                SELECT gen_random_uuid(), id, ?, 'CURRENT', 'RUB',
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM account_status WHERE name = ?
                """, clientId, accountStatus);
        assertThat(inserted).isEqualTo(1);
    }

    private String storedStatus(UUID clientId) {
        return jdbcTemplate.queryForObject("SELECT status FROM client WHERE id = ?",
                String.class, clientId);
    }
}
