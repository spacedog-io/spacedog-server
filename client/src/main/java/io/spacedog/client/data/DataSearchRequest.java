package io.spacedog.client.data;

public class DataSearchRequest {

	public boolean refresh;
	public String type;
	public String source;

	public DataSearchRequest refresh(boolean refresh) {
		this.refresh = refresh;
		return this;
	}

	public DataSearchRequest type(String type) {
		this.type = type;
		return this;
	}

	public DataSearchRequest source(String source) {
		this.source = source;
		return this;
	}

}