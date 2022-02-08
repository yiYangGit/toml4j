package com.moandjiezana.toml;

import java.util.concurrent.atomic.AtomicInteger;

interface ValueReader {

  /**
   * @param s must already have been trimmed
   */
  boolean canRead(String s,int readStartIndex);
  
  /**
   * Partial validation. Stops after type terminator, rather than at EOI.
   * 返回的索引为完整的读完的value的值后的第一个索引
   * 
   * @param s  must already have been validated by {@link #canRead(String,int)}
   * @param index where to start in s
   * @param context current line number, used for error reporting
   * @return a value or a {@link Results.Errors}
   */
  Object read(String s, AtomicInteger index, Context context);
}
