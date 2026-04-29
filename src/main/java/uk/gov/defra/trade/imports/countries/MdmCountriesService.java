package uk.gov.defra.trade.imports.countries;

import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.defra.trade.imports.client.MdmClient;
import uk.gov.defra.trade.imports.configuration.MdmConfiguration;

@Slf4j
@AllArgsConstructor
@Service
public class MdmCountriesService {

  private static final String MDM_API_TRACE_ID_KEY = "x-ms-middleware-request-id";

  private final MdmClient mdmClient;
  private final MdmConfiguration mdmConfiguration;

  public List<MdmCountry> getCountries(List<String> classifiers) {

    String ocpApimSubscriptionKey = mdmConfiguration.ocpApimSubscriptionKey;

    ResponseEntity<List<MdmCountry>> responseEntity =
        mdmClient.getCountries(ocpApimSubscriptionKey, null, classifiers);
    Objects.requireNonNull(responseEntity.getHeaders().get(MDM_API_TRACE_ID_KEY))
        .stream().findFirst()
        .ifPresentOrElse(
            mdmApiTraceId -> log.info("MDM trace id for this call is: {}", mdmApiTraceId),
            () -> log.error("No MDM trace id returned")
        );

    return responseEntity.getBody();
  }
}
