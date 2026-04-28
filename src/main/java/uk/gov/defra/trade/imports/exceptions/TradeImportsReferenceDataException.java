package uk.gov.defra.trade.imports.exceptions;

/**
 * Exception thrown when a requested resource is not found.
 * Will be mapped to 404 Not Found by GlobalExceptionHandler.
 */
public class TradeImportsReferenceDataException extends RuntimeException {

    public TradeImportsReferenceDataException(String message) {
        super(message);
    }
}
