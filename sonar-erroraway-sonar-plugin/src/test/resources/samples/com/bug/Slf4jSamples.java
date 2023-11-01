package com.bug;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jSamples {
	public final Logger logger = LoggerFactory.getLogger(getClass());
	
	private void slf4jPlaceHolderMismatch() {
		logger.info("Hello, {}.", "Hello", "World");
	}
}