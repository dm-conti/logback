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
import org.slf4j.impl.StaticLoggerBinder;
import org.slf4j.impl.StaticLoggerBinderFriend;
import org.slf4j.ext.EventLogger;
import org.slf4j.ext.StructuredDataImpl;

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

    String expected = "<"
        + (SyslogConstants.LOG_MAIL + SyslogConstants.DEBUG_SEVERITY) + ">";
    assertTrue(msg.startsWith(expected));

    String first = "<\\d{2}>1 \\w{4}-\\d{2}-\\d{2}T\\d{2}(:\\d{2}){2}\\.\\d{6}[\\S]* [\\w.-]* [\\w.-]* - [\\w.-]* ";
    checkRegexMatch(msg, first + "\\[" + threadName + "\\] " + loggerName
        + " " + logMsg);
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
    String sdId = "Event@18060";
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
    Map<String, Object> map = data.getData();
    map.put("FromAccount", "12345601");
    map.put("ToAccount", "12345602");
    map.put("Amount", "55.00");
    EventLogger.logEvent(data);

    // wait max 2 seconds for mock server to finish. However, it should
    // much sooner than that.
    mockServer.join(8000);
    StatusPrinter.print(lc);
    assertTrue(mockServer.isFinished());
    assertEquals(1, mockServer.getMessageList().size());
    String msg = mockServer.getMessageList().get(0);

    String expected = "<"
        + (SyslogConstants.LOG_LOCAL0 + SyslogConstants.INFO_SEVERITY) + ">";
    assertTrue(msg.startsWith(expected));

    String first = "<\\d{3}>1 \\w{4}-\\d{2}-\\d{2}T\\d{2}(:\\d{2}){2}\\.\\d{6}[\\S]* [\\w.-]* [\\w.-]* - [\\w.-]* ";
    checkRegexMatch(msg, first + "\\[" + sdId + "( [\\w.-]*=\"[\\w.-]*\")*\\] " + data.getMessage());
  }

  private void checkRegexMatch(String s, String regex) {
    assertTrue("The string ["+s+"] did not match regex ["+regex+"]", s.matches(regex));
  }
}