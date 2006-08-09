/**
 * LOGBack: the generic, reliable, fast and flexible logging framework.
 * 
 * Copyright (C) 1999-2006, QOS.ch
 * 
 * This library is free software, you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation.
 */


package ch.qos.logback.core.boolex;

import ch.qos.logback.core.spi.ContextAware;

/**
 * An EventEvaluator has the responsibility to evaluate whether a given an event
 * matches a given criteria. 
 * 
 * <p>Implementations are free to evaluate the event as they see fit. In 
 * particular, the evaluation results <em>may</em> depend on previous events.
 *    
 * @author Ceki G&uuml;lc&uuml;
 */

public interface EventEvaluator extends ContextAware {
  

  /**
   * Evaluates whether the event passed as parameter matches this evaluator's 
   * matching criteria.
   * 
   * <p>The <code>Evaluator</code> instance is free to evaluate the event as
   * it pleases. In particular, the evaluation results <em>may</em> depend on 
   * previous events. 
   * 
   * @param event The event to evaluate
   * @return true if there is a match, false otherwise. 
   * @throws NullPointerException can be thrown in presence of null values
   * @throws EvaluationException Thrown during evaluation
   */
  boolean evaluate(Object event) throws NullPointerException, EvaluationException;
  
  
  
  
  /**
   * Evaluators are named entities.
   * 
   * @return The name of this evaluator.
   */
  public String getName();

  
  /**
   * Evaluators are named entities.
   */
  public void setName(String name);
}