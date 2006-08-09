package ch.qos.logback.core.pattern.parser;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.pattern.util.EscapeUtil;

public class OptionTokenizer {

  private final static int EXPECTING_STATE = 0;
  private final static int COLLECTING_STATE = 1;
  private final static int QUOTED_COLLECTING_STATE = 2;

  private static final char ESCAPE_CHAR = '\\';
  private static final char COMMA_CHAR = ',';
  private static final char DOUBLE_QUOTE_CHAR = '"';
  private static final char SINGLE_QUOTE_CHAR = '\'';

  String pattern;
  int patternLength;
  char quoteChar;
  int pointer = 0;
  int state = EXPECTING_STATE;

  OptionTokenizer(String pattern) {
    this.pattern = pattern;
    patternLength = pattern.length();
  }

  List tokenize() throws ScanException {
    List<String> tokenList = new ArrayList<String>();
    StringBuffer buf = new StringBuffer();

    while (pointer < patternLength) {
      char c = pattern.charAt(pointer);
      pointer++;

      switch (state) {
      case EXPECTING_STATE:
        switch (c) {
        case ' ':
        case '\t':
        case '\r':
        case '\n':
          break;
        case SINGLE_QUOTE_CHAR:
        case DOUBLE_QUOTE_CHAR:
          state = QUOTED_COLLECTING_STATE;
          quoteChar = c;
          break;
        default:
          buf.append(c);
          state = COLLECTING_STATE;
        }
        break;
      case COLLECTING_STATE:
        switch (c) {
        case COMMA_CHAR:
          tokenList.add(buf.toString().trim());
          buf.setLength(0);
          state = EXPECTING_STATE;
          break;
        default:
          buf.append(c);
        }
        break;
      case QUOTED_COLLECTING_STATE:
        if (c == quoteChar) {
          tokenList.add(buf.toString());
          buf.setLength(0);
          state = EXPECTING_STATE;
        } else if (c == ESCAPE_CHAR) {
          escape(String.valueOf(quoteChar), buf);
        } else {
          buf.append(c);
        }

        break;
      }
    }

    // EOS
    switch (state) {
    case EXPECTING_STATE:
      break;
    case COLLECTING_STATE:
      tokenList.add(buf.toString().trim());
      break;
    default:
      throw new ScanException("Unexpected end of pattern string");
    }

    return tokenList;
  }

  void escape(String escapeChars, StringBuffer buf) {
    if ((pointer < patternLength)) {
      char next = pattern.charAt(pointer++);
      EscapeUtil.escape(escapeChars, buf, next, pointer);
    }
  }
}