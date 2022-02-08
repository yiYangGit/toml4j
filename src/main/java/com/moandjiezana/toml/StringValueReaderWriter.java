package com.moandjiezana.toml;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * 读 以 " 开头的 字符串
 */
class StringValueReaderWriter implements ValueReader, ValueWriter {
  
  static final StringValueReaderWriter STRING_VALUE_READER_WRITER = new StringValueReaderWriter();
  static private final String[] specialCharacterEscapes = new String[93];
  static private final boolean[] PLAIN_KEY_ALLOWED_CHARS_ARRAY;


  public static boolean isQuoteKey(char c) {
      if (c < PLAIN_KEY_ALLOWED_CHARS_ARRAY.length ) {
          return PLAIN_KEY_ALLOWED_CHARS_ARRAY[c];
      }
      return false;
  }
  static final Pattern QUOTE_KEY_PATTEN = Pattern.compile("^[a-z0-9A-Z_-]+$");
  static {
    specialCharacterEscapes['\b'] = "\\b";
    specialCharacterEscapes['\t'] = "\\t";
    specialCharacterEscapes['\n'] = "\\n";
    specialCharacterEscapes['\f'] = "\\f";
    specialCharacterEscapes['\r'] = "\\r";
    specialCharacterEscapes['"'] = "\\\"";
    specialCharacterEscapes['\\'] = "\\\\";
      String PLAIN_KEY_ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890_-";
      int arrLength = 0;
      for (int i = 0; i < PLAIN_KEY_ALLOWED_CHARS.length(); i++) {
          arrLength = Math.max(arrLength, PLAIN_KEY_ALLOWED_CHARS.charAt(i));
      }
      PLAIN_KEY_ALLOWED_CHARS_ARRAY = new boolean[arrLength+1];
      for (int i = 0; i < PLAIN_KEY_ALLOWED_CHARS.length(); i++) {
          PLAIN_KEY_ALLOWED_CHARS_ARRAY[PLAIN_KEY_ALLOWED_CHARS.charAt(i)] = true;
      }
  }

    public static boolean isNeedEscapesCharacter(char c) {
        if (c < specialCharacterEscapes.length) {
            return specialCharacterEscapes[c] != null;
        }
        return false;
    }

    public static String quoteKey(String stringKey) {
        boolean shouldEscape = !QUOTE_KEY_PATTEN.matcher(stringKey).matches();

        if (shouldEscape) {
            StringBuilder builder = new StringBuilder();
            builder.append('"');
            for (int i = 0; i < stringKey.length(); i++) {
                int codePoint = stringKey.codePointAt(i);
                if (codePoint < specialCharacterEscapes.length && specialCharacterEscapes[codePoint] != null) {
                    builder.append(specialCharacterEscapes[codePoint]);
                } else {
                    builder.append(stringKey.charAt(i));
                }
            }
            builder.append('"');
            stringKey = builder.toString();
        }

        return stringKey;
    }

  @Override
  public boolean canRead(String s,int readStartIndex) {
    return s.startsWith("\"",readStartIndex);
  }


    /**
     * 判断 s.charAt(charIndex) 是不是转义字符 \ 注意两个\\ 转义字符标识为\
     * 所以 charIndex 往前搜索连续的 \ 的数量为基数则为转义字符,偶数则不是转义字符
     * @param s
     * @param startIndex 从charIndex往前搜索的终止索引
     * @param charIndex
     * @return
     */
    private static boolean isEscChar(String s, int startIndex,int charIndex) {
        if (charIndex < startIndex) {
            throw new IllegalArgumentException();
        }
        if (charIndex < 0 || charIndex >= s.length()) {
            throw new IndexOutOfBoundsException();
        }
        char escChar = '\\';
        if (s.charAt(charIndex)!=escChar) {
            return false;
        }
        int escCharCount = 1;
        int searchEscCharIndex = charIndex-1;
        while (searchEscCharIndex >= startIndex && s.charAt(searchEscCharIndex) == escChar) {
            escCharCount++;
            searchEscCharIndex--;
        }
        if (escCharCount % 2 == 0) {
            return false;
        } else {
            return true;
        }

    }

    /**
     *  在  { 123123"1234567\"89" } 中的字符串中从 第一个 " 开始 往后找到最后一个"的索引 中间遇到换行符 \n 直接结束
     * @param s
     * @param startIndex 开始搜索的第一个索引 在 开始的 " 之后的一个索引
     * @return >0则为 找到结束符 " 否则没有找到结束符
     */
    private static int findEndIndex(String s, int startIndex) {
        int i;
        for (i = startIndex; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\n') {
                return -i;
            }
            if (ch == '"') {
                int charBeforeIndex = i - 1;
                if ((charBeforeIndex < startIndex) || !isEscChar(s, startIndex, charBeforeIndex)) {
                    //搜索到结束索引
                    return i;
                }
            }
        }
        return -i;
    }
  @Override
  public Object read(String s, AtomicInteger index, Context context) {
      int start = index.get();
      String result = readStr(s, index);
    if (result ==null) {
        Results.Errors errors = new Results.Errors();
        errors.unterminated(context.identifier.getName(), s.substring(start, index.get()), context.line.get());
        return errors;
    }

    return result;
  }

    /**
     * 自动读取 以 " 开头和结尾的  的字符串读取 自动处理转义
     * @return  不合法的字符串则为
     */
    public static String readStr(String s, AtomicInteger index) {
        int startIndex = index.get();
        if (s.charAt(startIndex) != '\"') {
            throw new IllegalArgumentException();
        }
        index.incrementAndGet();
        int endIndex = findEndIndex(s, index.get());
        index.set(Math.abs(endIndex));
        if (endIndex < 0) {

            return null;
        }
        String raw = s.substring(startIndex+1, endIndex);
        s = replaceSpecialCharactersAndUnicode(raw);
        return s;
    }
    private  String replaceUnicodeCharacters(String value) {
      StringBuilder builder = new StringBuilder();
      char ecsChar = '\\';
      char u = 'u';
      for (int i = 0; i < value.length(); i++) {
          char c = value.charAt(i);
          if (c == ecsChar && i+1<value.length() && value.charAt(i+1)== u && isEscChar(value,0,i)) {
            //  if (i == 0 || (value.charAt(i - 1) != ecsChar)) {
                  String errmsg = "错误的unicode转义字符: ";
                  if (i < value.length() - 5) {
                      String ecsUnicode = value.substring(i + 2, i + 6);
                      String regex = "[0-9a-fA-F]{4}";
                      if (ecsUnicode.matches(regex)) {
                          builder.append(new String(Character.toChars(Integer.parseInt(ecsUnicode, 16))));
                          i = i + 5;
                          continue;
                      } else {
                          throw new IllegalStateException(errmsg + value.substring(i, i + 6));
                      }
                  } else {

                      throw new IllegalStateException(errmsg + value.substring(i));
                  }
             // }
          }
          builder.append(c);
      }
      return builder.toString();
  }

  static String replaceSpecialCharactersAndUnicode(String s) {
      StringBuilder builder = new StringBuilder();

    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
        final char escChar = '\\';
        int nextCharIndex = i + 1;
        if (ch == escChar && nextCharIndex >= s.length()) {
            throw new IllegalStateException("invalid string: can not end with \\" + s);
        }
        if (ch == escChar && nextCharIndex<s.length()) {
            char next = s.charAt(i + 1);
            i++;
            switch (next) {
                case escChar:
                    builder.append("\\");
                    break;
                case 'b':
                    builder.append("\b");
                    break;
                case 'f':
                    builder.append("\f");
                    break;
                case 'n':
                    builder.append("\n");
                    break;
                case 't':
                    builder.append("\t");
                    break;
                case 'r':
                    builder.append("\r");
                    break;
                case 'u':
                    builder.append(readEcsUnicode(s, i - 1));
                    i=i+4;
                    break;
                case '\"':
                    builder.append("\"");
                    break;
                default:
                    throw new IllegalStateException("invalid string: " + s);
            }
        }else{
            builder.append(ch);
        }
    }
      s = builder.toString();

      return s;
  }

    protected static String readEcsUnicode(String value,int i) {
        char ecsChar = '\\';
        char u = 'u';
        if (value.charAt(i) != ecsChar || value.charAt(i+1) != u) {
            throw new IllegalArgumentException();
        }
        String errmsg = "错误的unicode转义字符: ";
        if (i < value.length() - 5) {
            String ecsUnicode = value.substring(i + 2, i + 6);
            String regex = "[0-9a-fA-F]{4}";
            if (ecsUnicode.matches(regex)) {
                return new String(Character.toChars(Integer.parseInt(ecsUnicode, 16)));
            } else {
                throw new IllegalStateException(errmsg + value.substring(i, i + 6));
            }
        } else {
            throw new IllegalStateException(errmsg + value.substring(i));
        }
    }

  @Override
  public boolean canWrite(Object value) {
    return value instanceof String || value instanceof Character || value instanceof URL || value instanceof URI || value instanceof Enum;
  }

  @Override
  public void write(Object value, WriterContext context) {
    context.write('"');
    escapeUnicode(value.toString(), context);
    context.write('"');
  }

  @Override
  public boolean isPrimitiveType() {
    return true;
  }

  private void escapeUnicode(String in, WriterContext context) {
    for (int i = 0; i < in.length(); i++) {
      int codePoint = in.codePointAt(i);
      if (codePoint < specialCharacterEscapes.length && specialCharacterEscapes[codePoint] != null) {
        context.write(specialCharacterEscapes[codePoint]);
      } else {
        context.write(in.charAt(i));
      }
    }
  }

  private StringValueReaderWriter() {}

  @Override
  public String toString() {
    return "string";
  }
}
