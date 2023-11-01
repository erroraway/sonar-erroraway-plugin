package application;

import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Simple {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
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

	public static void main(String[] args) {
		Set<Short> s = new HashSet<Short>();
		for (short i = 0; i < 100; i++) {
			s.add(i);
			s.remove(i - 1);
		}
		System.out.println(s.size());
	}
	
	private int npe() {
		Object o = null;
		
		if (o != null) {
			o = "";
		}
		
		return o.hashCode();
	}
	
	private void slf4jPlaceHolderMismatch() {
		logger.info("Hello, {}.", "Hello", "World");
	}
}
