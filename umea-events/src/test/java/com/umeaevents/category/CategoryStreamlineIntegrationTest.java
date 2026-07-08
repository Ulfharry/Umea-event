package com.umeaevents.category;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards the V16 category streamline: exactly the six broad buckets, old slugs gone. */
@SpringBootTest
class CategoryStreamlineIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void hasExactlyTheSixStreamlinedCategories() {
        var slugs = categoryRepository.findAll().stream().map(Category::getSlug).toList();

        assertThat(slugs).containsExactlyInAnyOrder(
                "nattliv", "livemusik", "quiz", "sport", "gastronomi", "ovriga-event");
        assertThat(slugs).doesNotContain("pubquiz", "dj-kvallar", "sportvisningar",
                "vinprovningar", "standup", "temakvallar");
    }
}
