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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import org.slf4j.StructuredData;
import org.slf4j.StructuredDataImpl;
import org.slf4j.StructuredDataId;
import org.slf4j.impl.StaticLoggerBinder;
import org.slf4j.impl.StaticLoggerBinderFriend;
import org.slf4j.ext.EventLogger;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.mock.MockSyslogServer;
import ch.qos.logback.core.net.SyslogConstants;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.StatusPrinter;

import java.util.Map;

public class IETFSyslogAppenderTest {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void RFC5424() throws InterruptedException {
    int port = RandomUtil.getRandomServerPort();

    MockSyslogServer mockServer = new MockSyslogServer(1, port);
    mockServer.start();
    // give MockSyslogServer head start
    Thread.sleep(100);

    LoggerContext lc = new LoggerContext();
    lc.setName("test");
    IETFSyslogAppender sa = new IETFSyslogAppender();
    sa.setContext(lc);
    sa.setSyslogHost("localhost");
    sa.setFacility("MAIL");
    sa.setPort(port);
    sa.setAppName("SyslogAppenderTest");
    sa.setMessageId("Test001");
    sa.setSuffixPattern("[%thread] %logger %msg");
    sa.start();
    assertTrue(sa.isStarted());

    String loggerName = this.getClass().getName();
    Logger logger = lc.getLogger(loggerName);
    logger.addAppender(sa);
    String logMsg = "hello";
    logger.debug(logMsg);

    // wait max 2 seconds for mock server to finish. However, it should
    // much sooner than that.
    mockServer.join(8000);
    assertTrue(mockServer.isFinished());
    assertEquals(1, mockServer.getMessageList().size());
    String msg = mockServer.getMessageList().get(0);

    String threadName = Thread.currentThread().getName();

    int index = msg.indexOf(" ");
    int length = Integer.parseInt(msg.substring(0, index));
    assertTrue("Invalid message length", length > 0);
    msg = msg.substring(index+1);
    String expected = "<"
        + (SyslogConstants.LOG_MAIL + SyslogConstants.DEBUG_SEVERITY) + ">";
    assertTrue(msg.startsWith(expected));

    String first = "<\\d{2}>1 \\w{4}-\\d{2}-\\d{2}T\\d{2}(:\\d{2}){2}\\.\\d{1,3}[\\S]* [\\w.-]* [\\w.-]* - [\\w.-]* ";
    checkRegexMatch(msg, first + "\\[" + threadName + "\\] " + loggerName
        + " " + logMsg);
  }

    @Test
  public void formatMessage() throws InterruptedException {
    int port = RandomUtil.getRandomServerPort();

    MockSyslogServer mockServer = new MockSyslogServer(1, port);
    mockServer.start();
    // give MockSyslogServer head start
    Thread.sleep(100);

    LoggerContext lc = new LoggerContext();
    lc.setName("test");
    IETFSyslogAppender sa = new IETFSyslogAppender();
    sa.setContext(lc);
    sa.setSyslogHost("localhost");
    sa.setFacility("MAIL");
    sa.setPort(port);
    sa.setAppName("SyslogAppenderTest");
    sa.setMessageId("Test001");
    sa.start();
    assertTrue(sa.isStarted());

    String loggerName = this.getClass().getName();
    Logger logger = lc.getLogger(loggerName);
    logger.addAppender(sa);
    StructuredData data = new StructuredDataImpl((String) null, "Hello, ${Name}", null);
    data.put("Name", "John Smith");
    logger.debug("", data);

    // wait max 2 seconds for mock server to finish. However, it should
    // much sooner than that.
    mockServer.join(8000);
     StatusPrinter.print(lc);
    assertTrue(mockServer.isFinished());
    assertEquals(1, mockServer.getMessageList().size());
    String msg = mockServer.getMessageList().get(0);

    int index = msg.indexOf(" ");
    int length = Integer.parseInt(msg.substring(0, index));
    assertTrue("Invalid message length", length > 0);
    msg = msg.substring(index+1);

    String expected = "<"
        + (SyslogConstants.LOG_MAIL + SyslogConstants.DEBUG_SEVERITY) + ">";
    assertTrue(msg.startsWith(expected));

    String first = "<\\d{2}>1 \\w{4}-\\d{2}-\\d{2}T\\d{2}(:\\d{2}){2}\\.\\d{1,3}[\\S]* [\\w.-]* [\\w.-]* - [\\w.-]* - ";
    checkRegexMatch(msg, first + "Hello, John Smith");
  }

  @Test
  public void RFC5424_Event() throws InterruptedException {
    int port = RandomUtil.getRandomServerPort();

    MockSyslogServer mockServer = new MockSyslogServer(1, port);
    mockServer.start();
    // give MockSyslogServer head start
    Thread.sleep(100);

    StaticLoggerBinderFriend.reset();
    LoggerContext lc = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
    lc.setName("test");
    IETFSyslogAppender sa = new IETFSyslogAppender();
    sa.setContext(lc);
    sa.setSyslogHost("localhost");
    sa.setFacility("LOCAL0");
    sa.setPort(port);
    sa.setAppName("Banking");
    // Valid id's are a) reseverd (no '@') or b) of the form mysid@EnterpriseNumber.
    StructuredDataId sdId = new StructuredDataId("Event", 18060, null, null);
    //sa.setStructuredDataId(sdId);
    sa.setMdcIncluded(true);
    sa.start();
    assertTrue(sa.isStarted());

    Logger logger = lc.getLogger("EventLogger");
    logger.addAppender(sa);
    logger.setAdditive(false);
    MDC.clear();
    MDC.put("UserId", "TestUser");
    MDC.put("IpAddress","10.200.10.5");
    StructuredData data = new StructuredDataImpl(sdId, "Transfer succeeded", "Transfer");
    data.put("FromAccount", "12345601");
    data.put("ToAccount", "12345602");
    data.put("Amount", "55.00");
    EventLogger.logEvent(data);

    // wait max 2 seconds for mock server to finish. However, it should
    // much sooner than that.
    mockServer.join(8000);
    StatusPrinter.print(lc);
    assertTrue(mockServer.isFinished());
    assertEquals(1, mockServer.getMessageList().size());
    String msg = mockServer.getMessageList().get(0);

    int index = msg.indexOf(" ");
    int length = Integer.parseInt(msg.substring(0, index));
    assertTrue("Invalid message length", length > 0);
    msg = msg.substring(index+1);

    String expected = "<"
        + (SyslogConstants.LOG_LOCAL0 + SyslogConstants.INFO_SEVERITY) + ">";
    assertTrue(msg.startsWith(expected));

    String first = "<\\d{3}>1 \\w{4}-\\d{2}-\\d{2}T\\d{2}(:\\d{2}){2}\\.\\d{1,3}[\\S]* [\\w.-]* [\\w.-]* - [\\w.-]* ";
    checkRegexMatch(msg, first + "[\\[" + sdId + "( [\\w.-]*=\"[\\w.-]*\")*\\]]+ " + data.getMessage());
  }

  private void checkRegexMatch(String s, String regex) {
    assertTrue("The string ["+s+"] did not match regex ["+regex+"]", s.matches(regex));
  }
}