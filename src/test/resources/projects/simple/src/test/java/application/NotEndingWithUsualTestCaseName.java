package application;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * It would be picked up by the unit tests of the actual project if its name ended with Test
 */
public class NotEndingWithUsualTestCaseName {

	@BeforeAll
	public static void startOrchestrator() {
	}
	
	@AfterAll
	public static void stopOrchestrator() {
	}

	@Test
	void test() {
	}
}
