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
package ch.qos.logback.classic.spi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import org.slf4j.message.Message;

/**
 * A read/write and serializable implementation of {@link ILoggingEvent}.
 *
 * @author Ceki G&uuml;lc&uuml;

 */
public class PubLoggingEventVO implements ILoggingEvent, Serializable {


  private static final long serialVersionUID = -3385765861078946218L;

  private static final int NULL_ARGUMENT_ARRAY = -1;
  private static final String NULL_ARGUMENT_ARRAY_ELEMENT = "NULL_ARGUMENT_ARRAY_ELEMENT";

  public String threadName;
  public String loggerName;
  public LoggerContextVO loggerContextVO;

  public transient Level level;
  public Message message;

  public IThrowableProxy throwableProxy;
  public StackTraceElement[] callerDataArray;
  public Marker marker;
  public Map<String, String> mdcPropertyMap;
  public long timeStamp;

  public String getThreadName() {
    return threadName;
  }

  public LoggerContextVO getLoggerContextVO() {
    return loggerContextVO;
  }

  public String getLoggerName() {
    return loggerName;
  }

  public Level getLevel() {
    return level;
  }

  public Message getMessage() {
    return message;
  }

  public String getFormattedMessage() {
    if (message == null) {
      return null;
    }
    return message.getFormattedMessage();
  }

  public IThrowableProxy getThrowableProxy() {
    return throwableProxy;
  }

  public StackTraceElement[] getCallerData() {
    return callerDataArray;
  }

  public boolean hasCallerData() {
    return callerDataArray != null;
  }

  public Marker getMarker() {
    return marker;
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  public long getContextBirthTime() {
    return loggerContextVO.getBirthTime();
  }

  public LoggerContextVO getContextLoggerRemoteView() {
    return loggerContextVO;
  }

  public Map<String, String> getMDCPropertyMap() {
    return mdcPropertyMap;
  }

  public void prepareForDeferredProcessing() {
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeInt(level.levelInt);
    out.writeObject(message);
  }

  private void readObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    in.defaultReadObject();
    int levelInt = in.readInt();
    level = Level.toLevel(levelInt);
    message = (Message) in.readObject();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((message == null) ? 0 : message.hashCode());
    result = prime * result
        + ((threadName == null) ? 0 : threadName.hashCode());
    result = prime * result + (int) (timeStamp ^ (timeStamp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final PubLoggingEventVO other = (PubLoggingEventVO) obj;
    if (message == null) {
      if (other.message != null)
        return false;
    } else if (!message.equals(other.message))
      return false;

    if (loggerName == null) {
      if (other.loggerName != null)
        return false;
    } else if (!loggerName.equals(other.loggerName))
      return false;

    if (threadName == null) {
      if (other.threadName != null)
        return false;
    } else if (!threadName.equals(other.threadName))
      return false;
    if (timeStamp != other.timeStamp)
      return false;

    if (marker == null) {
      if (other.marker != null)
        return false;
    } else if (!marker.equals(other.marker))
      return false;

    if (mdcPropertyMap == null) {
      if (other.mdcPropertyMap != null)
        return false;
    } else if (!mdcPropertyMap.equals(other.mdcPropertyMap))
      return false;
    return true;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(timeStamp);
    sb.append(" ");
    sb.append(level);
    sb.append(" [");
    sb.append(threadName);
    sb.append("] ");
    sb.append(loggerName);
    sb.append(" - ");
    sb.append(getFormattedMessage());
    return sb.toString();
  }

}
