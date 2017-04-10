package io.spacedog.cli;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Strings;

import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;

@Parameters(commandNames = { "login" }, //
		commandDescription = "login to specific backend")
public class LoginCommand {

	@Parameter(names = { "-b", "--backend" }, //
			required = true, //
			description = "the backend to login to (ex. https://vick.spacedog.io)")
	private String backend;

	@Parameter(names = { "-u", "--username" }, //
			required = true, //
			description = "the username to login with")
	private String username;

	private SpaceDog session = null;

	private LoginCommand() {
	}

	public LoginCommand backend(String backend) {
		this.backend = backend;
		return this;
	}

	public LoginCommand username(String username) {
		this.username = username;
		return this;
	}

	public LoginCommand clearCache() {
		this.session = null;
		return this;
	}

	public SpaceDog login() throws IOException {
		Check.notNullOrEmpty(backend, "backend");
		Check.notNullOrEmpty(username, "username");

		String userHome = System.getProperty("user.home");

		if (Strings.isNullOrEmpty(userHome))
			throw Exceptions.runtime("no user home directory available");

		Console console = System.console();
		String password = console == null ? "hi test" //
				: String.valueOf(//
						console.readPassword("Enter ‰s password: ", username));

		session = SpaceDog.backend(backend)//
				.username(username).login(password);

		Path path = Paths.get(userHome, ".spacedog.properties");

		Properties properties = new Properties();
		properties.put("backend", backend);
		properties.put("accessToken", session.accessToken().get());

		OutputStream out = null;
		try {
			out = Files.newOutputStream(path);
			properties.store(out, "SpaceDog CLI properties");

		} finally {
			if (out != null)
				out.close();
		}

		return session;
	}

	public SpaceDog session() throws IOException {

		if (session == null) {

			String userHome = System.getProperty("user.home");

			if (Strings.isNullOrEmpty(userHome))
				throw Exceptions.runtime("no user home directory available");

			Path path = Paths.get(userHome, ".spacedog.properties");

			if (!Files.exists(path))
				throw Exceptions.runtime("you must first login");

			Properties properties = new Properties();

			InputStream in = null;
			try {
				in = Files.newInputStream(path);
				properties.load(in);

			} finally {
				if (in != null)
					in.close();
			}

			session = SpaceDog.backend(properties.get("backend").toString())//
					.accessToken(properties.get("accessToken").toString());
		}

		if (!session.isTokenStillValid())
			throw Exceptions.runtime("SpaceDog session has expired, you must login again.");

		return session;
	}

	//
	// Singleton
	//

	private static LoginCommand instance = null;

	public static LoginCommand get() {
		if (instance == null)
			instance = new LoginCommand();
		return instance;
	}

}
