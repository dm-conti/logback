package ch.qos.logback.classic.pattern;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class MDCConverter extends MapConverter {

  public MDCConverter() {
  }

  protected Map<String, String> getMap(ILoggingEvent event) {
    return event.getMDCPropertyMap();
  }

  protected String get(ILoggingEvent event, String key) {
    return event.getMDCPropertyMap().get(key);
  }
}
