package com.umeaevents;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Röktest: misslyckas om Spring-kontexten inte kan laddas (t.ex. felaktig
 * konfiguration eller saknad bean). Kräver en nåbar databas eftersom
 * JPA/Flyway startar — se README för Testcontainers som nästa steg.
 */
@SpringBootTest
class UmeaEventsApplicationTests {

    @Test
    void contextLoads() {
    }
}
