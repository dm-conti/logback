/**
 * LOGBack: the reliable, fast and flexible logging library for Java.
 *
 * Copyright (C) 1999-2006, QOS.ch
 *
 * This library is free software, you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation.
 */
package ch.qos.logback.classic.pattern;

import java.util.Map;
import java.util.List;
import java.util.EnumMap;

/**
 * Construct and parse options passed to/from Converters.
 */
public class ConverterOptions<T extends Enum<T>> {
  private StringBuilder builder;

  public ConverterOptions() {
    builder = new StringBuilder();
  }

  public ConverterOptions(String first) {
    builder = new StringBuilder(first);
  }

  public void add(T option, boolean value) {
    if (builder.length() > 0) {
      builder.append(", ");
    }
    builder.append(option).append("=").append(Boolean.toString(value));
  }

  public void add(T option, String value) {
    if (value == null) {
      return;
    }
    if (builder.length() > 0) {
      builder.append(", ");
    }
    builder.append(option).append("=").append(value);
  }

  public String toString() {
    return builder.toString();
  }

  public static <T extends Enum<T>> String getFirstOption(Class<T> clazz, List<String> optionList) {

    if (optionList == null || optionList.size() == 0) {
      return null;
    }
    String str = optionList.get(0);
    int index = str.indexOf("=");
    if (index < 0) {
      return str;
    }
    T option = Enum.valueOf(clazz, str.substring(0, index));
    if (option == null) {
      return str;
    }
    return null;
  }

  public static <T extends Enum<T>> Map<T, String> getOptions(Class<T> clazz, List<String> optionList) {

    EnumMap<T, String> map = new EnumMap<T, String>(clazz);

    if (optionList == null) {
      return map;
    }

    for (int i = 0; i < optionList.size(); ++i) {
      if (i == 0 && !optionList.get(0).contains("=")) {
        continue;
      }
      String str = optionList.get(i);
      int index = str.indexOf("=");
      T option = Enum.valueOf(clazz, str.substring(0, index));
      map.put(option, str.substring(index + 1));
    }

    return map;
  }
}
