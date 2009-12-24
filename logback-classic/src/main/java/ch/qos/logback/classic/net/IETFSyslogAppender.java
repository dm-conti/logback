/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2009, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.classic.net;

import java.io.IOException;
import java.net.UnknownHostException;
import java.net.SocketException;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.net.SyslogAppenderBase;
import ch.qos.logback.core.net.SyslogWriter;

/**
 * This appender can be used to send messages to a remote syslog daemon in IETF Syslog format (RFC 5424).
 * <p> For more information about this appender, please refer to the online manual at
 * http://logback.qos.ch/manual/appenders.html#IETFSyslogAppender
 *
 * @author Ralph Goers
 */
public class IETFSyslogAppender extends SyslogAppenderBase<ILoggingEvent> {
  String appName;
  String messageId;
  String structuredDataId;
  String enterpriseNumber;
  boolean mdcIncluded;
  String mdcId;

  PatternLayout prefixLayout = new PatternLayout();

  TransportType transport = TransportType.UDP;

  public Layout<ILoggingEvent> buildLayout(String facilityStr) {

    IETFSyslogLayout layout = new IETFSyslogLayout();
    layout.setFacility(facilityStr);
    layout.setAppName(appName);
    layout.setMessageId(messageId);
    layout.setStructuredDataId(structuredDataId);
    layout.setMdcIncluded(mdcIncluded);
    layout.setEnterpriseNumber(enterpriseNumber);
    layout.setContext(getContext());
    layout.setMdcId(mdcId);

    if (suffixPattern != null) {
      layout.setDefaultPattern(suffixPattern);
      layout.setStructuredPattern(suffixPattern);
    }
    layout.start();

    return layout;
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

  public SyslogWriter getWriter() throws UnknownHostException, SocketException {
    return new IETFSyslogWriter(syslogHost, port, transport, this);
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

  public String getEnterpriseNumber() {
    return enterpriseNumber;
  }

  public void setEnterpriseNumber(String enterpriseNumber) {
    this.enterpriseNumber = enterpriseNumber;
  }

  public boolean isMdcIncluded() {
    return mdcIncluded;
  }

  public void setMdcIncluded(boolean mdcIncluded) {
    this.mdcIncluded = mdcIncluded;
  }

  /**
   * Sets the Transport to use. Must be either TCP or UDP.
   * @param transport The transport type to use.
   */
  public void setTransport(String transport) {
    TransportType t = TransportType.valueOf(transport);
    if (t == null) {
      throw new IllegalArgumentException("Invalid transport type " + transport);
    }
    this.transport = t;
  }

  /**
   * Returns the Transport type being used.
   * @return the Transport type.
   */
  public String getTransport() {
    return transport.name();
  }
}