/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.List;
import java.util.UUID;

import com.google.common.base.Strings;

import io.spacedog.http.WebPath;
import io.spacedog.model.Credentials;
import io.spacedog.model.DownloadRequest;
import io.spacedog.model.Permission;
import io.spacedog.model.ShareSettings;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/1/shares")
public class ShareService extends S3Service {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Object getAll(Context context) {
		shareSettings().permissions.check(SpaceContext.credentials(), Permission.search);
		S3File file = new S3File(getBucketName(), WebPath.ROOT).rootUri("/1/shares");
		return doList(file, context);
	}

	@Post("")
	@Post("/")
	public Payload post(Context context) {
		ShareSettings settings = shareSettings();
		Credentials credentials = SpaceContext.credentials();
		settings.permissions.check(credentials, Permission.create);

		long contentLength = checkContentLength(context, settings.sizeLimitInKB);
		String id = UUID.randomUUID().toString();
		String fileName = context.get("fileName");
		if (!Strings.isNullOrEmpty(fileName))
			id = id + '-' + fileName;

		S3File file = new S3File(getBucketName(), WebPath.newPath(id))//
				.contentLength(contentLength)//
				.fileName(fileName)//
				.owner(credentials)//
				.rootUri("/1/shares");

		return doUpload(file, settings.enableS3Location, context);
	}

	@Delete("")
	@Delete("/")
	public Payload deleteAll() {
		shareSettings().permissions.check(//
				SpaceContext.credentials(), Permission.delete);
		return doDeleteAll(new S3File(getBucketName(), WebPath.ROOT));
	}

	@Post("/download")
	@Post("/download/")
	public Payload postDownload(DownloadRequest request, Context context) {
		List<S3File> files = toS3Files(getBucketName(), request.paths);
		S3File.checkPermissions(files, shareSettings().permissions, //
				Permission.read, Permission.readGroup, Permission.readMine);
		return doZip(files, request.fileName);
	}

	@Get("/:id")
	@Get("/:id/")
	public Payload get(String id, Context context) {
		S3File file = new S3File(getBucketName(), WebPath.newPath(id));
		file.checkRead(shareSettings().permissions);
		return doGet(true, file, context);
	}

	@Delete("/:id")
	@Delete("/:id/")
	public Payload delete(String id, Context context) {
		S3File file = new S3File(getBucketName(), WebPath.newPath(id));
		file.checkDelete(shareSettings().permissions);
		return doDelete(file);
	}

	//
	// Implementation
	//

	private ShareSettings shareSettings() {
		return SettingsService.get().getAsObject(ShareSettings.class);
	}

	public static String getBucketName() {
		return getBucketName("shared");
	}

	//
	// singleton
	//

	private static ShareService singleton = new ShareService();

	static ShareService get() {
		return singleton;
	}

	private ShareService() {
		SettingsService.get().registerSettings(ShareSettings.class);
	}
}
