package ch.qos.logback.classic.pattern;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.PropertyContainer;
import ch.qos.logback.core.util.OptionHelper;
import org.slf4j.StructuredData;
import org.slf4j.MDC;

/**
 * Converts StructuredData into various formats:
 * 1. Structured Data that conforms to RFC 5424.
 * 2. Individual StructuredData fields
 * 3. Formats the message inserting data items by key name. For example:
 * StructuredData data = new StructuredDataImpl(null, "Hello, ${Name}", null);
 * data.getData().put("Name", "John Smith");
 *
 * and %SD{Message}% will result in
 *
 * "Hello, John Smith"
 */
public class StructuredDataConverter extends ClassicConverter {

  String key;
  String format;
  String defaultId;
  int enterpriseNumber = StructuredData.Id.RESERVED;
  boolean leadingSpace;
  boolean trailingSpace;
  boolean includeMDC;
  boolean hideNil;
  protected static final String EMPTY_STRING = "";
  protected static final String NIL_STRING = "-";
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
        case ENTERPRISE_NUMBER:
          enterpriseNumber = Integer.parseInt(entry.getValue());
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
        case HIDE_NIL:
          hideNil = Boolean.parseBoolean(entry.getValue());
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
      StringBuilder sb = new StringBuilder();
      StructuredData.Id id = data.getId();
      if (id == null) {
        if (defaultId != null) {
          id = new StructuredData.Id(defaultId, enterpriseNumber, null, null);  
        }
      } else {
        id = id.makeId(defaultId, enterpriseNumber);
      }
      String str = data.asString(format, id, maps);
      if (str != null && str.length() > 0) {
        if (leadingSpace) {
          sb.append(" ");
        }
        sb.append(str);
      } else if (!hideNil){
        sb.append(NIL_STRING);
      }
      if (trailingSpace) {
        sb.append(" ");
      }
      return sb.toString();
    }

    String msg = null;
    if (key.equalsIgnoreCase(MESSAGE)) {
      msg = OptionHelper.substVars(data.getMessage(), new SDContainer(data));
    } else if (key.equalsIgnoreCase(ID)) {
      msg = data.getId().toString();
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

  private class SDContainer implements PropertyContainer
  {
    private final StructuredData data;

    public SDContainer(StructuredData data) {
      this.data = data;
    }

    public String getProperty(String key) {
      String value = data.getData().get(key).toString();
      if (value != null) {
        return value;
      }
      if (includeMDC) {
        value = MDC.get(key);
      }
      return value;
    }
  }
}