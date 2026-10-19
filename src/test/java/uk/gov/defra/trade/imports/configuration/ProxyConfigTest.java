package uk.gov.defra.trade.imports.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.defra.trade.imports.metrics.CapturingLogAppender;

class ProxyConfigTest {

    private final ProxyConfig proxyConfig = new ProxyConfig();

    @AfterEach
    void clearProxyProperties() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://proxy-user:s3cret-password@proxy example:3128",
        "proxy-user:s3cret-password@proxy.example:3128",
        "http://proxy-user:s3cret@password@proxy.example:3128"
    })
    void configureProxy_unusableUrlWithCredentials_setsNoProxyAndLogsTheUrlRedactedWithoutThrowable(String proxyUrl) {
        ReflectionTestUtils.setField(proxyConfig, "httpProxy", proxyUrl);

        try (var logs = CapturingLogAppender.attachedTo(ProxyConfig.class)) {
            proxyConfig.configureProxy();

            assertThat(System.getProperty("http.proxyHost")).isNull();
            assertThat(logs.events()).isNotEmpty().allSatisfy(event -> {
                assertThat(event.getFormattedMessage())
                    .contains("***@")
                    .doesNotContain("proxy-user")
                    .doesNotContain("password");
                assertThat(event.getThrowableProxy()).isNull();
            });
        }
    }

    @Test
    void redactCredentials_replacesUserInfoAndKeepsSchemeHostAndPort() {
        assertThat(ProxyConfig.redactCredentials("http://user:secret@proxy.example:3128"))
            .isEqualTo("http://***@proxy.example:3128");
    }

    @Test
    void redactCredentials_leavesUrlWithoutCredentialsUnchanged() {
        assertThat(ProxyConfig.redactCredentials("http://proxy.example:3128"))
            .isEqualTo("http://proxy.example:3128");
    }
}
