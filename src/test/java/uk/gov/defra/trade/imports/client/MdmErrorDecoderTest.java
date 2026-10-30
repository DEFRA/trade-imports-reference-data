package uk.gov.defra.trade.imports.client;

import static org.assertj.core.api.Assertions.assertThat;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.defra.trade.imports.exceptions.MdmUnavailableException;

class MdmErrorDecoderTest {

  private final MdmErrorDecoder decoder = new MdmErrorDecoder();

  @ParameterizedTest
  @ValueSource(ints = {502, 503, 504})
  void decode_wrapsAnUnavailableMdmStatus(int status) {
    Exception decoded = decoder.decode("MdmClient#searchProducts", response(status));

    assertThat(decoded)
        .isInstanceOf(MdmUnavailableException.class)
        .hasMessage("MDM unavailable")
        .cause()
        .isInstanceOf(FeignException.class);
  }

  @Test
  void decode_leavesOtherStatusesToTheDefaultDecoder() {
    Exception decoded = decoder.decode("MdmClient#searchProducts", response(400));

    assertThat(decoded).isInstanceOf(FeignException.class).isNotInstanceOf(MdmUnavailableException.class);
  }

  private static Response response(int status) {
    Request request = Request.create(
        Request.HttpMethod.GET,
        "http://mdm/search",
        Collections.emptyMap(),
        null,
        StandardCharsets.UTF_8,
        new RequestTemplate());
    return Response.builder()
        .status(status)
        .reason("error")
        .request(request)
        .headers(Collections.emptyMap())
        .build();
  }
}
