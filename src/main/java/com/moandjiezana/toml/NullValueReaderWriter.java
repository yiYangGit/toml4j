package com.moandjiezana.toml;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * support read write null value
 */
class NullValueReaderWriter implements ValueReader, ValueWriter {

    static final NullValueReaderWriter NULL_VALUE_READER_WRITER = new NullValueReaderWriter();

    private static final String NULL = "null";
    @Override
    public boolean canRead(String s,int readStartIndex) {
        boolean canRead = s.startsWith(NULL);
        if (canRead) {
            int nullEndIndex = readStartIndex + NULL.length();
            if (s.length() > nullEndIndex) {
                if (!Character.isWhitespace(nullEndIndex)) {
                    canRead = false;
                }
            }
        }
        return canRead;
    }

    @Override
    public Object read(String s, AtomicInteger index, Context context) {
        int readStartIndex = index.get();
        if (!canRead(s, readStartIndex)) {
            throw new IllegalArgumentException();
        }
        index.set(readStartIndex + NULL.length() - 1);
        return null;
    }

    @Override
    public boolean canWrite(Object value) {
        return value == null;
    }

    @Override
    public void write(Object value, WriterContext context) {
        if (value != null) {
            throw new IllegalArgumentException();
        }
        context.write(NULL);
    }

    @Override
    public boolean isPrimitiveType() {
        return true;
    }

    @Override
    public String toString() {
        return "null";
    }
}
