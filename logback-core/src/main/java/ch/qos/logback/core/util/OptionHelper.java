/**
 * LOGBack: the reliable, fast and flexible logging library for Java.
 * 
 * Copyright (C) 1999-2006, QOS.ch
 * 
 * This library is free software, you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation.
 */
package ch.qos.logback.core.util;

import java.util.Properties;

import ch.qos.logback.core.CoreGlobal;


/**
 * @author Ceki Gulcu
 */
public class OptionHelper {

  @SuppressWarnings("unchecked")
  public static Object instantiateByClassName(String className, Class superClass)
      throws ClassNotFoundException, IncompatibleClassException,
      InstantiationException, IllegalAccessException {
    if (className == null) {
      throw new NullPointerException();
    }
    Class classObj = Class.forName(className);

    if (!superClass.isAssignableFrom(classObj)) {
      throw new IncompatibleClassException(superClass, classObj);
    }
    return classObj.newInstance();
  }

  /**
   * Find the value corresponding to <code>key</code> in <code>props</code>.
   * Then perform variable substitution on the found value.
   * 
   */
  public static String findAndSubst(String key, Properties props) {
    String value = props.getProperty(key);

    if (value == null) {
      return null;
    }

    try {
      return substVars(value, props);
    } catch (IllegalArgumentException e) {
      return value;
    }
  }

  final static String DELIM_START = "${";
  final static char DELIM_STOP = '}';
  final static int DELIM_START_LEN = 2;
  final static int DELIM_STOP_LEN = 1;
  /**
   * Perform variable substitution in string <code>val</code> from the values
   * of keys found the properties passed as parameter or in the system
   * properties.
   * 
   * <p>
   * The variable substitution delimeters are <b>${</b> and <b>}</b>.
   * 
   * <p>
   * For example, if the properties parameter contains a property "key1" set as
   * "value1", then the call
   * 
   * <pre>
   * String s = OptionConverter.substituteVars(&quot;Value of key is ${key1}.&quot;);
   * </pre>
   * 
   * will set the variable <code>s</code> to "Value of key is value1.".
   * 
   * <p>
   * If no value could be found for the specified key, then the system
   * properties are searched, if the value could not be found there, then
   * substitution defaults to the empty string.
   * 
   * <p>
   * For example, if system properties contains no value for the key
   * "inexistentKey", then the call
   * 
   * <pre>
   * String s = OptionConverter
   *     .subsVars(&quot;Value of inexistentKey is [${inexistentKey}]&quot;);
   * </pre>
   * 
   * will set <code>s</code> to "Value of inexistentKey is []".
   * 
   * <p>
   * Nevertheless, it is possible to specify a default substitution value using
   * the ":-" operator. For example, the call
   * 
   * <pre>
   * String s = OptionConverter.subsVars(&quot;Value of key is [${key2:-val2}]&quot;);
   * </pre>
   * 
   * will set <code>s</code> to "Value of key is [val2]" even if the "key2"
   * property is unset.
   * 
   * <p>
   * An {@link java.lang.IllegalArgumentException} is thrown if <code>val</code>
   * contains a start delimeter "${" which is not balanced by a stop delimeter
   * "}".
   * </p>
   * 
   * @param val
   *          The string on which variable substitution is performed.
   * @throws IllegalArgumentException
   *           if <code>val</code> is malformed.
   */
  public static String substVars(String val, Properties props) {

    StringBuffer sbuf = new StringBuffer();

    int i = 0;
    int j;
    int k;

    while (true) {
      j = val.indexOf(DELIM_START, i);

      if (j == -1) {
        // no more variables
        if (i == 0) { // this is a simple string

          return val;
        } else { // add the tail string which contails no variables and return
                  // the result.
          sbuf.append(val.substring(i, val.length()));

          return sbuf.toString();
        }
      } else {
        sbuf.append(val.substring(i, j));
        k = val.indexOf(DELIM_STOP, j);

        if (k == -1) {
          throw new IllegalArgumentException('"' + val
              + "\" has no closing brace. Opening brace at position " + j + '.');
        } else {
          j += DELIM_START_LEN;

          String rawKey = val.substring(j, k);

          // Massage the key to extract a default replacement if there is one
          String[] extracted = extractDefaultReplacement(rawKey);
          String key = extracted[0];
          String defaultReplacement = extracted[1]; // can be null

          String replacement = null;

          // first try the props passed as parameter
          if (props != null) {
            replacement = props.getProperty(key);
          }

          // then try in System properties
          if (replacement == null) {
            replacement = getSystemProperty(key, null);
          }

          // if replacement is still null, use the defaultReplacement which
          // still be null
          if (replacement == null) {
            replacement = defaultReplacement;
          }

          if (replacement != null) {
            // Do variable substitution on the replacement string
            // such that we can solve "Hello ${x2}" as "Hello p1"
            // where the properties are
            // x1=p1
            // x2=${x1}
            String recursiveReplacement = substVars(replacement, props);
            sbuf.append(recursiveReplacement);
          }

          i = k + DELIM_STOP_LEN;
        }
      }
    }
  }

  /**
   * Very similar to <code>System.getProperty</code> except that the
   * {@link SecurityException} is hidden.
   * 
   * @param key
   *          The key to search for.
   * @param def
   *          The default value to return.
   * @return the string value of the system property, or the default value if
   *         there is no property with that key.
   */
  public static String getSystemProperty(String key, String def) {
    try {
      return System.getProperty(key, def);
    } catch (Throwable e) { // MS-Java throws
                            // com.ms.security.SecurityExceptionEx
      return def;
    }
  }

  static public String[] extractDefaultReplacement(String key) {
    String[] result = new String[2];
    result[0] = key;
    int d = key.indexOf(":-");
    if (d != -1) {
      result[0] = key.substring(0, d);
      result[1] = key.substring(d + 2);
    }
    return result;
  }
  
  /**
   * If <code>value</code> is "true", then <code>true</code> is returned. If
   * <code>value</code> is "false", then <code>true</code> is returned.
   * Otherwise, <code>default</code> is returned.
   * 
   * <p>
   * Case of value is unimportant.
   */
  public static boolean toBoolean(String value, boolean dEfault) {
    if (value == null) {
      return dEfault;
    }

    String trimmedVal = value.trim();

    if ("true".equalsIgnoreCase(trimmedVal)) {
      return true;
    }

    if ("false".equalsIgnoreCase(trimmedVal)) {
      return false;
    }

    return dEfault;
  }

  public static boolean isEmpty(String val) {
    return ((val == null) || CoreGlobal.EMPTY_STRING.equals(val));
  }

}