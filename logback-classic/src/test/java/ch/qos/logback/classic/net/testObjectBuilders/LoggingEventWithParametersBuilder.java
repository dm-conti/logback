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
package ch.qos.logback.classic.net.testObjectBuilders;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.slf4j.message.ParameterizedMessage;

public class LoggingEventWithParametersBuilder implements Builder {

  final String MSG = "aaaaabbbbbcccc {} cdddddaaaaabbbbbcccccdddddaaaa {}";

  private Logger logger = new LoggerContext()
      .getLogger(Logger.ROOT_LOGGER_NAME);

  public Object build(int i) {

    LoggingEvent le = new LoggingEvent();
    le.setTimeStamp(System.currentTimeMillis());

    Object[] aa = new Object[] { i, "HELLO WORLD [========== ]" + i };

    String msg = MSG + i;
    le.setMessage(new ParameterizedMessage(msg, aa));

    // compute formatted message
    // this forces le.formmatedMessage to be set (this is the whole point of the
    // exercise)
    le.getFormattedMessage();
    le.setLevel(Level.DEBUG);
    le.setLoggerName(logger.getName());
    le.setLoggerContextRemoteView(logger.getLoggerRemoteView().getLoggerContextView());
    le.setThreadName("threadName");

    return le;
  }
}
