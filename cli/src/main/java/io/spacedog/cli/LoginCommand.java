package io.spacedog.cli;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Strings;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.http.SpaceBackend;
import io.spacedog.client.http.SpaceEnv;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Utils;

@Parameters(commandNames = { "login" }, //
		commandDescription = "login to specific backend")
public class LoginCommand extends AbstractCommand<LoginCommand> {

	@Parameter(names = { "-b", "--backend" }, //
			required = true, //
			converter = SpaceBackendConverter.class, //
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
		SpaceEnv.env().debug(verbose());

		// first logout from any previous session in case new login fails
		// to avoid sending commands to previous dog session
		logout();

		Check.notNull(backend, "backend");
		Check.notNullOrEmpty(username, "username");

		String userHome = System.getProperty("user.home");
		Check.notNullOrEmpty(userHome, "no user home directory available");

		if (password == null)
			password = askForPassword();

		SpaceDog session = SpaceDog.dog(backend)//
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

	public static void logout() {
		uncheckedSession().ifPresent(dog -> {
			if (dog.isTokenStillValid())
				dog.logout();
		});
	}

	public static SpaceDog session() {
		return uncheckedSession()//
				.filter(session -> session.isTokenStillValid())//
				.orElseThrow(() -> Exceptions.runtime("you must login first"));
	}

	public static Optional<SpaceDog> uncheckedSession() {

		Optional<Path> path = sessionFilePath();

		if (path.isPresent() && Files.exists(path.get())) {

			Properties properties = new Properties();

			try (InputStream in = Files.newInputStream(path.get())) {

				properties.load(in);

				String backend = properties.getProperty("backend");
				String accesToken = properties.getProperty("accessToken");

				if (!Utils.isNullOrEmpty(backend) && !Utils.isNullOrEmpty(accesToken))
					return Optional.of(SpaceDog.dog(backend).accessToken(accesToken));

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return Optional.empty();
	}

	public static void clearSession() {

		sessionFilePath()//
				.filter(path -> Files.exists(path))//
				.ifPresent(path -> {
					try {
						Files.delete(path);
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
	}

	////////
	//////// Implementation
	////////

	private String askForPassword() {
		Console console = System.console();
		if (console == null)
			throw Exceptions.runtime("no console to ask for password");

		return String.valueOf(console.readPassword("Enter %s password: ", username));
	}

	private static Optional<Path> sessionFilePath() {
		String userHome = System.getProperty("user.home");

		if (Strings.isNullOrEmpty(userHome))
			return Optional.empty();

		return Optional.of(Paths.get(userHome, ".spacedog", "cli.properties"));
	}
}
