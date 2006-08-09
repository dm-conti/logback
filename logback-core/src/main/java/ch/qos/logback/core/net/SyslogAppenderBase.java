package ch.qos.logback.core.net;

import java.io.IOException;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;

public abstract class SyslogAppenderBase extends AppenderBase {

  Layout layout;
  int facility;
  String facilityStr;
  String syslogHost;
  SyslogWriter sw;
  //private String localHostname;
  
  public void start() {
    int errorCount = 0;
    if (facilityStr == null) {
      addError("The Facility option is mandatory");
      errorCount++;
    }
    
    facility = facilityStringToint(facilityStr);
    
    layout = getLayout();
    
    if(errorCount == 0) {
      super.start();
    }
  }
  
  abstract public Layout buildLayout(int facility);
  
  abstract public int getSeverityForEvent(Object eventObject);
  
  @Override
  protected void append(Object eventObject) {
    if(!isStarted()) {
      return;
    }
    try {
      String msg = layout.doLayout(eventObject);
      sw.write(msg);
      sw.flush();
    } catch(IOException ioe) {
      addError("Failed to send diagram to "+syslogHost, ioe);
      stop();
      
    }
  }

  /**
   * Returns the integer value corresponding to the named syslog facility.
   * 
   * @throws IllegalArgumentException if the facility string is not recognized
   * */
  static public int facilityStringToint(String facilityStr) {
    if ("KERN".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_KERN;
    } else if ("USER".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_USER;
    } else if ("MAIL".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_MAIL;
    } else if ("DAEMON".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_DAEMON;
    } else if ("AUTH".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_AUTH;
    } else if ("SYSLOG".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_SYSLOG;
    } else if ("LPR".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_LPR;
    } else if ("NEWS".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_NEWS;
    } else if ("UUCP".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_UUCP;
    } else if ("CRON".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_CRON;
    } else if ("AUTHPRIV".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_AUTHPRIV;
    } else if ("FTP".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_FTP;
    } else if ("LOCAL0".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_LOCAL0;
    } else if ("LOCAL1".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_LOCAL1;
    } else if ("LOCAL2".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_LOCAL2;
    } else if ("LOCAL3".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_LOCAL3;
    } else if ("LOCAL4".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_LOCAL4;
    } else if ("LOCAL5".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_LOCAL5;
    } else if ("LOCAL6".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_LOCAL6;
    } else if ("LOCAL7".equalsIgnoreCase(facilityStr)) {
      return SyslogConstants.LOG_LOCAL7;
    } else {
      throw new IllegalArgumentException(facilityStr + " is not a valid syslog facility string");
    }
  }

  
  /**
   * Returns the value of the <b>SyslogHost</b> option.
   */
  public String getSyslogHost() {
    return syslogHost;
  }

  /**
   * The <b>SyslogHost</b> option is the name of the the syslog host where log
   * output should go.
   *
   * <b>WARNING</b> If the SyslogHost is not set, then this appender will fail.
   */
  public void setSyslogHost(String syslogHost) {
    this.syslogHost = syslogHost;
  }

  /**
   * Returns the string value of the <b>Facility</b> option.
   *
   * See {@link #setFacility} for the set of allowed values.
   */
  public String getFacility() {
    return facilityStr;
  }

  /**
   * The <b>Facility</b> option must be set one of the strings KERN,
   * USER, MAIL, DAEMON, AUTH, SYSLOG, LPR, NEWS, UUCP, CRON, AUTHPRIV, FTP,
   * NTP, AUDIT, ALERT, CLOCK, LOCAL0, LOCAL1, LOCAL2, LOCAL3, LOCAL4, LOCAL5,
   * LOCAL6, LOCAL7. Case is not important.
   *
   * <p>See {@link SyslogConstants} and RFC 3164 for more information about the
   * <b>Facility</b> option.
   */
  public void setFacility(String facilityStr) {
    if(facilityStr != null) {
      facilityStr = facilityStr.trim();
    }
    this.facilityStr = facilityStr;
  }
}