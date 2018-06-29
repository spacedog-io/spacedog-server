package io.spacedog.services.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.io.ByteStreams;

import io.spacedog.server.Services;
import io.spacedog.utils.Utils;
import net.codestory.http.payload.StreamingOutput;

class FileBucketExport implements StreamingOutput {

	private String bucket;
	private List<DogFile> files;

	public FileBucketExport(String bucket, List<DogFile> files) {
		this.bucket = bucket;
		this.files = files;
	}

	@Override
	public void write(OutputStream output) throws IOException {
		ZipOutputStream zip = new ZipOutputStream(output);
		for (DogFile file : files) {
			zip.putNextEntry(new ZipEntry(file.getPath()));
			InputStream fileStream = Services.files().getContent(bucket, file);
			ByteStreams.copy(fileStream, zip);
			Utils.closeSilently(fileStream);
			zip.flush();
		}
		zip.close();
	}

}