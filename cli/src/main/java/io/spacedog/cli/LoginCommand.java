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

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceBackend;
import io.spacedog.http.SpaceEnv;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Utils;

@Parameters(commandNames = { "login" }, //
		commandDescription = "login to specific backend")
public class LoginCommand extends AbstractCommand<LoginCommand> {

	@Parameter(names = { "-b", "--backend" }, //
			required = true, //
			description = "the backend to login to (ex. https://vick.spacedog.io)")
	private SpaceBackend backend;

	@Parameter(names = { "-u", "--username" }, //
			required = true, //
			description = "the username to login with")
	private String username;

	private String password;

	public LoginCommand backend(String backend) {
		this.backend = SpaceBackend.valueOf(backend);
		return this;
	}

	public LoginCommand backend(SpaceBackend backend) {
		this.backend = backend;
		return this;
	}

	public LoginCommand username(String username) {
		this.username = username;
		return this;
	}

	public LoginCommand password(String password) {
		this.password = password;
		return this;
	}

	public SpaceDog login() {
		Check.notNull(backend, "backend");
		Check.notNullOrEmpty(username, "username");

		SpaceEnv.env().debug(verbose());
		String userHome = System.getProperty("user.home");

		if (Strings.isNullOrEmpty(userHome))
			throw Exceptions.runtime("no user home directory available");

		Console console = System.console();
		if (password == null)
			password = console == null ? "hi " + username //
					: String.valueOf(//
							console.readPassword("Enter %s password: ", username));

		session = SpaceDog.dog(backend)//
				.username(username).login(password);

		OutputStream out = null;
		Path path = Paths.get(userHome, ".spacedog");

		try {
			Files.createDirectories(path);
			path = path.resolve("cli.properties");

			Properties properties = new Properties();
			properties.put("backend", backend.url());
			properties.put("accessToken", session.accessToken().get());

			out = Files.newOutputStream(path);
			properties.store(out, "SpaceDog CLI properties");

		} catch (IOException e) {
			throw Exceptions.runtime(e, "error writing CLI session properties");

		} finally {
			Utils.closeSilently(out);
		}

		return session;
	}

	//
	// Static part
	//

	private static SpaceDog session;

	public static SpaceDog session() throws IOException {

		if (session == null) {

			String userHome = System.getProperty("user.home");

			if (Strings.isNullOrEmpty(userHome))
				throw Exceptions.runtime("no user home directory available");

			Path path = Paths.get(userHome, ".spacedog", "cli.properties");

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

			session = SpaceDog.dog(properties.get("backend").toString())//
					.accessToken(properties.get("accessToken").toString());
		}

		if (!session.isTokenStillValid())
			throw Exceptions.runtime("SpaceDog session has expired, you must login again.");

		return session;
	}

	public static void clearCache() {
		session = null;
	}

}
