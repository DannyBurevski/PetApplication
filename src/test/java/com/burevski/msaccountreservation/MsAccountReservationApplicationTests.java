package com.burevski.msaccountreservation;

import com.burevski.msaccountreservation.config.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class MsAccountReservationApplicationTests {

    @Test
    void contextLoads() {
    }
}
