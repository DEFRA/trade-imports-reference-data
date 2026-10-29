package uk.gov.defra.trade.imports.exceptions;

public class TokenEndpointUnavailableException extends RuntimeException {

  public TokenEndpointUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
