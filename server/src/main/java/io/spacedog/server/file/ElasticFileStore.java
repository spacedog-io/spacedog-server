/**
 * © David Attias 2015
 */
package io.spacedog.server.file;

import java.io.InputStream;
import java.util.Iterator;

public class ElasticFileStore implements FileStore {

	ElasticFileStore() {
	}

	@Override
	public PutResult put(String backendId, String bucket, InputStream bytes, Long length) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists(String backendId, String bucket, String key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public InputStream get(String backendId, String bucket, String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<String> list(String backendId, String bucket) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteAll() {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAll(String backendId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAll(String backendId, String bucket) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(String backendId, String bucket, String key) {
		// TODO Auto-generated method stub

	}

}
