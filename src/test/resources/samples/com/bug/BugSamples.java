package com.bug;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class BugSamples {

	public Duration duration() {
		return Duration.of(1, ChronoUnit.YEARS);
	}
	
	public void npe() {
		nonNullableArg(null);
	}
	
	public void nonNullableArg(String x) {
		System.out.println(x.toLowerCase());
	}
}
