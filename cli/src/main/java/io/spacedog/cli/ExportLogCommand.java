package io.spacedog.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.joda.time.DateTime;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.sdk.SpaceDog;
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

		ObjectNode query = Json.objectBuilder().object("range")//
				.object("receivedAt").put("gte", gte.toString())//
				.put("lt", lt.toString()).build();

		JsonNode payload = dog.post("/1/log/search").bodyJson(query)//
				.size(10000).go(200).asJson();

		Files.write(target, Json.toPrettyString(payload).getBytes(Utils.UTF8));
	}
}
