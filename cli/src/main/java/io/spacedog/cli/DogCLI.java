package io.spacedog.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.common.base.Strings;

import io.spacedog.client.http.SpaceBackend;
import io.spacedog.client.http.SpaceEnv;
import io.spacedog.client.http.SpaceException;
import io.spacedog.utils.ClassResources;
import io.spacedog.utils.Utils;

public class DogCLI {

	public static void main(String... args) throws Exception {

		initEnv();
		JCommander cli = new JCommander();
		cli.setProgramName("spacedog");
		cli.addCommand(new VersionCommand());

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

			else if (command.equalsIgnoreCase("version"))
				showVersion();

			else if (command.equalsIgnoreCase("sync"))
				fileSynchCommand.synch();

			else if (command.equalsIgnoreCase("exportlog"))
				exportLogCommand.export();

			else if (command.equalsIgnoreCase("login"))
				loginCommand.login();

			System.exit(0);

		} catch (ParameterException e) {
			System.err.println(e.getMessage());

		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());

		} catch (SpaceException e) {
			System.err.println(e.getMessage());

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.exit(1);
	}

	private static void showVersion() {
		String version = ClassResources.loadAsString(DogCLI.class, "version.txt");
		Utils.info("SpaceDog CLI version %s", version);
	}

	private static void initEnv() {
		SpaceEnv env = new SpaceEnv();
		env.apiBackend(SpaceBackend.production);
		env.debug(false);
		SpaceEnv.env(env);
	}

	@Parameters(commandNames = { "version" }, //
			commandDescription = "show command version")
	private static class VersionCommand {
	}

}
