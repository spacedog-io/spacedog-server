package io.spacedog.server;

import java.util.List;

public class SearchResults<T> {

	public String type;
	public long total = 0;
	public List<T> results;
}
