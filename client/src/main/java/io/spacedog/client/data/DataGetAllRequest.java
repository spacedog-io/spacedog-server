package io.spacedog.client.data;

public class DataGetAllRequest {

	public String type;
	public int from = 0;
	public int size = 10;
	public boolean refresh;
	public String q;

	public DataGetAllRequest type(String type) {
		this.type = type;
		return this;
	}

	public DataGetAllRequest q(String q) {
		this.q = q;
		return this;
	}

	public DataGetAllRequest from(int from) {
		this.from = from;
		return this;
	}

	public DataGetAllRequest size(int size) {
		this.size = size;
		return this;
	}

	public DataGetAllRequest refresh() {
		this.refresh = true;
		return this;
	}

}