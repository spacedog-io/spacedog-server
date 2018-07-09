package io.spacedog.tutorials;

import org.joda.time.DateTime;
import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.push.Installation;
import io.spacedog.client.push.PushProtocol;
import io.spacedog.client.schema.GeoPoint;
import io.spacedog.tutorials.Driver.LastLocation;

public class T2_DriverSignUp extends DemoBase {

	@Test
	public void superadminRegistersNewDriver() {

		SpaceDog superadmin = superadmin();

		superadmin.credentials().create("max", "hi max", "max@caremen.fr");
		SpaceDog max = SpaceDog.dog().username("max").password("hi max");

		superadmin.credentials().setRole(max.id(), "driver");

		Driver source = new Driver();
		source.status = "not-working";
		source.firstname = "Max";
		source.lastname = "La Menace";
		source.phone = "+33662627520";
		source.vehicule = new Driver.Vehicule();
		source.vehicule.brand = "Renault";
		source.vehicule.model = "Taliman";
		source.vehicule.color = "Rouge";
		source.vehicule.type = "premium";
		source.vehicule.licencePlate = "ZZ-666-ZZ";

		DataWrap<Driver> driver = DataWrap.wrap(source)//
				.id(max.id());

		driver.owner(max.id());

		superadmin.data().save(driver);
	}

	@Test
	public void driverCreatesHisInstallation() {

		SpaceDog max = max();

		Installation installation = new Installation()//
				.appId("carerec-driver")//
				.protocol(PushProtocol.APNS)//
				.token("0c9a956052bb439a63fdf9786062a683d2112af4c47ba5850498f60f00a86797")//
				.tags(max.id());

		max.push().saveInstallation(max.id(), installation);
	}

	@Test
	public void driverEntersIntoService() {

		SpaceDog max = max();

		// max sets her status to 'working'

		max.data().saveField("driver", max.id(), "status", "working");

		// max's phone sets regularly her last known position

		LastLocation location = new Driver.LastLocation();
		location.when = DateTime.now();
		location.where = new GeoPoint(48.816674, 2.230359);

		max.data().saveField("driver", max.id(), "lastLocation", location);
	}

	@Test
	public void superadminDeletesDriver() {

		SpaceDog max = max();
		superadmin().data().delete("driver", max.id(), false);
		superadmin().push().deleteInstallation(max.id(), false);
		superadmin().credentials().delete(max.id());
	}
}
