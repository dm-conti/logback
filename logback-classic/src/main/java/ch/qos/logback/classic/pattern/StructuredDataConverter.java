package ch.qos.logback.classic.pattern;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.PropertyContainer;
import ch.qos.logback.core.util.OptionHelper;
import org.slf4j.MDC;
import org.slf4j.message.Message;
import org.slf4j.message.StructuredDataId;
import org.slf4j.message.StructuredDataMessage;

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
  int enterpriseNumber = StructuredDataId.RESERVED;
  boolean leadingSpace;
  boolean trailingSpace;
  boolean includeMDC;
  String mdcName = "mdc";
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
        case MDC_ELEMENT:
          mdcName = entry.getValue();
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
    Message message = event.getMessage();
    Map mdc = event.getMDCPropertyMap();

    boolean isStructured = message != null && message instanceof StructuredDataMessage;
    boolean isMDC = includeMDC && mdc != null && mdc.size() > 0;

    if (!isStructured && !isMDC) {
      return EMPTY_STRING;
    }

    StructuredDataMessage data = isStructured ? (StructuredDataMessage) message : null;
    boolean leadingDone = false;
    StructuredDataId id = null;

    if (key == null) {
      StringBuilder sb = new StringBuilder();
      if (data != null) {
        id = data.getId();
        if (id != null && defaultId != null) {
          id = id.makeId(defaultId, enterpriseNumber);
        } else if (defaultId != null) {
          id = new StructuredDataId(defaultId, enterpriseNumber, null, null);
        }
        if (id != null) {
          String str = data.asString(format, id);
          if (str != null && str.length() > 0) {
            if (leadingSpace) {
              leadingDone = true;
              sb.append(" ");
            }
            sb.append(str);
          }
        }
      }
      if (isMDC) {
        int ein = id == null || id.isReserved() ? enterpriseNumber : id.getEnterpriseNumber();
        if (ein > 0) {
          id = new StructuredDataId(mdcName, ein, null, null);
          StructuredDataMessage mdcData = new StructuredDataMessage(id, null, null);
          mdcData.putAll(mdc);
          String str = mdcData.asString(format, id);
          if (leadingSpace && !leadingDone) {
            sb.append(" ");
          }
          sb.append(str);
        }
      }
      if (sb.length() == 0 && !hideNil) {
          sb.append(NIL_STRING);
      }
      if (trailingSpace) {
        sb.append(" ");
      }
      return sb.toString();
    }

    String msg = null;
    if (data != null) {
      if (key.equalsIgnoreCase(MESSAGE)) {
        String txt = data.getMessageFormat();
        if (txt != null) {
          msg = OptionHelper.substVars(txt, new SDContainer(data));
        }
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
    }
    return msg == null ? EMPTY_STRING : msg;
  }

  private class SDContainer implements PropertyContainer
  {
    private final StructuredDataMessage data;

    public SDContainer(StructuredDataMessage data) {
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