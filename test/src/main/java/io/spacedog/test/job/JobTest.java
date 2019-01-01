package io.spacedog.test.job;

public class JobTest {

	public Object run() {
		return System.getenv("spacedog_url");
	}

}
