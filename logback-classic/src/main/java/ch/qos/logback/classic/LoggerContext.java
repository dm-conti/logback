/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * 
 * Copyright (C) 1999-2006, QOS.ch
 * 
 * This library is free software, you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation.
 */

package ch.qos.logback.classic;

import java.util.HashMap;
import java.util.Hashtable;

import org.slf4j.ILoggerFactory;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreGlobal;
import ch.qos.logback.core.status.ErrorStatus;


/**
 * @author ceki
 */
public class LoggerContext extends ContextBase implements ILoggerFactory {

	public static final String ROOT_NAME = "root";
	
  final Logger root;
  private int size;
  private int noAppenderWarning = 0;
  
  // We want loggerCache to be synchronized so Hashtable is a good choice. In practice, it 
  // performs a little faster than the map returned by Collections.synchronizedMap at the 
  // cost of a very slightly higher memory footprint.
  private Hashtable<String, Logger> loggerCache;

  
  public LoggerContext() {
    super();
    this.root = new Logger("root", null, this);
    this.root.setLevel(Level.DEBUG);
    this.loggerCache = new Hashtable<String, Logger>();
    size = 1;
    putObject(CoreGlobal.EVALUATOR_MAP, new HashMap());
  }

  public final Logger getLogger(final Class clazz) {
    return getLogger(clazz.getName());
  }
  public final Logger getLogger(final String name) {

    //if we are asking for the root logger, then let us return it without wasting time
    if (ROOT_NAME.equalsIgnoreCase(name)) {
    	return root;
    }
    
    int i = 0;
    Logger logger = root;
    
    Logger childLogger = (Logger) loggerCache.get(name);
    // if we have the child, then let us return it without wasting time
    if (childLogger != null) {
      return childLogger;
    }

    String childName;
    while (true) {
      int h = name.indexOf('.', i);
      if (h == -1) {
        childName = name;
      } else {
        childName = name.substring(0, h);
      }
      // move i left of the last point
      i = h + 1;
      synchronized (logger) {
        childLogger = logger.getChildByName(childName);
        if (childLogger == null) {
          childLogger = logger.createChildByName(childName);
          loggerCache.put(childName, childLogger);
          incSize();
        }
      }
      logger = childLogger;
      if (h == -1) {
        return childLogger;
      }
    }
  }

  private synchronized void incSize() {
    size++;
  }

  int size() {
    return size;
  }

  /**
   * Check if the named logger exists in the hierarchy. If so return
   * its reference, otherwise returns <code>null</code>.
   *
   * @param name the name of the logger to search for.
   */
  Logger exists(String name) {
    return (Logger) loggerCache.get(name);
  }
  
  final void noAppenderDefinedWarning(final Logger logger) {
  	 if (noAppenderWarning++ == 0) {
  	      getStatusManager().add(new ErrorStatus(
	        "No appenders present in context ["+ getName() +"] for logger [" + logger.getName() + "].", logger));
  	 }
  }
}