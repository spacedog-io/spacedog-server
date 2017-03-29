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

		FileSynchronizer synch = FileSynchronizer.newInstance();
		JCommander cli = new JCommander();
		cli.setProgramName("spacedog");
		cli.addCommand(synch);

		try {

			cli.parse(args);
			String command = cli.getParsedCommand();

			if (Strings.isNullOrEmpty(command))
				cli.usage();

			else if (command.equalsIgnoreCase("sync")) {
				String password = String.valueOf(//
						System.console().readPassword("Enter your admin password: "));
				synch.password(password).synch();
			}

		} catch (ParameterException e) {
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
