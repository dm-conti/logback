/**
 * LOGBack: the reliable, fast and flexible logging library for Java.
 * 
 * Copyright (C) 1999-2006, QOS.ch
 * 
 * This library is free software, you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation.
 */
package ch.qos.logback.core.rolling;

import ch.qos.logback.core.spi.LifeCycle;



/**
 * A <code>RollingPolicy</code> is responsible for performing the
 * rolling over of the active log file. The <code>RollingPolicy</code>
 * is also responsible for providing the <em>active log file</em>,
 * that is the live file where logging output will be directed.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * */
public interface RollingPolicy extends LifeCycle {
  
  /**
   * Rolls over log files according to implementation policy.  
   * <p>
   * <p>This method is invoked by {@link RollingFileAppender}, usually 
   * at the behest of its {@link TriggeringPolicy}.
   * 
   * @throws RolloverFailure Thrown if the rollover operation fails for any
   * reason.
   */
  public void rollover() throws RolloverFailure;

  /**
   * Get the new name of the active log file.
   * */  
  public String getActiveFileName();
}