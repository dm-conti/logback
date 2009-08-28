package ch.qos.logback.classic.pattern;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StructuredData;

public class StructuerdDataConverter extends MapConverter {

  public StructuerdDataConverter() {
  }

  protected Map<String, String> getMap(ILoggingEvent event) {
    StructuredData data = event.getStructuredData();
    if (data == null) {
      return null;
    }
    return data.getMap();
  }

  public String get(ILoggingEvent event, String key) {
    StructuredData data = event.getStructuredData();
    if ("EventMessage".equals(key)) {
      return data.getEventMessage();
    } else if ("EventType".equals(key)) {
      return data.getEventType();
    }
    return data.getMap().get(key);
  }
}