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
package ch.qos.logback.classic;

import ch.qos.logback.classic.pattern.IETFSyslogStartConverter;
import ch.qos.logback.classic.pattern.SyslogOption;
import ch.qos.logback.classic.pattern.ConverterOptions;
import ch.qos.logback.classic.pattern.StructuredDataOption;

/**
 * <p>
 * Layout that matches the RFC 5424 specification.
 *
 */

public class IETFSyslogLayout extends PatternLayout {

  String facilityStr;
  String appName;
  String messageId;
  String structuredDataId;
  String enterpriseNumber;
  boolean mdcIncluded;

  public void start() {
    getInstanceConverterMap().put("syslogStart", IETFSyslogStartConverter.class.getName());
    if (facilityStr == null) {
      addError("The Facility option is mandatory");
    }
    ConverterOptions<SyslogOption> syslogOptions = new ConverterOptions<SyslogOption>(facilityStr);
    syslogOptions.add(SyslogOption.APPNAME, getAppName());
    syslogOptions.add(SyslogOption.MESSAGEID, getMessageId());

    ConverterOptions<StructuredDataOption> sdOptions = new ConverterOptions<StructuredDataOption>();
    sdOptions.add(StructuredDataOption.TRAILING_SPACE, true);
    if (structuredDataId != null) {
      sdOptions.add(StructuredDataOption.DEFAULT_ID, getStructuredDataId());
    }
    sdOptions.add(StructuredDataOption.ENTERPRISE_NUMBER, getEnterpriseNumber());
    sdOptions.add(StructuredDataOption.INCLUDE_MDC, isMdcIncluded());

    String prefixPattern = "%syslogStart{" + syslogOptions.toString() + "}%SD{" + sdOptions.toString() + "}%nopex";

    if (getPattern() == null) {
      setPattern(prefixPattern + "%SD{Message}");
    } else {
      setPattern(prefixPattern + getPattern());
    }

    super.start();
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
   * Returns the string value of the <b>Facility</b> option.
   *
   * See {@link #setFacility} for the set of allowed values.
   * @return the Facility string.
   */
  public String getFacility() {
    return facilityStr;
  }

  /**
   * The <b>Facility</b> option must be set one of the strings KERN, USER,
   * MAIL, DAEMON, AUTH, SYSLOG, LPR, NEWS, UUCP, CRON, AUTHPRIV, FTP, NTP,
   * AUDIT, ALERT, CLOCK, LOCAL0, LOCAL1, LOCAL2, LOCAL3, LOCAL4, LOCAL5,
   * LOCAL6, LOCAL7. Case is not important.
   *
   * <p>
   * See {@link ch.qos.logback.core.net.SyslogConstants} and RFC 3164 for more information about the
   * <b>Facility</b> option.
   * @param facilityStr The facility String.
   */
  public void setFacility(String facilityStr) {
    if (facilityStr != null) {
      facilityStr = facilityStr.trim();
    }
    this.facilityStr = facilityStr;
  }
}