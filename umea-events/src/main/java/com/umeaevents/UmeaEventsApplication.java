package com.umeaevents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Applikationens entrypoint. @SpringBootApplication aktiverar
 * auto-konfiguration, component scanning (från detta paket och nedåt)
 * och konfigurationsegenskaper.
 */
@SpringBootApplication
public class UmeaEventsApplication {

    public static void main(String[] args) {
        SpringApplication.run(UmeaEventsApplication.class, args);
    }
}
