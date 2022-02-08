package com.moandjiezana.toml;

import static com.moandjiezana.toml.IdentifierConverter.IDENTIFIER_CONVERTER;

import java.util.concurrent.atomic.AtomicInteger;

class TomlParser {

  static Results run(String tomlString) {
    final Results results = new Results();
    
    if (tomlString.isEmpty()) {
      return results;
    }
    
    AtomicInteger index = new AtomicInteger();
    boolean inComment = false;
    AtomicInteger line = new AtomicInteger(1);
    Identifier identifier = null;
    Object value = null;
    AtomicInteger beforeCommentIndex = new AtomicInteger(0);
    for (int i = index.get(); i < tomlString.length(); i = index.incrementAndGet()) {
      char c = tomlString.charAt(i);
      
      if (results.errors.hasErrors()) {
        break;
      }
      if (c == '#' && !inComment) {
        inComment = true;
      } else if (!Character.isWhitespace(c) && !inComment && identifier == null) {
          int commentLastIndex = index.get();
          int firstIndex = beforeCommentIndex.get();
          String beforeComment = null;
          if (commentLastIndex > firstIndex) {
              beforeComment = tomlString.substring(firstIndex, commentLastIndex);
          }
        Identifier id = IDENTIFIER_CONVERTER.convert(tomlString, index, new Context(null, line, results.errors));
        id.setBeforeComment(beforeComment);
        if (id != Identifier.INVALID) {
          if (id.isKey()) {
            identifier = id;
          } else if (id.isTable()) {
            results.startTables(id, line);
              beforeCommentIndex.set(index.get()+2);
          } else if (id.isTableArray()) {
            results.startTableArray(id, line);
            beforeCommentIndex.set(index.get()+2);
          }
        }
      } else if (c == '\n') {
        inComment = false;
        identifier = null;
        value = null;
        line.incrementAndGet();
      } else if (!inComment && identifier != null && identifier.isKey() && value == null && !Character.isWhitespace(c)) {
        value = ValueReaders.VALUE_READERS.convert(tomlString, index, new Context(identifier, line, results.errors));
          int rightCommentStartIndex =  index.get() + 1;
          int rightCommentEndIndex =  rightCommentStartIndex;
          if (rightCommentStartIndex < tomlString.length()) {
              while ( rightCommentEndIndex<tomlString.length()&& tomlString.charAt(rightCommentEndIndex) != '\n') {
                  rightCommentEndIndex++;
              }
              identifier.setRightComment(tomlString.substring(rightCommentStartIndex,rightCommentEndIndex));
              rightCommentEndIndex++;
          }
          if (rightCommentEndIndex < tomlString.length()) {
              beforeCommentIndex.set(rightCommentEndIndex);
          }else{
              beforeCommentIndex.set(-1);
          }

        if (value instanceof Results.Errors) {
          results.errors.add((Results.Errors) value);
        } else {
          results.addValue(identifier,identifier.getName(), value, line);
        }
      } else if (value != null && !inComment && !Character.isWhitespace(c)) {
        results.errors.invalidTextAfterIdentifier(identifier, c, line.get());
      }
    }
    int lastCommentIndex = beforeCommentIndex.get();
      if (lastCommentIndex > 0 && lastCommentIndex<tomlString.length()) {
          results.setLastComment(tomlString.substring(lastCommentIndex));
      }
    return results;
  }
  
  private TomlParser() {}
}
