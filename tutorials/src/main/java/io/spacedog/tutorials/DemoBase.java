package io.spacedog.tutorials;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.http.SpaceEnv;

public class DemoBase {

	private SpaceDog david;
	private SpaceDog max;
	private SpaceDog superadmin;
	private DataWrap<Course> course;
	private DataWrap<Customer> customer;

	protected SpaceDog max() {
		if (max == null)
			max = SpaceDog.dog()//
					.username("max")//
					.password("hi max")//
					.login();
		return max;
	}

	protected SpaceDog superadmin() {
		if (superadmin == null)
			superadmin = SpaceDog.dog()//
					.username("superadmin")//
					.password("hi superadmin")//
					.login();
		return superadmin;
	}

	protected SpaceDog guest() {
		return SpaceDog.dog();
	}

	protected SpaceDog david() {
		if (david == null)
			david = SpaceDog.dog()//
					.username("david")//
					.password("hi dave")//
					.login();
		return david;
	}

	protected DataWrap<Customer> davidCustomer() {
		if (customer == null)
			customer = david().data()//
					.get("customer", david().id(), Customer.Wrap.class);
		return customer;
	}

	protected SpaceDog superdog() {
		return SpaceDog.dog().username("superdog")//
				.password(SpaceEnv.env().superdogPassword());
	}

	protected DataWrap<Course> course() {
		if (course == null)
			course = superadmin().data().fetch(//
					new Course.Wrap().id("myCourse"));
		return course;
	}

	protected SpaceDog cashier() {
		return SpaceDog.dog()//
				.username("cashier")//
				.password("hi cashier");
	}
}
