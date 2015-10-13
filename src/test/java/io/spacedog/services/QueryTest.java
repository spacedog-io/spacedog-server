/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.google.common.collect.Lists;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class QueryTest extends AbstractTest {

	@Test
	public void resetAndImportDataset() throws UnirestException {
		AdminResourceTest.resetTestAccount();
		SchemaResourceTest.resetCarSchema();

		for (int i = 0; i < 500; i++) {
			RequestBodyEntity req2 = preparePost("/v1/data/car", AdminResourceTest.testClientKey())
					.body(jsonCar(i).toString());

			post(req2, 201);
		}
	}

	private JsonObject jsonCar(int i) {
		calendar.roll(Calendar.DAY_OF_MONTH, false);
		calendar.roll(Calendar.HOUR_OF_DAY, false);

		return Json.builder().add("serialNumber", String.valueOf(i))
				.add("buyDate", dateFormat.format(calendar.getTime()))
				.add("buyTime", timeFormat.format(calendar.getTime()))
				.add("buyTimestamp", timestampFormat.format(calendar.getTime())) //
				.add("color", CarColor.values()[i % 4].toString()) //
				.add("techChecked", i % 2 == 0) //
				.stObj("location") //
				.add("lat", 48.85341 + i / 100) //
				.add("lon", 2.3488 + i / 100) //
				.end() //
				.stObj("model").add("description", randomText()) //
				.add("fiscalPower", i % 11 + 2) //
				.add("size", Math.PI * i).build();
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
