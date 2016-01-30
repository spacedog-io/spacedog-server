/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;

import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/v1/file")
public class FileResource extends AbstractS3Resource {

	private static final String FILE_BUCKET = "spacedog-files";

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Object getAll(Context context) {
		return doGet(Optional.empty(), context);
	}

	@Get("/:fileName")
	@Get("/:fileName/")
	public Object get(String fileName, Context context) {
		return doGet(Optional.of(fileName), context);
	}

	@Get("/:a/:fileName")
	@Get("/:a/:fileName/")
	public Object get(String a, String fileName, Context context) {
		return doGet(Optional.of(String.join(SLASH, a, fileName)), context);
	}

	@Put("/:fileName")
	@Put("/:fileName/")
	public Payload put(String fileName, byte[] bytes, Context context) {
		return doPut(null, fileName, bytes, context);
	}

	@Put("/:a/:fileName")
	@Put("/:a/:fileName/")
	public Payload put(String a, String fileName, byte[] bytes, Context context) {
		return doPut(a, fileName, bytes, context);
	}

	@Put("/:a/:b/:fileName")
	@Put("/:a/:b/:fileName/")
	public Payload put(String a, String b, String fileName, byte[] bytes, Context context) {
		return doPut(String.join(SLASH, a, b), fileName, bytes, context);
	}

	@Delete("")
	@Delete("/")
	public Payload deleteAll() {
		return doDelete(Optional.empty());
	}

	@Delete("/:a")
	@Delete("/:a/")
	public Payload delete(String a) {
		return doDelete(Optional.of(a));
	}

	@Delete("/:a/:b")
	@Delete("/:a/:b/")
	public Payload delete(String a, String b) {
		return doDelete(Optional.of(String.join(SLASH, a, b)));
	}

	@Delete("/:a/:b/:c")
	@Delete("/:a/:b/:c/")
	public Payload delete(String a, String b, String c) {
		return doDelete(Optional.of(String.join(SLASH, a, b, c)));
	}

	private Object doGet(Optional<String> path, Context context) {
		Credentials credentials = SpaceContext.checkCredentials();
		return doGet(FILE_BUCKET, credentials.backendId(), path, context);
	}

	private Payload doPut(String path, String fileName, byte[] bytes, Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		return doUpload(FILE_BUCKET, "/v1/file", credentials, path, fileName, bytes, context);
	}

	private Payload doDelete(Optional<String> path) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		return doDelete(FILE_BUCKET, credentials, path);
	}

	//
	// singleton
	//

	private static FileResource singleton = new FileResource();

	static FileResource get() {
		return singleton;
	}

	private FileResource() {
	}
}
