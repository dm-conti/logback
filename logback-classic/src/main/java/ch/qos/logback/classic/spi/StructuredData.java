package ch.qos.logback.classic.spi;

import org.slf4j.ext.EventData;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Map;
import java.util.HashMap;

/**
 *
 */
public class StructuredData {

  private static final String EVENT_DATA = "org.slf4j.ext.EventData";
  private static Class eventDataClass;

  private final EventData eventData;

  static {
    try {
      eventDataClass = Class.forName(EVENT_DATA);
    } catch (ClassNotFoundException e) {
      // No support for EventData.
    }
  }

  public static boolean isMarker(Marker marker) {
    return eventDataClass != null && marker != null && MarkerFactory.getMarker("EVENT") == marker;
  }

  public static StructuredData getStructuredData(String msg, Object[] args) {

    EventData data;
    if (args != null && args.length > 0 && args[1] instanceof EventData) {
      data = (EventData) args[1];
    } else {
      try {
        data = new EventData(msg);
      } catch (Exception ex) {
        return null;
      }
    }
    return new StructuredData(data);
  }

  public StructuredData(Map<String, String>data) {
    eventData = new EventData();
    for (Map.Entry<String, String>entry : data.entrySet())
    {
      eventData.put(entry.getKey(), entry.getValue());
    }
  }

  private StructuredData(EventData data) {
    eventData = data;
  }

  public String getEventType() {
    return eventData.getEventType();
  }

  public String getEventMessage() {
    return eventData.getMessage();
  }

  public Map<String, String> getMap() {
    return getMap(false);
  }
  public Map<String, String> getMap(boolean full) {
    Map<String, String> map = new HashMap<String, String>();
    for (Map.Entry<String, Object> entry : eventData.getEventMap().entrySet()) {
      if (full || (!EventData.EVENT_TYPE.equals(entry.getKey()) &&
          !EventData.EVENT_MESSAGE.equals(entry.getKey()))) {
        map.put(entry.getKey(), entry.getValue().toString());
      }
    }
    return map;
  }
}
