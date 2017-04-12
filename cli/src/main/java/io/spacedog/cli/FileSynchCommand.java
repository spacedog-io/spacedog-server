package io.spacedog.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import io.spacedog.rest.SpaceEnv;
import io.spacedog.sdk.FileEndpoint.File;
import io.spacedog.sdk.FileEndpoint.FileList;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.WebPath;

@Parameters(commandNames = { "sync" }, //
		commandDescription = "synchronize source folder to backend")
public class FileSynchCommand extends AbstractCommand<FileSynchCommand> {

	@Parameter(names = { "-s", "--source" }, //
			required = true, //
			description = "the source directory to synchronize")
	private String source;

	@Parameter(names = { "-p", "--prefix" }, //
			required = true, //
			description = "the file bucket prefix to use")
	private String prefix;

	@Parameter(names = { "-l", "--login" }, //
			required = true, //
			description = "the administrator login to use")
	private String login;

	/**
	 * Set of server file path checked with matching local files. A checked
	 * local file is uploaded if different.
	 */
	private Set<String> checked = Sets.newHashSet();

	/**
	 * Set of local file path uploaded to the server.
	 */
	private Set<String> uploaded = Sets.newHashSet();

	/**
	 * Set of server file path deleted from the server.
	 */
	private Set<String> deleted = Sets.newHashSet();

	private SpaceDog backend;

	public FileSynchCommand source(String source) {
		this.source = source;
		return this;
	}

	public FileSynchCommand source(Path source) {
		this.source = source.toString();
		return this;
	}

	public FileSynchCommand prefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	private FileSynchCommand() {
	}

	public void synch() throws IOException {
		Check.notNull(source, "source");
		Check.notNullOrEmpty(prefix, "prefix");

		SpaceEnv.defaultEnv().debug(verbose());
		backend = LoginCommand.get().session();

		synchFromServer();
		synchFromLocal();
	}

	public Set<String> uploaded() {
		return Collections.unmodifiableSet(uploaded);
	}

	public Set<String> deleted() {
		return Collections.unmodifiableSet(deleted);
	}

	public Set<String> checked() {
		return Collections.unmodifiableSet(checked);
	}

	public void reset() {
		uploaded.clear();
		deleted.clear();
		checked.clear();
	}

	//
	// Implementation
	//

	private void synchFromServer() throws IOException {
		String next = null;
		String webPath = WebPath.newPath(prefix).toString();

		do {
			FileList list = backend.file().list(webPath, next);

			for (File file : list.files)
				check(file);

			next = list.next;

		} while (next != null);
	}

	private void check(File file) throws IOException {
		// removes slash, prefix and slash
		String relativePath = file.path.substring(prefix.length() + 2);
		Path filePath = Paths.get(source).resolve(relativePath);

		if (Files.isRegularFile(filePath)) {
			checked.add(file.path);

			if (!check(filePath, file.etag))
				upload(filePath);

		} else
			delete(file.path);
	}

	private void synchFromLocal() throws IOException {

		Files.walk(Paths.get(source))//
				.filter(path -> !path.getFileName().toString().startsWith("."))//
				.filter(Files::isRegularFile)//
				.filter(this::notAlreadyCheckedAndSynched)//
				.forEach(path -> upload(path));
	}

	private boolean notAlreadyCheckedAndSynched(Path filePath) {
		String webPath = toWebPath(filePath).toString();
		return !checked.contains(webPath);
	}

	private WebPath toWebPath(Path filePath) {
		return WebPath//
				.parse(Paths.get(source).relativize(filePath).toString())//
				.addFirst(prefix);
	}

	private void delete(String webPath) {
		backend.file().delete(webPath);
		deleted.add(webPath);
	}

	private void upload(Path filePath) {

		try {
			String webPath = toWebPath(filePath).toString();
			// no need to escape since sdk is already escaping
			backend.file().save(webPath, filePath);
			uploaded.add(webPath);

		} catch (Exception e) {
			Exceptions.runtime(e);
		}
	}

	private boolean check(Path path, String etag) throws IOException {
		HashCode local = com.google.common.io.Files.hash(path.toFile(), Hashing.md5());
		return local.equals(HashCode.fromString(etag));
	}

	//
	// Singleton
	//

	private static FileSynchCommand instance = null;

	public static FileSynchCommand get() {
		if (instance == null)
			instance = new FileSynchCommand();
		return instance;
	}

}
