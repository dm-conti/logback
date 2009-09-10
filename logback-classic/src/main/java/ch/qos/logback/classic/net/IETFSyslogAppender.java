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

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.pattern.ConverterOptions;
import ch.qos.logback.classic.pattern.SyslogOption;
import ch.qos.logback.classic.pattern.IETFSyslogStartConverter;
import ch.qos.logback.classic.pattern.StructuredDataOption;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.net.SyslogAppenderBase;
import ch.qos.logback.core.net.SyslogWriter;
import org.slf4j.StructuredData;

/**
 * This appender can be used to send messages to a remote syslog daemon. <p> For
 * more information about this appender, please refer to the online manual at
 * http://logback.qos.ch/manual/appenders.html#SyslogAppender
 *
 * @author Ceki G&uumllc&uuml;
 */
public class IETFSyslogAppender extends SyslogAppenderBase<ILoggingEvent> {
  public static final String DEFAULT_SUFFIX_PATTERN = "[%thread] %logger %msg";
  String appName;
  String messageId;
  String structuredDataId;
  boolean mdcIncluded;

  PatternLayout prefixLayout = new PatternLayout();
  PatternLayout structuredLayout;
  PatternLayout defaultLayout;

  public Layout<ILoggingEvent> buildLayout(String facilityStr) {
    ConverterOptions<SyslogOption> syslogOptions = new ConverterOptions<SyslogOption>(facilityStr);
    syslogOptions.add(SyslogOption.APPNAME, getAppName());
    syslogOptions.add(SyslogOption.MESSAGEID, getMessageId());

    String prefixPattern = "%syslogStart{" + syslogOptions.toString() + "}%nopex";

    prefixLayout.getInstanceConverterMap().put("syslogStart", IETFSyslogStartConverter.class.getName());

    prefixLayout.setPattern(prefixPattern);
    prefixLayout.setContext(getContext());
    prefixLayout.start();

    structuredLayout = new PatternLayout();
    defaultLayout = new PatternLayout();

    structuredLayout.getInstanceConverterMap().put("syslogStart", IETFSyslogStartConverter.class.getName());
    defaultLayout.getInstanceConverterMap().put("syslogStart", IETFSyslogStartConverter.class.getName());

    if (suffixPattern == null) {
      ConverterOptions<StructuredDataOption> options = new ConverterOptions<StructuredDataOption>();
      options.add(StructuredDataOption.LEADING_SPACE, false);
      options.add(StructuredDataOption.TRAILING_SPACE, true);
      options.add(StructuredDataOption.DEFAULT_ID, getStructuredDataId());
      options.add(StructuredDataOption.INCLUDE_MDC, isMdcIncluded());
      suffixPattern = "%SD{" + options.toString() + "}%SD{Message}";
      defaultLayout.setPattern(prefixPattern + DEFAULT_SUFFIX_PATTERN);
    } else {
      defaultLayout.setPattern(prefixPattern + suffixPattern);
    }
    structuredLayout.setPattern(prefixPattern + suffixPattern);
    structuredLayout.setContext(getContext());
    structuredLayout.start();
     
    defaultLayout.setContext(getContext());
    defaultLayout.start();

    return defaultLayout;
  }

  protected Layout<ILoggingEvent> getLayout(ILoggingEvent event) {
    Object[] args = event.getArgumentArray();
    if (args != null && args.length == 1 && args[0] instanceof StructuredData) {
      return structuredLayout;
    }
    return defaultLayout;
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


  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getStructuredDataId() {
    return structuredDataId;
  }

  public void setStructuredDataId(String structuredDataId) {
    this.structuredDataId = structuredDataId;
  }

  public boolean isMdcIncluded() {
    return mdcIncluded;
  }

  public void setMdcIncluded(boolean mdcIncluded) {
    this.mdcIncluded = mdcIncluded;
  }
}