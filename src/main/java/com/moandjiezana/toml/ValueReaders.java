package com.moandjiezana.toml;

import static com.moandjiezana.toml.ArrayValueReader.ARRAY_VALUE_READER;
import static com.moandjiezana.toml.BooleanValueReaderWriter.BOOLEAN_VALUE_READER_WRITER;
import static com.moandjiezana.toml.DateValueReaderWriter.DATE_VALUE_READER_WRITER;
import static com.moandjiezana.toml.InlineTableValueReader.INLINE_TABLE_VALUE_READER;
import static com.moandjiezana.toml.LiteralStringValueReader.LITERAL_STRING_VALUE_READER;
import static com.moandjiezana.toml.MultilineLiteralStringValueReader.MULTILINE_LITERAL_STRING_VALUE_READER;
import static com.moandjiezana.toml.MultilineStringValueReader.MULTILINE_STRING_VALUE_READER;
import static com.moandjiezana.toml.NullValueReaderWriter.NULL_VALUE_READER_WRITER;
import static com.moandjiezana.toml.NumberValueReaderWriter.NUMBER_VALUE_READER_WRITER;
import static com.moandjiezana.toml.StringValueReaderWriter.STRING_VALUE_READER_WRITER;

import java.util.concurrent.atomic.AtomicInteger;

class ValueReaders {
  
  static final ValueReaders VALUE_READERS = new ValueReaders();
  
  Object convert(String value, AtomicInteger index, Context context) {
    for (ValueReader valueParser : READERS) {
      if (valueParser.canRead(value,index.get())) {
        return valueParser.read(value, index, context);
      }
    }
    String substring = value.substring(index.get());
    Results.Errors errors = new Results.Errors();
    errors.invalidValue(context.identifier.getName(), substring, context.line.get());
    return errors;
  }
  
  private ValueReaders() {}
  
  private static final ValueReader[] READERS = { 
    MULTILINE_STRING_VALUE_READER, MULTILINE_LITERAL_STRING_VALUE_READER, LITERAL_STRING_VALUE_READER, STRING_VALUE_READER_WRITER, DATE_VALUE_READER_WRITER, NUMBER_VALUE_READER_WRITER, BOOLEAN_VALUE_READER_WRITER, ARRAY_VALUE_READER, INLINE_TABLE_VALUE_READER,
          NULL_VALUE_READER_WRITER
  };
}
