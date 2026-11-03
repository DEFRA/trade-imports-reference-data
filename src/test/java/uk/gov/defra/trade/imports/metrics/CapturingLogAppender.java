package uk.gov.defra.trade.imports.metrics;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.LoggerFactory;

/**
 * A test-only appender that keeps every event one class logs, with the MDC as it was when the
 * event was logged.
 */
public class CapturingLogAppender extends AppenderBase<ILoggingEvent> implements AutoCloseable {

  private final List<ILoggingEvent> events = new CopyOnWriteArrayList<>();
  private final Logger logger;

  private CapturingLogAppender(Logger logger) {
    this.logger = logger;
  }

  /** Starts capturing what the given class logs, until closed. */
  public static CapturingLogAppender attachedTo(Class<?> loggingClass) {
    var appender = new CapturingLogAppender((Logger) LoggerFactory.getLogger(loggingClass));
    appender.start();
    appender.logger.addAppender(appender);
    return appender;
  }

  public List<ILoggingEvent> events() {
    return events;
  }

  /** The event's SLF4J key-value pairs by key, empty when it has none. */
  public static Map<String, Object> keyValues(ILoggingEvent event) {
    Map<String, Object> keyValues = new HashMap<>();
    if (event.getKeyValuePairs() != null) {
      event.getKeyValuePairs().forEach(pair -> keyValues.put(pair.key, pair.value));
    }
    return keyValues;
  }

  @Override
  protected void append(ILoggingEvent event) {
    // The MDC is read lazily, and by then the logging thread has cleared it.
    event.prepareForDeferredProcessing();
    events.add(event);
  }

  @Override
  public void close() {
    logger.detachAppender(this);
    stop();
  }
}
