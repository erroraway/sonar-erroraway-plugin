package com.bug;

import com.google.common.collect.ImmutableSet;
import java.math.BigDecimal;

public class PicnicErrorProneSupportSample {
	static BigDecimal getNumber() {
		return BigDecimal.valueOf(0);
	}
	
	public ImmutableSet<Integer> getSet() {
		ImmutableSet<Integer> set = ImmutableSet.of(1);
		return ImmutableSet.copyOf(set);
	}
}