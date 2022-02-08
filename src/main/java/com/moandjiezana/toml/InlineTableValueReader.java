package com.moandjiezana.toml;

import static com.moandjiezana.toml.ValueReaders.VALUE_READERS;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

class InlineTableValueReader implements ValueReader {

  static final InlineTableValueReader INLINE_TABLE_VALUE_READER = new InlineTableValueReader();

    static final Pattern INI_LINE_KEY_PATTEN = Pattern.compile("^[.a-z0-9A-Z_-]+$");
  @Override
  public boolean canRead(String s,int readStartIndex) {
    return s.startsWith("{",readStartIndex);
  }

  @Override
  public Object read(String s, AtomicInteger sharedIndex, Context context) {
    AtomicInteger line = context.line;
    int startLine = line.get();
    int startIndex = sharedIndex.get();
    boolean inKey = true;
      boolean inSearchEnd = true;
    boolean inValue = false;
    boolean terminated = false;
      String currentKey = null;
    LinkedHashMap<String, Object> results = new LinkedHashMap<String, Object>();
    Results.Errors errors = new Results.Errors();
    
    for (int i = sharedIndex.incrementAndGet(); sharedIndex.get() < s.length(); i = sharedIndex.incrementAndGet()) {
      char c = s.charAt(i);
        if (c == '\n') {
            line.incrementAndGet();
        }
        if (inSearchEnd && c == '}') {
            terminated = true;
            break;
        }else if (!Character.isWhitespace(c)&& inKey){
          ReadKeyResult readKeyResult = GeneralStringKeyRead.readStrKeyAndMvIndexToEqualSplit(s, sharedIndex);
          String readStrKey = readKeyResult.key;
          if (!readKeyResult.isValid) {
              errors.invalidKey(readStrKey, line.get());
              return errors;
          }
          if (readStrKey == null) {
              errors.unterminated(context.identifier.getName(), s.substring(startIndex), line.get());
              return errors;
          }
          if (c == '\"'|| c== '\'') {
              currentKey = readStrKey;
          } else{
              if (INI_LINE_KEY_PATTEN.matcher(readStrKey).matches() && !readStrKey.startsWith(".") && !readStrKey.endsWith(".")) {
                  currentKey = readStrKey;
              } else {
                  errors.invalidKey(currentKey, line.get());
                  return errors;
              }
          }
          if (s.charAt(sharedIndex.get()) != '=') {
              throw new IllegalStateException();
          }
          inKey = false;
          inValue = true;
          inSearchEnd = false;
      }else if (inValue && !Character.isWhitespace(c)) {
          Object converted = VALUE_READERS.convert(s, sharedIndex, context.with(new Identifier(currentKey, Identifier.Type.KEY)));
        if (converted instanceof Results.Errors) {
          errors.add((Results.Errors) converted);
          return errors;
        }

          String currentKeyTrimmed = currentKey;
        Object previous = results.put(currentKeyTrimmed, converted);

        if (previous != null) {
          errors.duplicateKey(currentKeyTrimmed, context.line.get());
          return errors;
        }
        currentKey = null;
        inValue = false;
        inSearchEnd = true;
      } else if (c == ',') {
        inKey = true;
        inValue = false;
          currentKey = null;
      }
    }
    
    if (!terminated) {
      errors.unterminated(context.identifier.getName(), s.substring(startIndex), startLine);
    }
    
    if (errors.hasErrors()) {
      return errors;
    }
    
    return results;
  }

  private InlineTableValueReader() {}
}
