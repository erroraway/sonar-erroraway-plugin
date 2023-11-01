package com.bugs;

import java.time.ZoneId;

public class BugsSamples {

	public String bug() {
		ZoneId zoneId = ZoneId.of("Z");
		
		return zoneId.getId();
	}
}
