package io.spacedog.client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Strings;

public class DogCli {

	public static void main(String... args) throws Exception {

		configureSpaceRequests();

		FileSynchronizer synch = FileSynchronizer.newInstance();
		JCommander cli = new JCommander();
		cli.setProgramName("dog");
		cli.addCommand(synch);

		try {

			cli.parse(args);
			String command = cli.getParsedCommand();

			if (Strings.isNullOrEmpty(command))
				cli.usage();

			else if (command.equalsIgnoreCase("sync")) {
				String password = String.valueOf(//
						System.console().readPassword("Enter your SpaceDog admin password: "));
				synch.password(password).synch();
			}

		} catch (ParameterException e) {
			System.err.println(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void configureSpaceRequests() {
		SpaceRequestConfiguration configuration = new SpaceRequestConfiguration();
		configuration.target(SpaceTarget.production);
		configuration.debug(true);
		SpaceRequest.setConfigurationDefault(configuration);
	}

}
