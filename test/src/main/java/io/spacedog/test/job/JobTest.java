package io.spacedog.test.job;

public class JobTest {

	public String run() {
		return System.getenv("spacedog_url");
	}

}
