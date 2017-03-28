/**
 * © David Attias 2015
 */
package io.spacedog.test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.spacedog.model.GeoPoint;
import io.spacedog.model.Schema;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json7;

public class QueryTest extends SpaceTest {

	@Test
	public void importCarDataset() {

		prepareTest();
		SpaceDog test = resetTestBackend();

		test.schema().set(buildCarSchema());

		for (int i = 0; i < 500; i++)
			SpaceRequest.post("/1/data/car").adminAuth(test).body(jsonCar(i)).go(201);
	}

	private Schema buildCarSchema() {
		return Schema.builder("car") //
				.string("serialNumber")//
				.date("buyDate")//
				.time("buyTime")//
				.timestamp("buyTimestamp") //
				.enumm("color")//
				.bool("techChecked") //
				.geopoint("location") //

				.object("model")//
				.text("description").french()//
				.integer("fiscalPower")//
				.floatt("size")//
				.close() //

				.build();
	}

	private ObjectNode jsonCar(int i) {
		calendar.roll(Calendar.DAY_OF_MONTH, false);
		calendar.roll(Calendar.HOUR_OF_DAY, false);

		return Json7.objectBuilder().put("serialNumber", String.valueOf(i))
				.put("buyDate", dateFormat.format(calendar.getTime()))
				.put("buyTime", timeFormat.format(calendar.getTime()))
				.put("buyTimestamp", timestampFormat.format(calendar.getTime())) //
				.put("color", CarColor.values()[i % 4].toString()) //
				.put("techChecked", i % 2 == 0) //
				.object("location") //
				.put("lat", 48.85341 + i / 100) //
				.put("lon", 2.3488 + i / 100) //
				.end() //
				.object("model").put("description", randomText()) //
				.put("fiscalPower", i % 11 + 2) //
				.put("size", Math.PI * i).build();
	}

	public static class Car {

		public Car(int i) {
			serialNumber = String.valueOf(i);
			calendar.roll(Calendar.DAY_OF_MONTH, false);
			calendar.roll(Calendar.HOUR_OF_DAY, false);
			buyDate = calendar.getTime();
			buyTime = calendar.getTime();
			buyTimestamp = calendar.getTime();
			color = CarColor.values()[i % 4];
			techChecked = i % 2 == 0;
			model = new Model();
			model.description = randomText();
			model.fiscalPower = i % 11;
			model.size = i;
		}

		String serialNumber;
		Date buyDate;
		Date buyTime;
		Date buyTimestamp;
		CarColor color;
		boolean techChecked;
		Model model;
		GeoPoint location;
	}

	public enum CarColor {
		BLUE, RED, YELLOW, GREEN
	}

	public static class Model {
		String description;
		int fiscalPower;
		float size;
	}

	public static String randomText() {

		if (dico == null) {
			dico = Lists.newArrayList(dicoBase.split("[ ,.:;?!()\\n\\']"));
		}

		StringBuilder builder = new StringBuilder();
		for (int j = 0; j < 50; j++) {
			builder.append(dico.get((int) (Math.random() * dico.size()))).append(' ');
		}
		return builder.toString();
	}

	private static Calendar calendar = new Calendar.Builder().setDate(2014, 12, 31).setTimeOfDay(15, 35, 0).build();

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");

	private static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

	private static SimpleDateFormat timestampFormat = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSZ");

	private static String dicoBase = "Avec ses voitures électriques, Tesla a rencontré un gros succès aux Etats-Unis (mais pas en Chine)."
			+ " La société a décidé de s'ouvrir un nouveau marché bien plus large en proposant des batteries destinées à alimenter les foyers."
			+ " Elles prennent la forme de modules d'une puissance de 10 kWh. Il est possible d'en assembler jusqu'à 9 dans ce qui ressemble"
			+ " ici à un gros réfrigérateur, de quoi alimenter même de petites entreprises. Si l'on y ajoute des panneaux solaires et que"
			+ " l'on tire partie des systèmes d'heures pleines et d'heures creuses, ces systèmes pourraient permettre des économies"
			+ " d'énergies conséquentes. La société prévoit même d'aller encore plus loin et de proposer des GWh dans le futur pour"
			+ " les industries. Chaque module de 10 kWh est vendu 3500$, ce qui n'est pas une somme colossale. L'avenir nous dira si"
			+ " Tesla arrive à révolutionner la distribution de l'énergie aux Etats-Unis.";

	private static List<String> dico = null;
}
