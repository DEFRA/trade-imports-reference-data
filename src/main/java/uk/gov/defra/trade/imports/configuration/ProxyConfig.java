package uk.gov.defra.trade.imports.configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;

@Configuration
@Slf4j
public class ProxyConfig {

    @Value("${cdp.proxyUrl}")
    private String httpProxy;

    @PostConstruct
    public void configureProxy() {

        if (httpProxy == null || httpProxy.isEmpty()) {
            log.info("No HTTP_PROXY configured - using direct connections");
            return;
        }

        try {
            URI proxyUri = URI.create(httpProxy);
            String proxyHost = proxyUri.getHost();
            int proxyPort = proxyUri.getPort();

            if (proxyHost == null || proxyPort == -1) {
                log.warn("Invalid HTTP_PROXY format: {}. Expected http://host:port", redactCredentials(httpProxy));
                return;
            }

            setProxySystemProperties(proxyHost, proxyPort);
            ProxySelector.setDefault(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));

            log.info("HTTP proxy configured: {}:{}", proxyHost, proxyPort);

        } catch (IllegalArgumentException e) {
            log.error("Failed to parse HTTP_PROXY {}: {}", redactCredentials(httpProxy), e.getClass().getSimpleName());
        }
    }

    static String redactCredentials(String proxyUrl) {
        return proxyUrl.replaceFirst("(?s)^([^/@]*//)?.*@", "$1***@");
    }

    private static void setProxySystemProperties(String proxyHost, int proxyPort) {
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", String.valueOf(proxyPort));
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", String.valueOf(proxyPort));
    }
}
