package uk.gov.defra.trade.imports.exceptions;

public class MdmUnavailableException extends RuntimeException {

  public MdmUnavailableException(String message) {
    super(message);
  }

  public MdmUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
