package io.spacedog.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Strings;

import io.spacedog.http.SpaceBackend;
import io.spacedog.http.SpaceEnv;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceRequestException;
import io.spacedog.utils.Utils;

public class DogCLI {

	public static void main(String... args) throws Exception {

		setEnv();
		JCommander cli = new JCommander();
		cli.setProgramName("spacedog");

		LoginCommand loginCommand = new LoginCommand();
		cli.addCommand(loginCommand);

		FileSynchCommand fileSynchCommand = new FileSynchCommand();
		cli.addCommand(fileSynchCommand);

		ExportLogCommand exportLogCommand = new ExportLogCommand();
		cli.addCommand(exportLogCommand);

		try {

			cli.parse(args);
			String command = cli.getParsedCommand();

			if (Strings.isNullOrEmpty(command))
				cli.usage();

			else if (command.equalsIgnoreCase("sync"))
				fileSynchCommand.synch();

			else if (command.equalsIgnoreCase("exportlog"))
				exportLogCommand.export();

			else if (command.equalsIgnoreCase("login"))
				loginCommand.login();

			Utils.info();
			Utils.info("Done.");

		} catch (ParameterException e) {
			System.err.println(e.getMessage());

		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());

		} catch (SpaceRequestException e) {
			System.err.println(e.serverErrorMessage());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void setEnv() {
		SpaceEnv env = new SpaceEnv();
		env.target(SpaceBackend.production);
		env.debug(false);
		SpaceRequest.env(env);
	}

}
