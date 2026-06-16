package com.umeaevents.scraping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DateTextTest {

    @Test
    void prefersIsoDate() {
        assertThat(DateText.firstDate("Spelas 2026-07-01 kl 20.00")).isEqualTo("2026-07-01");
    }

    @Test
    void extractsNamedMonth() {
        assertThat(DateText.firstDate("Den 13 december intar bandet scenen")).isEqualTo("13 december");
    }

    @Test
    void prefersNamedMonthOverNumericTimeLikeToken() {
        // "start 20.00" must not win over the real date "24 januari"
        assertThat(DateText.firstDate("Lördag 24 januari, start 20.00")).isEqualTo("24 januari");
    }

    @Test
    void acceptsNumericDateWithValidMonth() {
        assertThat(DateText.firstDate("VM-kval 5/9 mot Spanien")).isEqualTo("5/9");
        assertThat(DateText.firstDate("Fest 1.3 i vår")).isEqualTo("1.3");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Quizet startar 20:00",          // colon time
            "GRATIS aktiviteter från 20.00", // month 00 -> not a date
            "Onsdagar 20.30",                // month 30 -> not a date
            "Öppet 11.00 - 13.30",           // both time-shaped
    })
    void rejectsTimeShapedTokens(String text) {
        assertThat(DateText.firstDate(text)).isNull();
    }

    @Test
    void returnsNullForBlankOrNull() {
        assertThat(DateText.firstDate(null)).isNull();
        assertThat(DateText.firstDate("   ")).isNull();
        assertThat(DateText.firstDate("Inga datum här")).isNull();
    }
}
