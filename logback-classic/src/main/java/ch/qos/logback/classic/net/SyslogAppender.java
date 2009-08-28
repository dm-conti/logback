/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 *
 * Copyright (C) 1999-2006, QOS.ch
 *
 * This library is free software, you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation.
 */
package ch.qos.logback.classic.net;

import java.io.IOException;
import java.util.Map;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.pattern.SyslogStartConverter;
import ch.qos.logback.classic.pattern.ConverterOptions;
import ch.qos.logback.classic.pattern.SyslogOption;
import ch.qos.logback.classic.pattern.StructuerdDataConverter;
import ch.qos.logback.classic.pattern.MapOption;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.net.SyslogAppenderBase;
import ch.qos.logback.core.net.SyslogWriter;

/**
 * This appender can be used to send messages to a remote syslog daemon. <p> For
 * more information about this appender, please refer to the online manual at
 * http://logback.qos.ch/manual/appenders.html#SyslogAppender
 *
 * @author Ceki G&uumllc&uuml;
 */
public class SyslogAppender extends SyslogAppenderBase<ILoggingEvent> {

  static final public String DEFAULT_SUFFIX_PATTERN = "[%thread] %logger %msg";

  PatternLayout prefixLayout = new PatternLayout();

  public Layout<ILoggingEvent> buildLayout(String facilityStr) {
    StringBuilder prefixPattern = new StringBuilder();
    ConverterOptions<SyslogOption> syslogOptions = new ConverterOptions<SyslogOption>(facilityStr);
    syslogOptions.add(SyslogOption.RFC5254, this.isRfc5424());
    syslogOptions.add(SyslogOption.APPNAME, getAppName());
    syslogOptions.add(SyslogOption.MESSAGEID, getMessageId());

    prefixPattern.append("%syslogStart{").append(syslogOptions.toString()).append("}").append("%nopex");

    Map<String, String> prefixConverterMap = prefixLayout.getInstanceConverterMap();

    PatternLayout fullLayout = new PatternLayout();
    Map<String, String> fullConverterMap = fullLayout.getInstanceConverterMap();

    if (suffixPattern == null) {
      StringBuilder suffixBuilder = new StringBuilder();
      if (isRfc5424() && getStructuredDataId() != null) {
        suffixBuilder.append("[").append(getStructuredDataId());
        ConverterOptions<MapOption> sdOptions = new ConverterOptions<MapOption>();
        sdOptions.add(MapOption.SEPARATOR, "");
        sdOptions.add(MapOption.QUOTED, "TRUE");
        sdOptions.add(MapOption.LEADING_SPACE, "TRUE");
        prefixConverterMap.put("structuredData", StructuerdDataConverter.class.getName());
        fullConverterMap.put("structuredData", StructuerdDataConverter.class.getName());
        ConverterOptions<MapOption> mdcOptions = new ConverterOptions<MapOption>();
        mdcOptions.add(MapOption.SEPARATOR, "");
        mdcOptions.add(MapOption.QUOTED, "TRUE");
        mdcOptions.add(MapOption.LEADING_SPACE, "TRUE");
        suffixBuilder.append("%structuredData{").append(sdOptions.toString()).append("}");
        suffixBuilder.append("%X{").append(mdcOptions.toString()).append("}");
        suffixBuilder.append("] %structuredData{EventMessage}" );
      } else {
        suffixBuilder.append(DEFAULT_SUFFIX_PATTERN);
      }

      suffixPattern = suffixBuilder.toString();
    }

    prefixConverterMap.put("syslogStart", SyslogStartConverter.class.getName());

    prefixLayout.setPattern(prefixPattern.toString());
    prefixLayout.setContext(getContext());
    prefixLayout.start();

    fullConverterMap.put("syslogStart", SyslogStartConverter.class.getName());

    fullLayout.setPattern(prefixPattern + suffixPattern);
    fullLayout.setContext(getContext());
    fullLayout.start();
    return fullLayout;
  }

  /*
   * Convert a level to equivalent syslog severity. Only levels for printing
   * methods i.e DEBUG, WARN, INFO and ERROR are converted.
   *
   * @see ch.qos.logback.core.net.SyslogAppenderBase#getSeverityForEvent(java.lang.Object)
   */
  @Override
  public int getSeverityForEvent(Object eventObject) {
    ILoggingEvent event = (ILoggingEvent) eventObject;
    return LevelToSyslogSeverity.convert(event);
  }

  @Override
  protected void postProcess(Object eventObject, SyslogWriter sw) {
    ILoggingEvent event = (ILoggingEvent) eventObject;

    String prefix = prefixLayout.doLayout(event);

    IThrowableProxy tp = event.getThrowableProxy();
    while (tp != null) {
      StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
      try {
        for (StackTraceElementProxy step : stepArray) {
          sw.write(prefix);
          sw.write(CoreConstants.TAB);
          sw.write(step.toString());
          sw.flush();
        }
      } catch (IOException e) {
        break;
      }
      tp = tp.getCause();
    }
  }
}
