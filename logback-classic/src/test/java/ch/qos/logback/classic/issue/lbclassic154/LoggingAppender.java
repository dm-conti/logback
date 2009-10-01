package ch.qos.logback.classic.issue.lbclassic154;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;


/**
 *
 */
public class LoggingAppender extends AppenderBase<ILoggingEvent>
{
    Logger logger;

    public void start()
    {
        super.start();
        logger = ((LoggerContext)getContext()).getLogger("Ignore");
    }

    protected void append(ILoggingEvent eventObject)
    {
        logger.debug("Ignore this");
    }
}
