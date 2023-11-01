package com.bug;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class AutoValueSamples {
  abstract String name();

  static Builder builder() {
	Duration.of(1, ChronoUnit.YEARS);
    return new AutoValue_AutoValueSamples.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder setName(String value);
    abstract AutoValueSamples build();
  }
}