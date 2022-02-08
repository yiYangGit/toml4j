package com.moandjiezana.toml;

import java.util.TimeZone;

class DatePolicy {

  private final TimeZone timeZone;
  private final boolean showFractionalSeconds = true;
  
  DatePolicy(TimeZone timeZone) {
    this.timeZone = timeZone;
  }

  TimeZone getTimeZone() {
    return timeZone;
  }

  boolean isShowFractionalSeconds() {
    return showFractionalSeconds;
  }
}
