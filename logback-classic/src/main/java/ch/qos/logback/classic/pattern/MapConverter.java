package ch.qos.logback.classic.pattern;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;

public abstract class MapConverter extends ClassicConverter {

  String key;
  String separator = ",";
  boolean isQuoted;
  boolean leadingSpace;
  protected static final String EMPTY_STRING = "";

  public MapConverter() {
  }

  @Override
  public void start() {
    key = ConverterOptions.getFirstOption(MapOption.class, getOptionList());

    Map<MapOption, String> options = ConverterOptions.getOptions(MapOption.class, getOptionList());

    for (Map.Entry<MapOption, String> entry : options.entrySet()) {
      switch (entry.getKey()) {
        case SEPARATOR:
          separator = entry.getValue();
          break;
        case QUOTED:
          isQuoted = Boolean.parseBoolean(entry.getValue());
          break;
        case LEADING_SPACE:
          leadingSpace = Boolean.parseBoolean(entry.getValue());
          break;
      }
    }
    super.start();
  }

  @Override
  public void stop() {
    super.stop();
    key = null;
  }

  protected abstract Map<String, String> getMap(ILoggingEvent event);

  protected abstract String get(ILoggingEvent event, String key);


  @Override
  public String convert(ILoggingEvent event) {
    Map<String, String> map = getMap(event);

    if (map == null) {
      return EMPTY_STRING;
    }

    if (key == null) {
      // if no key is specified, return all the
      // values present in the MDC, separated with a single space.
      StringBuilder buf = new StringBuilder();
      String quoteChar = isQuoted ? "\"" : "";
      for (Map.Entry<String, String> entry : map.entrySet()) {
        if (buf.length() > 0 || leadingSpace) {
          buf.append(separator).append(" ");
        }
        buf.append(entry.getKey()).append('=').append(quoteChar).append(entry.getValue()).append(quoteChar);
      }
      return buf.toString();
    }

    String value = get(event, key);
    if (value != null) {
      return value;
    } else {
      return EMPTY_STRING;
    }
  }
}