package io.spacedog.services.data;

import java.util.List;

public class SearchResults<T> {

	public String type;
	public long total = 0;
	public List<T> results;
}
