package io.spacedog.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.joda.time.DateTime;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

@Parameters(commandNames = { "exportlog" }, //
		commandDescription = "export log to file")
public class ExportLogCommand extends AbstractCommand<ExportLogCommand> {

	@Parameter(names = { "-d", "--day" }, //
			required = false, //
			description = "the day to export (ex. 2017-03-23)")
	private String day;

	@Parameter(names = { "-f", "--file" }, //
			required = false, //
			description = "the file to export to")
	private String file;

	public ExportLogCommand file(String file) {
		this.file = file;
		return this;
	}

	public ExportLogCommand day(String day) {
		this.day = day;
		return this;
	}

	public void export() throws IOException {

		SpaceRequest.env().debug(verbose());
		SpaceDog dog = LoginCommand.session();

		DateTime date = Strings.isNullOrEmpty(day) ? DateTime.now() //
				: DateTime.parse(day);

		DateTime gte = date.withTimeAtStartOfDay();
		DateTime lt = date.plusDays(1);

		if (Strings.isNullOrEmpty(file))
			file = "log-" + gte + ".json";

		Path target = Paths.get(file);

		ObjectNode query = Json.builder().object()//
				.add("size", 5000)//
				.add("sort", "receivedAt")//
				.object("query")//
				.object("range")//
				.object("receivedAt")//
				.add("gte", gte.toString())//
				.add("lt", lt.toString())//
				.build();

		ObjectNode payload = dog.post("/1/log/search").refresh()//
				.bodyJson(query).go(200).asJsonObject();

		if (payload.get("total").asLong() > 5000)
			System.err.println("WARNING: log export limited to the first 5000 logs");

		Files.write(target, Json.toPrettyString(payload).getBytes(Utils.UTF8));
	}
}
