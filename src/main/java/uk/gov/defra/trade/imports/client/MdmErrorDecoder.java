package uk.gov.defra.trade.imports.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import uk.gov.defra.trade.imports.exceptions.MdmUnavailableException;

public class MdmErrorDecoder implements ErrorDecoder {

  private final ErrorDecoder defaultDecoder = new Default();

  @Override
  public Exception decode(String methodKey, Response response) {
    Exception decoded = defaultDecoder.decode(methodKey, response);
    return switch (response.status()) {
      case 502, 503, 504 -> new MdmUnavailableException("MDM unavailable", decoded);
      default -> decoded;
    };
  }
}
