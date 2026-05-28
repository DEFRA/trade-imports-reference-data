package uk.gov.defra.trade.imports.integration;

import static org.mockserver.model.HttpRequest.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Verifies AC2: expired cache entries are re-fetched from MDM.
 *
 * A separate Spring context is used here (cache.mdm.ttl-minutes=0) so that
 * entries expire immediately after write, without affecting the cache-hit
 * tests in CountriesControllerIT which rely on the default 60-minute TTL.
 */
class CountriesControllerCacheExpiryIT extends IntegrationBase {

  @DynamicPropertySource
  static void zeroTtl(DynamicPropertyRegistry registry) {
    registry.add("cache.mdm.ttl-minutes", () -> "0");
  }

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private CacheManager cacheManager;

  @BeforeEach
  void stubServices() {
    cacheManager.getCache("MDM_COUNTRIES_CACHE").clear();
    stubMdmCountriesResponse();
  }

  @Test
  void getCountries_refetchesFromMdm_afterCacheExpires() {
    // When: called twice with zero-TTL cache (entries expire immediately after write)
    restTemplate.getForEntity("/countries", String.class);
    restTemplate.getForEntity("/countries", String.class);

    // Then: MDM was called twice — the cached entry expired before the second request
    usingStub().verify(
        request().withMethod("GET").withPath("/mdm-service/mdm/geo/countries"),
        VerificationTimes.exactly(2)
    );
  }
}
