package com.umeaevents.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data genererar implementationen vid uppstart. JpaRepository ger
 * findAll/findById/save/delete gratis. Metoderna nedan följer Spring Datas
 * query-derivation (metodnamnet blir SQL).
 */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByActiveTrueOrderByNameAsc();

    boolean existsBySlug(String slug);
}
