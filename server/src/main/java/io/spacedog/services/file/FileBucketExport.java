package io.spacedog.services.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.io.ByteStreams;

import io.spacedog.client.file.SpaceFile;
import io.spacedog.services.Services;
import io.spacedog.utils.Utils;
import net.codestory.http.payload.StreamingOutput;

class FileBucketExport implements StreamingOutput {

	private String bucket;
	private List<SpaceFile> files;
	private boolean flatZip;

	public FileBucketExport(String bucket, boolean flatZip, List<SpaceFile> files) {
		this.bucket = bucket;
		this.files = files;
		this.flatZip = flatZip;
	}

	@Override
	public void write(OutputStream output) throws IOException {
		ZipOutputStream zip = new ZipOutputStream(output);
		for (SpaceFile file : files) {
			zip.putNextEntry(toZipEntry(file));
			InputStream fileStream = Services.files().getAsByteStream(bucket, file);
			ByteStreams.copy(fileStream, zip);
			Utils.closeSilently(fileStream);
			zip.flush();
		}
		zip.close();
	}

	private ZipEntry toZipEntry(SpaceFile file) {
		String path = file.getPath();
		if (flatZip)
			path = SpaceFile.flatPath(path);
		return new ZipEntry(path);
	}

}