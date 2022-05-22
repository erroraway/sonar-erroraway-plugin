import java.util.Properties;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Simple {
	public final Logger logger = LoggerFactory.getLogger(getClass());
	
	public void method() throws Exception {
		System.out.println(getClass().newInstance());
		
		Long t = null;
		if (System.currentTimeMillis() > 12) {
			t = 12L;
		}

		System.out.println(t.doubleValue());

		if (System.currentTimeMillis() == Double.NaN) {
			System.out.println("NaN");
		}
	}
	
	private void slf4jPlaceHolderMismatch() {
		logger.info("Hello, {}.", "Hello", "World");
	}
}
