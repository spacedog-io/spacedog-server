package io.spacedog.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Strings;

import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTarget;

public class DogCLI {

	public static void main(String... args) throws Exception {

		setEnv();
		JCommander cli = new JCommander();
		cli.setProgramName("spacedog");

		cli.addCommand(LoginCommand.get());
		cli.addCommand(LogCommand.get());
		cli.addCommand(FileSynchCommand.get());

		try {

			cli.parse(args);
			String command = cli.getParsedCommand();

			if (Strings.isNullOrEmpty(command))
				cli.usage();

			else if (command.equalsIgnoreCase("sync"))
				FileSynchCommand.get().synch();

			else if (command.equalsIgnoreCase("exportlog"))
				LogCommand.get().export();

			else if (command.equalsIgnoreCase("login"))
				LoginCommand.get().login();

		} catch (ParameterException e) {
			System.err.println(e.getMessage());

		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void setEnv() {
		SpaceEnv env = new SpaceEnv();
		env.target(SpaceTarget.production);
		env.debug(true);
		SpaceRequest.env(env);
	}

}
