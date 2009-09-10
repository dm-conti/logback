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
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Calendar;
import java.util.GregorianCalendar;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.net.SyslogAppenderBase;
import ch.qos.logback.core.CoreConstants;
import org.slf4j.StructuredData;

public class IETFSyslogStartConverter extends ClassicConverter {

  private static final DecimalFormat TWO_DIGIT = new DecimalFormat("00");
  private static final DecimalFormat FOUR_DIGIT = new DecimalFormat("0000");

  long lastTimestamp = -1;
  String timesmapStr = null;
  SimpleDateFormat simpleFormat;
  String localHostName;
  int facility;

  String appName;
  String messageId;

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
      }
    }

    localHostName = getLocalHostname();
    try {
      simpleFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
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
    sb.append(">1 ");
    sb.append(computeTimeStampString(event.getTimeStamp()));
    sb.append(' ');
    sb.append(localHostName);
    sb.append(' ');
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
    Object[] args = event.getArgumentArray();
    if (args != null && args.length == 1 && args[0] instanceof StructuredData) {
      sb.append(((StructuredData) args[0]).getType());
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
        StringBuilder buf = new StringBuilder();
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(now);
        buf.append(FOUR_DIGIT.format(cal.get(Calendar.YEAR)));
        buf.append("-");
        buf.append(TWO_DIGIT.format(cal.get(Calendar.MONTH) + 1));
        buf.append("-");
        buf.append(TWO_DIGIT.format(cal.get(Calendar.DAY_OF_MONTH)));
        buf.append("T");
        buf.append(TWO_DIGIT.format(cal.get(Calendar.HOUR_OF_DAY)));
        buf.append(":");
        buf.append(TWO_DIGIT.format(cal.get(Calendar.MINUTE)));
        buf.append(":");
        buf.append(TWO_DIGIT.format(cal.get(Calendar.SECOND)));

        int millis = cal.get(Calendar.MILLISECOND);
        if (millis != 0) {
          buf.append(".").append((int) ((float) millis / 10F));
        }

        int tzmin = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 60000;
        if (tzmin == 0) {
          buf.append("Z");
        } else {
          if (tzmin < 0) {
            tzmin = -tzmin;
            buf.append("-");
          } else {
            buf.append("+");
          }
          int tzhour = tzmin / 60;
          tzmin -= tzhour * 60;
          buf.append(TWO_DIGIT.format(tzhour));
          buf.append(":");
          buf.append(TWO_DIGIT.format(tzmin));
        }
        timesmapStr = buf.toString();
      }
      return timesmapStr;
    }
  }
}