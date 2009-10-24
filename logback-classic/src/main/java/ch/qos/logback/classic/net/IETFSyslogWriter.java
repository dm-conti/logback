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
package ch.qos.logback.classic.net;

import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.net.SyslogWriter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.Socket;

/**
 * SyslogWriter is a wrapper around the {@link java.net.DatagramSocket} class so that it
 * behaves like a {@link java.io.Writer}.
 */
public class IETFSyslogWriter extends SyslogWriter {

  /**
   * The default reconnection delay (30000 milliseconds or 30 seconds).
   */
  static final int DEFAULT_RECONNECTION_DELAY = 30000;

  private static final int MAX_LEN = 8192;
  /**
   * The maximum length after which we discard the existing string buffer and
   * start anew.
   */
  private IOHandler handler;
  protected int reconnectionDelay = DEFAULT_RECONNECTION_DELAY;
  protected ContextAware base;
  boolean isUDP;

  public IETFSyslogWriter(String syslogHost, int port, TransportType transport, ContextAware base)
      throws UnknownHostException, SocketException {
    super(syslogHost, port);
    this.base = base;
    switch(transport) {
      case UDP:
        isUDP = true;
        break;
      case TCP:
        handler = new TCPIOHandler();
        break;
      default:
        throw new IllegalArgumentException("Unsupported transport type " + transport.name());
    }
  }

  public void flush() throws IOException {
    if (isUDP) {
      StringBuilder sb = new StringBuilder();
      sb.append(Integer.toString(buf.length()));
      sb.append(" ");
      sb.append(buf.toString());
      buf = new StringBuffer(sb.toString());
      super.flush();
    } else {
      handler.flush();
    }
  }

  public void close() {
    if (isUDP) {
      super.close();
    } else {
      handler.close();
      handler = null;
    }
  }

  /**
   * The <b>ReconnectionDelay</b> option takes a positive integer representing
   * the number of milliseconds to wait between each failed connection attempt
   * to the server. The default value of this option is 30000 which corresponds
   * to 30 seconds.
   *
   * <p>
   * Setting this option to zero turns off reconnection capability.
   * @param delay The number of milleseconds to wait.
   */
  public void setReconnectionDelay(int delay) {
    this.reconnectionDelay = delay;
  }

  /**
   * Returns value of the <b>ReconnectionDelay</b> option.
   * @return the number of milliseconds to wait.
   */
  public int getReconnectionDelay() {
    return reconnectionDelay;
  }

  private interface IOHandler {
    public void flush() throws IOException;
    public void close();
  }

  private class TCPIOHandler implements IOHandler {
    private Connector connector;
    protected OutputStreamWriter osw;

    protected int counter = 0;

    public TCPIOHandler() {
      connect();
    }

    public void flush() throws IOException {
      if (osw != null) {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toString(buf.length()));
        sb.append(" ");
        sb.append(buf.toString());
        try {
          osw.write(sb.toString());
          // addInfo("=========Flushing.");
          osw.flush();
          if (++counter >= CoreConstants.OOS_RESET_FREQUENCY) {
            counter = 0;
          }
        } catch (IOException e) {
          if (osw != null) {
              osw.close();
          }
          osw = null;
          base.addWarn("Detected problem with connection: " + e);
          if (reconnectionDelay > 0) {
            fireConnector();
          }
        } finally {
          // clean up for next round
          if (buf.length() > MAX_LEN) {
            buf = new StringBuffer();
          } else {
            buf.setLength(0);
          }
        }
      }
    }

    public void close() {
      cleanUp();
    }

    private void connect() {
      try {
        // First, close the previous connection if any.
        cleanUp();
        osw = new OutputStreamWriter(new Socket(address, port).getOutputStream());
      } catch (IOException e) {

        String msg = "Could not connect to remote logback server at ["
            + address.getHostName() + "].";
        if (reconnectionDelay > 0) {
          msg += " We will try again later.";
          fireConnector(); // fire the connector thread
        }
        base.addWarn(msg, e);
      }
    }

    /**
     * Drop the connection to the remote host and release the underlying connector
     * thread if it has been created
     */
    private void cleanUp() {
      if (osw != null) {
        try {
          osw.close();
        } catch (IOException ioe) {
          base.addWarn("Error closing stream " + ioe.getMessage());
        }

        osw = null;
      }
      if (connector != null) {
        base.addInfo("Interrupting the connector.");
        connector.interrupted = true;
        connector = null; // allow gc
      }
    }

    private void fireConnector() {
      if (connector == null) {
        base.addInfo("Starting a new connector thread.");
        connector = new Connector();
        connector.setDaemon(true);
        connector.setPriority(Thread.MIN_PRIORITY);
        connector.start();
      }
    }

    /**
     * The Connector will reconnect when the server becomes available again. It
     * does this by attempting to open a new connection every
     * <code>reconnectionDelay</code> milliseconds.
     * <p/>
     * <p/>
     * It stops trying whenever a connection is established. It will restart to
     * try reconnect to the server when previpously open connection is droppped.
     *
     * @since 0.8.4
     */
    class Connector extends Thread {

      boolean interrupted = false;

      public void run() {
        Socket socket;
        while (!interrupted) {
          try {
            sleep(reconnectionDelay);
            base.addInfo("Attempting connection to " + address.getHostName());
            socket = new Socket(address, port);
            synchronized (this) {
              osw = new OutputStreamWriter(socket.getOutputStream());
              connector = null;
              base.addInfo("Connection established. Exiting connector thread.");
              break;
            }
          } catch (InterruptedException e) {
            base.addInfo("Connector interrupted. Leaving loop.");
            return;
          } catch (java.net.ConnectException e) {
            base.addInfo("Remote host " + address.getHostName()
                + " refused connection.");
          } catch (IOException e) {
            base.addInfo("Could not connect to " + address.getHostName()
                + ". Exception is " + e);
          }
        }
        // addInfo("Exiting Connector.run() method.");
      }

      /**
       * public void finalize() { LogLog.debug("Connector finalize() has been
       * called."); }
       */
    }
  }

}