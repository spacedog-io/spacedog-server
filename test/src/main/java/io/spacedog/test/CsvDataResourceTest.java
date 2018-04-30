package io.spacedog.test;

import java.io.StringReader;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import io.spacedog.model.CsvRequest;
import io.spacedog.model.CsvRequest.Column.Type;
import io.spacedog.model.GeoPoint;
import io.spacedog.model.Schema;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.DataObject;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.RandomUtils;

public class CsvDataResourceTest extends SpaceTest {

	@Test
	public void test() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		Schema schema = Course.schema();
		superadmin.schema().set(schema);

		// superadmin creates bunch of random courses
		Set<Course> objects = Sets.newHashSet();
		for (int i = 0; i < 2000; i++) {
			Course course = randomCourse();
			superadmin.data().save(course);
			objects.add(course);
		}

		// superadmin exports all courses
		CsvRequest request = new CsvRequest();
		request.refresh = true;
		request.settings = new CsvRequest.Settings();
		request.settings.delimiter = ';';
		request.addColumn("meta.createdAt");
		request.addColumn("status");
		request.addColumn("to.address");
		request.addColumn("to.geopoint");
		request.addColumn("tags");
		request.addColumn("time");
		request.addColumn("fare");

		String csv = superadmin.data().csv(Course.TYPE, request).asString();

		//
		CsvParserSettings settings = new CsvParserSettings();
		settings.getFormat().setDelimiter(request.settings.delimiter);
		CsvParser parser = new CsvParser(settings);
		List<String[]> all = parser.parseAll(new StringReader(csv));

		assertEquals(objects.size(), all.size());
		for (String[] strings : all)
			assertTrue(objects.contains(toObject(strings)));

		//
		request.settings.firstRowOfHeaders = true;
		request.columns.get(0).type = Type.timestamp;
		request.columns.get(0).pattern = "dd/MM/yy";
		request.columns.get(6).type = Type.floating;
		request.columns.get(6).pattern = "#.##";

		csv = superadmin.data().csv(Course.TYPE, request).asString();
		System.out.println(csv);
	}

	private Course toObject(String[] strings) {
		Course course = new Course();
		course.createdAt(DateTime.parse(strings[0]));
		course.status = strings[1];
		course.to = new Course.Location();
		course.to.address = strings[2];
		course.time = Long.valueOf(strings[5]);
		course.fare = Double.valueOf(strings[6]);
		return course;
	}

	private static Course randomCourse() {
		Course course = new Course();
		course.status = RandomUtils.nextKeyword();
		course.tags = Lists.newArrayList("hi", "ola");
		course.time = RandomUtils.nextLong();
		course.fare = RandomUtils.nextDouble();
		course.to = new Course.Location();
		course.to.address = RandomUtils.nextAddress();
		course.to.geopoint = RandomUtils.nextGeoPoint();
		return course;
	}

	public static class Course extends DataObject<Course> {

		public static final String TYPE = "course";

		public String status;
		public List<String> tags;
		public Location to;
		public Double fare;
		public Long time; // in millis

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Location {
			public String address;
			public GeoPoint geopoint;
		}

		@Override
		public boolean equals(Object obj) {
			Course course = (Course) obj;
			return course.status.equals(status) //
					&& course.to.address.equals(to.address) //
					&& course.time.equals(time) //
					&& course.fare.equals(fare);
		}

		@Override
		public int hashCode() {
			return status.hashCode();
		}

		public static Schema schema() {
			return Schema.builder(Course.TYPE) //

					.string("status") //
					.string("tags").array() //
					.floatt("fare") // in euros
					.longg("time") // in millis

					.object("to")//
					.text("address").french()//
					.geopoint("geopoint")//
					.close()//

					.build();
		}
	}
}
