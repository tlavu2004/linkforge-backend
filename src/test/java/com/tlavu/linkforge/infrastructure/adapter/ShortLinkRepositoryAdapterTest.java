package com.tlavu.linkforge.infrastructure.adapter;

import com.tlavu.linkforge.domain.entity.ShortLink;
import com.tlavu.linkforge.domain.valueobject.OriginalUrl;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.infrastructure.persistence.mapper.ShortLinkMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(ShortLinkRepositoryAdapter.class)
@ComponentScan(basePackageClasses = ShortLinkMapper.class)
class ShortLinkRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private ShortLinkRepositoryAdapter adapter;

    @Test
    @DisplayName("Should save and return domain entity")
    void shouldSaveAndReturnDomainEntity() {
        ShortLink link = ShortLink.create(
                1L, // ID is ignored/overwritten by DB sequence usually, but here manually assigned
                    // if not generated?
                    // Wait, JpaEntity has @Id but no @GeneratedValue strategy shown in previous
                    // view.
                    // Let's assume we need to handle ID or it's manually assigned.
                    // In ShortLink.java create factory, ID is passed.
                ShortCode.of("testCode"),
                OriginalUrl.of("http://example.com"),
                Instant.now().plus(1, ChronoUnit.DAYS),
                null,
                "deleteHash");

        ShortLink saved = adapter.save(link);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getShortCode().code()).isEqualTo("testCode");
        assertThat(saved.getOriginalUrl().url()).isEqualTo("http://example.com");
    }

    @Test
    @DisplayName("Should find by short code")
    void shouldFindByShortCode() {
        ShortLink link = ShortLink.create(
                2L,
                ShortCode.of("findMe"),
                OriginalUrl.of("http://find.com"),
                Instant.now().plus(1, ChronoUnit.DAYS),
                null,
                "hash");
        adapter.save(link);

        Optional<ShortLink> found = adapter.findByShortCode(ShortCode.of("findMe"));

        assertThat(found).isPresent();
        assertThat(found.get().getOriginalUrl().url()).isEqualTo("http://find.com");
    }

    @Test
    @DisplayName("Should delete entity")
    void shouldDeleteEntity() {
        ShortLink link = ShortLink.create(
                3L,
                ShortCode.of("deleteMe"),
                OriginalUrl.of("http://del.com"),
                Instant.now().plus(1, ChronoUnit.DAYS),
                null,
                "hash");
        ShortLink saved = adapter.save(link);

        adapter.delete(saved.getId());

        Optional<ShortLink> found = adapter.findByShortCode(ShortCode.of("deleteMe"));
        assertThat(found).isEmpty();
    }
}
