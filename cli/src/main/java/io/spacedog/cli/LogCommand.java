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

import io.spacedog.rest.SpaceEnv;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json7;
import io.spacedog.utils.Utils;

@Parameters(commandNames = { "log" }, //
		commandDescription = "export logs to file")
public class LogCommand extends AbstractCommand<LogCommand> {

	@Parameter(names = { "-d", "--day" }, //
			required = false, //
			description = "the day to export (ex. 2017-03-23)")
	private String day;

	@Parameter(names = { "-f", "--file" }, //
			required = true, //
			description = "the file to export to")
	private String file;

	public LogCommand file(String file) {
		this.file = file;
		return this;
	}

	public LogCommand day(String day) {
		this.day = day;
		return this;
	}

	private LogCommand() {
	}

	public void export() throws IOException {

		SpaceEnv.defaultEnv().debug(verbose());
		SpaceDog dog = LoginCommand.get().session();

		DateTime date = Strings.isNullOrEmpty(day) ? DateTime.now() //
				: DateTime.parse(day);

		DateTime gte = date.withTimeAtStartOfDay();
		DateTime lt = date.plusDays(1);

		if (Strings.isNullOrEmpty(file))
			file = "log-" + gte + ".json";

		Path target = Paths.get(file);

		ObjectNode query = Json7.objectBuilder().object("range")//
				.object("receivedAt").put("gte", gte.toString())//
				.put("lt", lt.toString()).build();

		String payload = dog.post("/1/log/search").bodyJson(query)//
				.size(10000).go(200).string();

		Files.write(target, payload.getBytes(Utils.UTF8));
	}

	//
	// Singleton
	//

	private static LogCommand instance = null;

	public static LogCommand get() {
		if (instance == null)
			instance = new LogCommand();
		return instance;
	}

}
