/**
 * LOGBack: the reliable, fast and flexible logging library for Java.
 *
 * Copyright (C) 1999-2006, QOS.ch
 *
 * This library is free software, you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation.
 */
package ch.qos.logback.core;

import java.util.Map;

import ch.qos.logback.core.spi.FilterAttachable;
import ch.qos.logback.core.status.StatusManager;


public interface Context extends FilterAttachable {

	
  StatusManager getStatusManager();
  
  /**
   * A Context can act as a store for various objects used
   * by LOGBack components.
   * 
   * @return The object stored under 'key'.
   */
  public Object getObject(String key);
 
  /**
   * Store an object under 'key'. If no object can be found, null is returned.
   * 
   * @param key
   * @param value
   */
  public void putObject(String key, Object value);
  
  /** 
   * Get the properties specific for this context.
   */
  public Map getPropertyMap();

  /** 
   * Get the property of this context.
   */
  public String getProperty(String key);

  /** 
   * Set a property of this context.
   */
  public void setProperty(String key, String value);
  
  
  /**
   * LB contexts have a notion of context-specific converter maps.
   * @return
   */
  public Map<String, String> getConverterMap();
  
  /**
   * Contexts are named objects.
   * 
   * @return the name for this context
   */
  public String getName();
  
  /**
   * The name of the context can be set only once.
   * @param name
   */
  public void setName(String name);
}