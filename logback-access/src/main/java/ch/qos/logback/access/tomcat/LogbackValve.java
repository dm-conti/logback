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
package ch.qos.logback.access.tomcat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import ch.qos.logback.access.AccessConstants;
import ch.qos.logback.access.joran.JoranConfigurator;
import ch.qos.logback.access.spi.AccessEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.BasicStatusManager;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import ch.qos.logback.core.spi.FilterAttachable;
import ch.qos.logback.core.spi.FilterAttachableImpl;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * This class is an implementation of tomcat's Valve interface, by extending
 * ValveBase.
 * 
 * <p>For more information on using LogbackValve please refer to the online
 * documentation on <a
 * href="http://logback.qos.ch/access.html#tomcat">logback-acces and tomcat</a>.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * @author S&eacute;bastien Pennec
 */
public class LogbackValve extends ValveBase implements Lifecycle, Context,
    AppenderAttachable<AccessEvent>, FilterAttachable<AccessEvent> {

  public final static String DEFAULT_CONFIG_FILE = "conf" + File.separatorChar
      + "logback-access.xml";
  
  private long birthTime = System.currentTimeMillis();
  Object configurationLock = new Object();

  
  // Attributes from ContextBase:
  private String name;
  StatusManager sm = new BasicStatusManager();
  // TODO propertyMap should be observable so that we can be notified
  // when it changes so that a new instance of propertyMap can be
  // serialized. For the time being, we ignore this shortcoming.
  Map<String, String> propertyMap = new HashMap<String, String>();
  Map<String, Object> objectMap = new HashMap<String, Object>();
  private FilterAttachableImpl<AccessEvent> fai = new FilterAttachableImpl<AccessEvent>();

  AppenderAttachableImpl<AccessEvent> aai = new AppenderAttachableImpl<AccessEvent>();
  String filename;
  boolean quiet;
  boolean started;
  boolean alreadySetLogbackStatusManager = false;

  public LogbackValve() {
    putObject(CoreConstants.EVALUATOR_MAP, new HashMap());
  }

  public void start() {
    if (filename == null) {
      String tomcatHomeProperty = OptionHelper
          .getSystemProperty("catalina.home");

      filename = tomcatHomeProperty + File.separatorChar + DEFAULT_CONFIG_FILE;
      getStatusManager().add(
          new InfoStatus("filename property not set. Assuming [" + filename
              + "]", this));
    }
    File configFile = new File(filename);
    if (configFile.exists()) {
      try {
        JoranConfigurator jc = new JoranConfigurator();
        jc.setContext(this);
        jc.doConfigure(filename);
      } catch (JoranException e) {
        // TODO can we do better than printing a stack trace on syserr?
        e.printStackTrace();
      }
    } else {
      getStatusManager().add(
          new WarnStatus("[" + filename + "] does not exist", this));
    }

    if (!quiet) {
      StatusPrinter.print(getStatusManager());
    }

    started = true;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public boolean isQuiet() {
    return quiet;
  }

  public void setQuiet(boolean quiet) {
    this.quiet = quiet;
  }

  public void invoke(Request request, Response response) throws IOException,
      ServletException {

    try {

      if (!alreadySetLogbackStatusManager) {
        alreadySetLogbackStatusManager = true;
        org.apache.catalina.Context tomcatContext = request.getContext();
        if (tomcatContext != null) {
          ServletContext sc = tomcatContext.getServletContext();
          if (sc != null) {
            sc.setAttribute(AccessConstants.LOGBACK_STATUS_MANAGER_KEY,
                getStatusManager());
          }
        }
      }

      getNext().invoke(request, response);

      TomcatServerAdapter adapter = new TomcatServerAdapter(request, response);
      AccessEvent accessEvent = new AccessEvent(request, response, adapter);

      if (getFilterChainDecision(accessEvent) == FilterReply.DENY) {
        return;
      }

      // TODO better exception handling
      aai.appendLoopOnAppenders(accessEvent);
    } finally {
      request
          .removeAttribute(AccessConstants.LOGBACK_STATUS_MANAGER_KEY);
    }
  }

  public void stop() {
    started = false;
  }

  public void addAppender(Appender<AccessEvent> newAppender) {
    aai.addAppender(newAppender);
  }

  public Iterator<Appender<AccessEvent>> iteratorForAppenders() {
    return aai.iteratorForAppenders();
  }

  public Appender<AccessEvent> getAppender(String name) {
    return aai.getAppender(name);
  }

  public boolean isAttached(Appender<AccessEvent> appender) {
    return aai.isAttached(appender);
  }

  public void detachAndStopAllAppenders() {
    aai.detachAndStopAllAppenders();

  }

  public boolean detachAppender(Appender<AccessEvent> appender) {
    return aai.detachAppender(appender);
  }

  public boolean detachAppender(String name) {
    return aai.detachAppender(name);
  }

  public String getInfo() {
    return "Logback's implementation of ValveBase";
  }

  // Methods from ContextBase:
  public StatusManager getStatusManager() {
    return sm;
  }

  public Map<String, String> getPropertyMap() {
    return propertyMap;
  }

  public void putProperty(String key, String val) {
    this.propertyMap.put(key, val);
  }

  public String getProperty(String key) {
    return (String) this.propertyMap.get(key);
  }

  public Object getObject(String key) {
    return objectMap.get(key);
  }

  public void putObject(String key, Object value) {
    objectMap.put(key, value);
  }

  public void addFilter(Filter<AccessEvent> newFilter) {
    fai.addFilter(newFilter);
  }

  public Filter getFirstFilter() {
    return fai.getFirstFilter();
  }

  public void clearAllFilters() {
    fai.clearAllFilters();
  }

  public List<Filter<AccessEvent>> getCopyOfAttachedFiltersList() {
    return fai.getCopyOfAttachedFiltersList();
  }
  
  public FilterReply getFilterChainDecision(AccessEvent event) {
    return fai.getFilterChainDecision(event);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (this.name != null) {
      throw new IllegalStateException(
          "LogbackValve has been already given a name");
    }
    this.name = name;
  }

  public long getBithTime() {
    return birthTime;
  }

  public Object getConfigurationLock() {
    return configurationLock;
  }
  
  // ====== Methods from catalina Lifecycle =====

  public void addLifecycleListener(LifecycleListener arg0) {
    // dummy NOP implementation
  }

  public LifecycleListener[] findLifecycleListeners() {
    return new LifecycleListener[0];
  }

  public void removeLifecycleListener(LifecycleListener arg0) {
    // dummy NOP implementation
  }

}
