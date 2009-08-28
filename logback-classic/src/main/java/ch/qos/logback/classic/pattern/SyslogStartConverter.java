/**
 * LOGBack: the reliable, fast and flexible logging library for Java.
 *
 * Copyright (C) 1999-2006, QOS.ch
 *
 * This library is free software, you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation.
 */
package ch.qos.logback.classic.pattern;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StructuredData;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.net.SyslogAppenderBase;
import ch.qos.logback.core.CoreConstants;

public class SyslogStartConverter extends ClassicConverter {

  long lastTimestamp = -1;
  String timesmapStr = null;
  SimpleDateFormat simpleFormat;
  String localHostName;
  int facility;

  String appName;
  String messageId;
  String messageIdKey;
  String structuredDataId;
  boolean isRfc5254;

  public void start() {
    int errorCount = 0;

    String facilityStr = getFirstOption();
    if (facilityStr == null) {
      addError("was expecting a facility string as an option");
      return;
    }
    facility = SyslogAppenderBase.facilityStringToint(facilityStr);

    Map<SyslogOption, String> options = ConverterOptions.getOptions(SyslogOption.class, getOptionList());

    for (Map.Entry<SyslogOption, String> entry : options.entrySet()) {
      switch (entry.getKey()) {
        case APPNAME:
          appName = entry.getValue();
          break;
        case MESSAGEID:
          messageId = entry.getValue();
          break;
        case MESSAGEID_KEY:
          messageIdKey = entry.getValue();
          break;
        case RFC5254:
          isRfc5254 = Boolean.parseBoolean(entry.getValue());
          break;
      }
    }

    localHostName = getLocalHostname();
    try {
      // hours should be in 0-23, see also http://jira.qos.ch/browse/LBCLASSIC-48
      simpleFormat = isRfc5254 ? new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS000Z")
          : new SimpleDateFormat("MMM dd HH:mm:ss", new DateFormatSymbols(Locale.US));
    } catch (IllegalArgumentException e) {
      addError("Could not instantiate SimpleDateFormat", e);
      errorCount++;
    }

    if(errorCount == 0) {
      super.start();
    }
  }

  public String convert(ILoggingEvent event) {
    StringBuilder sb = new StringBuilder();

    int pri = facility + LevelToSyslogSeverity.convert(event);

    sb.append("<");
    sb.append(pri);
    sb.append(">");
    sb.append(computeTimeStampString(event.getTimeStamp()));
    sb.append(' ');
    sb.append(localHostName);
    sb.append(' ');

    if (isRfc5254) {
      sb.append(extendedHeader(event));
    }

    return sb.toString();
  }

  public String extendedHeader(ILoggingEvent event) {
    StringBuilder sb = new StringBuilder();
    if (appName != null) {
      sb.append(appName);
    } else if (event.getLoggerContextVO().getName() != null) {
      sb.append(event.getLoggerContextVO().getName());
    } else {
      sb.append("-");
    }
    sb.append(" ");
    sb.append(getProcId());
    sb.append(" ");
    Map<String, String> mdc = event.getMDCPropertyMap();
    StructuredData data = event.getStructuredData();

    if (data != null && data.getEventType() != null) {
      sb.append(data.getEventType());
    } else if (messageIdKey != null && mdc.containsKey(messageIdKey)) {
      sb.append(mdc.get(messageIdKey));
    } else if (messageId != null) {
      sb.append(messageId);
    } else {
      StackTraceElement[] cda = event.getCallerData();
      if (cda != null && cda.length > 0) {
        String fqClassName = cda[0].getClassName();
        int lastIndex = fqClassName.lastIndexOf(CoreConstants.DOT);
        if (lastIndex != -1) {
          sb.append(fqClassName.substring(lastIndex + 1, fqClassName.length()));
        } else {
          sb.append(fqClassName);
        }
      } else {
        sb.append("-");
      }
    }
    sb.append(" ");
    return sb.toString();
  }

  String getProcId() {
    return "-";
  }

  /**
   * This method gets the network name of the machine we are running on.
   * Returns "UNKNOWN_LOCALHOST" in the unlikely case where the host name
   * cannot be found.
   * @return String the name of the local host
   */
  public String getLocalHostname() {
    try {
      InetAddress addr = InetAddress.getLocalHost();
      return addr.getHostName();
    } catch (UnknownHostException uhe) {
      addError("Could not determine local host name", uhe);
      return "UNKNOWN_LOCALHOST";
    }
  }

  String computeTimeStampString(long now) {
    synchronized (this) {
      if (now != lastTimestamp) {
        lastTimestamp = now;
        timesmapStr = simpleFormat.format(new Date(now));
      }
      return timesmapStr;
    }
  }
}
