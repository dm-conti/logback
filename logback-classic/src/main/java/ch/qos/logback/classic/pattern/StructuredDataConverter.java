package ch.qos.logback.classic.pattern;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.StructuredData;
import org.slf4j.MDC;

public class StructuredDataConverter extends ClassicConverter {

  String key;
  String format;
  String defaultId;
  boolean leadingSpace;
  boolean trailingSpace;
  boolean includeMDC;
  protected static final String EMPTY_STRING = "";
  public static final String ID ="Id";
  public static final String MESSAGE = "Message";
  public static final String TYPE = "Type";

  @Override
  public void start() {
    key = ConverterOptions.getFirstOption(StructuredDataOption.class, getOptionList());

    Map<StructuredDataOption, String> options =
        ConverterOptions.getOptions(StructuredDataOption.class, getOptionList());

    for (Map.Entry<StructuredDataOption, String> entry : options.entrySet()) {
      switch (entry.getKey()) {
        case FORMAT:
          format = entry.getValue();
          break;
        case DEFAULT_ID:
          defaultId = entry.getValue();
          break;
        case LEADING_SPACE:
          leadingSpace = Boolean.parseBoolean(entry.getValue());
          break;
        case TRAILING_SPACE:
          trailingSpace = Boolean.parseBoolean(entry.getValue());
          break;
        case INCLUDE_MDC:
          includeMDC = Boolean.parseBoolean(entry.getValue());
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

  @Override
  public String convert(ILoggingEvent event) {
    Object objects[] = event.getArgumentArray();

    if (objects == null || objects.length != 1 || !(objects[0] instanceof StructuredData)) {
      return EMPTY_STRING;
    }

    StructuredData data = (StructuredData) objects[0];

    if (key == null) {
      Map[] maps = null;
      if (includeMDC) {
        maps = new Map[] { MDC.getCopyOfContextMap() };
      }
      String str = data.asString(format, defaultId, maps);
      if (str != null && str.length() > 0) {
        StringBuilder sb = new StringBuilder();
        if (leadingSpace) {
          sb.append(" ");
        }
        sb.append(str);
        if (trailingSpace) {
          sb.append(" ");
        }
        return sb.toString();
      }
      return EMPTY_STRING;
    }

    String msg = null;
    if (key.equalsIgnoreCase(ID)) {
      msg = data.getId();
    } else if (key.equalsIgnoreCase(MESSAGE)) {
      msg = data.getMessage();
    } else if (key.equalsIgnoreCase(TYPE)) {
      msg = data.getType();
    } else {
      Object obj = data.getData().get(key);
      if (obj != null) {
        msg = obj.toString();
      }
    }
    return msg == null ? EMPTY_STRING : msg;
  }
}