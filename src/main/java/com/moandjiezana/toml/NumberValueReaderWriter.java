package com.moandjiezana.toml;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class NumberValueReaderWriter implements ValueReader, ValueWriter {
  static final NumberValueReaderWriter NUMBER_VALUE_READER_WRITER = new NumberValueReaderWriter();
  private static final Map<String,Double> SPECIAL_STRING_DOUBLE;
  private static final Map<Double,String> SPECIAL_DOUBLE_STRING;
  static {
      Map<String, Double> stringDoubleMap = new HashMap<>();
      stringDoubleMap.put("NaN", Double.NaN);
      stringDoubleMap.put("+Infinity", Double.POSITIVE_INFINITY);
      stringDoubleMap.put("-Infinity", Double.NEGATIVE_INFINITY);
      SPECIAL_STRING_DOUBLE = Collections.unmodifiableMap(stringDoubleMap);
      Map<Double, String> doubleString = new HashMap<>();
      for (Map.Entry<String, Double> entry : stringDoubleMap.entrySet()) {
          doubleString.put(entry.getValue(), entry.getKey());
      }
      SPECIAL_DOUBLE_STRING = Collections.unmodifiableMap(doubleString);
  }
  @Override
  public boolean canRead(String s,int readStartIndex) {
    char firstChar = s.charAt(readStartIndex);
    boolean canRead = isMatchSpecialDouble(s, new AtomicInteger(readStartIndex),false)!=null;
    if (!canRead) {
        canRead =  firstChar == '+' || firstChar == '-' || Character.isDigit(firstChar);
    }
    return canRead;
  }

    private Double isMatchSpecialDouble(String s, AtomicInteger index,boolean isMoveIndex) {
        int readStartIndex = index.get();
        Double value =null;
        String mathKey =null;
        for (Map.Entry<String, Double> entry : SPECIAL_STRING_DOUBLE.entrySet()) {
            String matchStr = entry.getKey();
            int matchEndIndex = readStartIndex + matchStr.length();
            if (s.startsWith(matchStr,readStartIndex)) {
                if (matchEndIndex < s.length()) {
                    if (Character.isWhitespace(s.charAt(matchEndIndex))) {
                        mathKey = matchStr;
                        value= entry.getValue();
                        break;
                    }
                } else {
                    mathKey = matchStr;
                    value= entry.getValue();
                    break;
                }
            }
        }
        if (isMoveIndex && mathKey != null) {
            index.set(readStartIndex + mathKey.length()-1);
        }
        return value;
    }

    @Override
  public Object read(String s, AtomicInteger index, Context context) {
    Double specialDouble = isMatchSpecialDouble(s, index,true);
    if (specialDouble != null) {
            return specialDouble;
    }
    boolean signable = true;
    boolean dottable = false;
    boolean exponentable = false;
    boolean terminatable = false;
    boolean underscorable = false;
    String type = "";
    StringBuilder sb = new StringBuilder();

    for (int i = index.get(); i < s.length(); i = index.incrementAndGet()) {
      char c = s.charAt(i);
      boolean notLastChar = s.length() > i + 1;

      if (Character.isDigit(c)) {
        sb.append(c);
        signable = false;
        terminatable = true;
        if (type.isEmpty()) {
          type = "integer";
          dottable = true;
        }
        underscorable = notLastChar;
        exponentable = !type.equals("exponent");
      } else if ((c == '+' || c == '-') && signable && notLastChar) {
        signable = false;
        terminatable = false;
        if (c == '-') {
          sb.append('-');
        }
      } else if (c == '.' && dottable && notLastChar) {
        sb.append('.');
        type = "float";
        terminatable = false;
        dottable = false;
        exponentable = false;
        underscorable = false;
      } else if ((c == 'E' || c == 'e') && exponentable && notLastChar) {
        sb.append('E');
        type = "exponent";
        terminatable = false;
        signable = true;
        dottable = false;
        exponentable = false;
        underscorable = false;
      } else if (c == '_' && underscorable && notLastChar && Character.isDigit(s.charAt(i + 1))) {
        underscorable = false;
      } else {
        if (!terminatable) {
          type = "";
        }
        index.decrementAndGet();
        break;
      }
    }

      if (type.equals("integer")) {
          return new BigDecimal(sb.toString()).longValue();
      } else if( type.equals("float") || type.equals("exponent")){
          return new BigDecimal(sb.toString()).doubleValue();
      } else {
          Results.Errors errors = new Results.Errors();
          errors.invalidValue(context.identifier.getName(), sb.toString(), context.line.get());
          return errors;
      }
  }

  @Override
  public boolean canWrite(Object value) {
    return Number.class.isInstance(value) && ! (value instanceof Float) ;
  }

  @Override
  public void write(Object value, WriterContext context) {
      if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte || value instanceof BigDecimal || value instanceof BigInteger) {
          context.write(value.toString());
      } else if (value instanceof Double) {
          String s = SPECIAL_DOUBLE_STRING.get(value);
          if (s!=null) {
              context.write(s);
          } else {
              //String s1 = new BigDecimal(value.toString()).toString();
              //context.write(s1);
              context.write(value.toString());
          }
      } else {
          throw new IllegalArgumentException();
      }
  }

  @Override
  public boolean isPrimitiveType() {
    return true;
  }

  @Override
  public String toString() {
    return "number";
  }
}
