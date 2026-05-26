package de.tostsoft.solarmonitoring.app.tag;

import de.tostsoft.solarmonitoring.app.AppBaseTest;
import de.tostsoft.solarmonitoring.app.service.InfluxService;
import de.tostsoft.solarmonitoring.app.service.TagService;
import de.tostsoft.solarmonitoring.lib.dto.TagAggregationDTO;
import de.tostsoft.solarmonitoring.lib.model.*;
import de.tostsoft.solarmonitoring.lib.model.enums.InfluxMeasurement;
import de.tostsoft.solarmonitoring.lib.model.enums.PublicMode;
import de.tostsoft.solarmonitoring.lib.model.enums.SolarSystemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for tag aggregation caching functionality.
 *
 * This test verifies that:
 * 1. Cache infrastructure is properly configured
 * 2. Authenticated users bypass the cache
 * 3. Cache expiration works correctly
 * 4. Expensive operations (MongoDB and InfluxDB queries) are skipped on cache hits
 */
@TestPropertySource(properties = {
    "spring.cache.type=caffeine",
    "spring.cache.caffeine.spec=maximumSize=10,expireAfterWrite=10s"
})
public class TagAggregationCacheTest extends AppBaseTest {

    @Autowired
    private CacheManager cacheManager;

    @SpyBean
    private TagService tagService;

    @SpyBean
    private InfluxService influxService;

    @BeforeEach
    public void prepare() {
        clearDatabase();
        clearAllCaches(); // Clear caches between different test methods
        // Reset spy invocation counts from previous tests
        if (tagService != null) {
            Mockito.reset(tagService);
        }
        if (influxService != null) {
            Mockito.reset(influxService);
        }
    }

    private void clearAllCaches() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
    }

    private SolarSystem createSystemWithTag(User owner, Tag tag, String systemName,
                                           PublicMode publicMode, SolarSystemType type) {
        SolarSystem system = addSolarSystemForUser(owner, type, systemName);
        system.setPublicMode(publicMode);
        if (system.getTags() == null) {
            system.setTags(new java.util.ArrayList<>());
        }
        system.getTags().add(tag);
        system.setCurrentValues(CurrentValues.builder().build());
        return solarSystemRepository.save(system);
    }

    @Test
    public void testCacheManagerIsConfigured() {
        assertThat(cacheManager).isNotNull();
        Cache cache = cacheManager.getCache("tagAggregationPublic");
        assertThat(cache).isNotNull();
    }

    @Test
    public void testAuthenticatedUsersBypassCache() throws Exception {
        User owner = addUser(false);
        Tag tag = addTag("TestTag", "#FF0000");
        createSystemWithTag(owner, tag, "System1",
            PublicMode.PRODUCTION, SolarSystemType.GRID);

        String jwt = signIn(owner.getUsername());
        String url = "/api/tags/aggregation/" + tag.getId();

        ResponseEntity<String> response = doRestRequest(url, "", HttpMethod.GET,
            Collections.singletonMap("Cookie", "jwt=" + jwt));
        TagAggregationDTO result1 = objectMapper.readValue(response.getBody(), TagAggregationDTO.class);

        assertThat(result1).isNotNull();
        assertThat(result1.getTotalSystems()).isEqualTo(1);

        // Authenticated requests should NOT populate cache
        Cache cache = cacheManager.getCache("tagAggregationPublic");
        assertThat(cache).isNotNull();
        Cache.ValueWrapper cached = cache.get(tag.getId());
        assertThat(cached).isNull();
    }

    @Test
    public void testCacheExpiration() throws Exception {
        User owner = addUser(false);
        Tag tag = addTag("TestTag", "#FF0000");
        createSystemWithTag(owner, tag, "System1",
            PublicMode.PRODUCTION, SolarSystemType.GRID);

        Cache cache = cacheManager.getCache("tagAggregationPublic");
        assertThat(cache).isNotNull();

        // Manually put something in cache to test expiration
        cache.put(tag.getId(), "test-data");
        assertThat(cache.get(tag.getId())).isNotNull();

        // Wait for expiration (test uses 10s TTL)
        Thread.sleep(11000);

        // Verify cache entry expired
        assertThat(cache.get(tag.getId())).isNull();
    }

    @Test
    public void testResponseIsCorrectWithDifferentPagination() throws Exception {
        User owner = addUser(false);
        Tag tag = addTag("TestTag", "#FF0000");

        // Create 5 systems
        for (int i = 1; i <= 5; i++) {
            createSystemWithTag(owner, tag, "System" + i,
                PublicMode.PRODUCTION, SolarSystemType.GRID);
        }

        // Test different pagination parameters produce correct results
        ResponseEntity<String> response1 = doRestRequest(
            "/api/tags/aggregation/" + tag.getId() + "?page=0&size=2", "", HttpMethod.GET);
        TagAggregationDTO result1 = objectMapper.readValue(response1.getBody(), TagAggregationDTO.class);

        assertThat(result1).isNotNull();
        assertThat(result1.getSystems().getContent()).hasSize(2);
        assertThat(result1.getSystems().getTotalElements()).isEqualTo(5);

        ResponseEntity<String> response2 = doRestRequest(
            "/api/tags/aggregation/" + tag.getId() + "?page=1&size=2", "", HttpMethod.GET);
        TagAggregationDTO result2 = objectMapper.readValue(response2.getBody(), TagAggregationDTO.class);

        assertThat(result2).isNotNull();
        assertThat(result2.getSystems().getContent()).hasSize(2);
        assertThat(result2.getSystems().getTotalElements()).isEqualTo(5);
        assertThat(result2.getSystems().getPage()).isEqualTo(1);
    }

    @Test
    public void testResponseIsCorrectWithDifferentSorting() throws Exception {
        User owner = addUser(false);
        Tag tag = addTag("TestTag", "#FF0000");

        createSystemWithTag(owner, tag, "ASystem",
            PublicMode.PRODUCTION, SolarSystemType.GRID);
        createSystemWithTag(owner, tag, "ZSystem",
            PublicMode.PRODUCTION, SolarSystemType.GRID_BATTERY);

        // Test ascending sort
        ResponseEntity<String> response1 = doRestRequest(
            "/api/tags/aggregation/" + tag.getId() + "?sortBy=name&sortOrder=asc", "", HttpMethod.GET);
        TagAggregationDTO result1 = objectMapper.readValue(response1.getBody(), TagAggregationDTO.class);

        assertThat(result1).isNotNull();
        assertThat(result1.getSystems().getContent().get(0).getName()).isEqualTo("ASystem");
        assertThat(result1.getSystems().getContent().get(1).getName()).isEqualTo("ZSystem");

        // Test descending sort
        ResponseEntity<String> response2 = doRestRequest(
            "/api/tags/aggregation/" + tag.getId() + "?sortBy=name&sortOrder=desc", "", HttpMethod.GET);
        TagAggregationDTO result2 = objectMapper.readValue(response2.getBody(), TagAggregationDTO.class);

        assertThat(result2).isNotNull();
        assertThat(result2.getSystems().getContent().get(0).getName()).isEqualTo("ZSystem");
        assertThat(result2.getSystems().getContent().get(1).getName()).isEqualTo("ASystem");
    }

    @Test
    public void testDifferentTagsProduceDifferentResults() throws Exception {
        User owner = addUser(false);
        Tag tag1 = addTag("Tag1", "#FF0000");
        Tag tag2 = addTag("Tag2", "#00FF00");

        createSystemWithTag(owner, tag1, "System1",
            PublicMode.PRODUCTION, SolarSystemType.GRID);
        createSystemWithTag(owner, tag2, "System2",
            PublicMode.PRODUCTION, SolarSystemType.GRID_BATTERY);

        ResponseEntity<String> response1 = doRestRequest(
            "/api/tags/aggregation/" + tag1.getId(), "", HttpMethod.GET);
        TagAggregationDTO result1 = objectMapper.readValue(response1.getBody(), TagAggregationDTO.class);

        ResponseEntity<String> response2 = doRestRequest(
            "/api/tags/aggregation/" + tag2.getId(), "", HttpMethod.GET);
        TagAggregationDTO result2 = objectMapper.readValue(response2.getBody(), TagAggregationDTO.class);

        assertThat(result1).isNotNull();
        assertThat(result1.getTotalSystems()).isEqualTo(1);
        assertThat(result1.getSystems().getContent().get(0).getName()).isEqualTo("System1");

        assertThat(result2).isNotNull();
        assertThat(result2.getTotalSystems()).isEqualTo(1);
        assertThat(result2.getSystems().getContent().get(0).getName()).isEqualTo("System2");
    }

    @Test
    public void testCacheHitDoesNotCallTagService() throws Exception {
        User owner = addUser(false);
        Tag tag = addTag("TestTag", "#FF0000");
        createSystemWithTag(owner, tag, "System1", PublicMode.PRODUCTION, SolarSystemType.GRID);

        String url = "/api/tags/aggregation/" + tag.getId();

        // First request - should call TagService and populate cache
        ResponseEntity<String> response1 = doRestRequest(url, "", HttpMethod.GET);
        assertThat(response1.getStatusCode().is2xxSuccessful()).isTrue();

        // Verify TagService was called on first request
        verify(tagService, times(1)).findTagWithAccessibleSystems(tag.getId());

        // Verify cache is populated
        Cache cache = cacheManager.getCache("tagAggregationPublic");
        assertThat(cache).isNotNull();
        Cache.ValueWrapper cached = cache.get(tag.getId());
        assertThat(cached).withFailMessage("Cache should be populated after first request").isNotNull();

        // Reset spy to count only next invocations
        Mockito.reset(tagService);

        // Second request - should hit cache and NOT call TagService
        ResponseEntity<String> response2 = doRestRequest(url, "", HttpMethod.GET);
        assertThat(response2.getStatusCode().is2xxSuccessful()).isTrue();

        // Verify TagService was NOT called on second request (cache hit)
        verify(tagService, never()).findTagWithAccessibleSystems(tag.getId());

        // Verify cache still contains the same entry
        Cache.ValueWrapper cachedAfter = cache.get(tag.getId());
        assertThat(cachedAfter).isNotNull();
        assertThat(cachedAfter.get()).isSameAs(cached.get());
    }

    @Test
    public void testCacheHitDoesNotCallInfluxService() throws Exception {
        User owner = addUser(false);
        Tag tag = addTag("TestTag", "#FF0000");
        createSystemWithTag(owner, tag, "System1", PublicMode.PRODUCTION, SolarSystemType.GRID);

        String url = "/api/tags/aggregation/" + tag.getId();

        // First request - should call InfluxService and populate cache
        ResponseEntity<String> response1 = doRestRequest(url, "", HttpMethod.GET);
        assertThat(response1.getStatusCode().is2xxSuccessful()).isTrue();

        // Verify InfluxService was called on first request (at least once for the system)
        verify(influxService, atLeastOnce()).getStatisticsDataAsJson(
            any(SolarSystem.class),
            any(InfluxMeasurement.class),
            any(Date.class),
            any(Date.class),
            anyBoolean()
        );

        // Verify cache is populated
        Cache cache = cacheManager.getCache("tagAggregationPublic");
        assertThat(cache.get(tag.getId())).isNotNull();

        // Reset spy to count only next invocations
        Mockito.reset(influxService);

        // Second request - should hit cache and NOT call InfluxService
        ResponseEntity<String> response2 = doRestRequest(url, "", HttpMethod.GET);
        assertThat(response2.getStatusCode().is2xxSuccessful()).isTrue();

        // Verify InfluxService was NOT called on second request (cache hit)
        verify(influxService, never()).getStatisticsDataAsJson(
            any(SolarSystem.class),
            any(InfluxMeasurement.class),
            any(Date.class),
            any(Date.class),
            anyBoolean()
        );
    }

    @Test
    public void testDifferentParametersShareCacheEntry() throws Exception {
        User owner = addUser(false);
        Tag tag = addTag("TestTag", "#FF0000");

        // Create multiple systems to test pagination
        for (int i = 1; i <= 5; i++) {
            createSystemWithTag(owner, tag, "System" + i, PublicMode.PRODUCTION, SolarSystemType.GRID);
        }

        String baseUrl = "/api/tags/aggregation/" + tag.getId();

        // First request with page=0 - populates cache
        doRestRequest(baseUrl + "?page=0&size=2", "", HttpMethod.GET);

        // Verify TagService was called once
        verify(tagService, times(1)).findTagWithAccessibleSystems(tag.getId());

        Cache cache = cacheManager.getCache("tagAggregationPublic");
        Cache.ValueWrapper cached = cache.get(tag.getId());
        assertThat(cached).isNotNull();

        // Reset spy to verify next requests don't call services
        Mockito.reset(tagService);

        // Second request with page=1 - should use same cache entry
        doRestRequest(baseUrl + "?page=1&size=2", "", HttpMethod.GET);
        Cache.ValueWrapper cachedAfterPage1 = cache.get(tag.getId());
        assertThat(cachedAfterPage1.get()).isSameAs(cached.get());

        // Verify TagService was NOT called (cache hit)
        verify(tagService, never()).findTagWithAccessibleSystems(tag.getId());

        // Third request with different sort - should use same cache entry
        doRestRequest(baseUrl + "?sortBy=name&sortOrder=desc", "", HttpMethod.GET);
        Cache.ValueWrapper cachedAfterSort = cache.get(tag.getId());
        assertThat(cachedAfterSort.get()).isSameAs(cached.get());

        // Verify TagService still was NOT called (cache hit)
        verify(tagService, never()).findTagWithAccessibleSystems(tag.getId());
    }
}
