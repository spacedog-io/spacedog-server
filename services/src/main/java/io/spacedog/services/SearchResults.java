package io.spacedog.services;

import java.util.List;

public class SearchResults<T> {

	public String type;

	public long total = 0;
	public int from = 0;
	public int size = 10;

	public List<T> results;
}
