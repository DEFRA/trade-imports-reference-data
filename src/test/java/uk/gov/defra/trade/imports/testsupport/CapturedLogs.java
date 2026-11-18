package uk.gov.defra.trade.imports.testsupport;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

public final class CapturedLogs {

  private CapturedLogs() {}

  /**
   * Runs the action and returns every event the owner's logger wrote meanwhile, from any thread,
   * with the MDC as it was when each event was logged.
   */
  public static List<ILoggingEvent> loggedBy(Class<?> loggerOwner, Runnable action) {
    Logger logger = (Logger) LoggerFactory.getLogger(loggerOwner);
    List<ILoggingEvent> events = new CopyOnWriteArrayList<>();
    AppenderBase<ILoggingEvent> appender =
        new AppenderBase<>() {
          @Override
          protected void append(ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            events.add(event);
          }
        };
    appender.start();
    logger.addAppender(appender);
    try {
      action.run();
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
    return events;
  }

  public static Map<String, Object> keyValues(ILoggingEvent event) {
    Map<String, Object> keyValues = new LinkedHashMap<>();
    List<KeyValuePair> pairs = event.getKeyValuePairs();
    if (pairs != null) {
      pairs.forEach(pair -> keyValues.put(pair.key, pair.value));
    }
    return keyValues;
  }
}
