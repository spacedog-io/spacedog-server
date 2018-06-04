package io.spacedog.services;

import java.io.IOException;
import java.io.OutputStream;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;

import net.codestory.http.payload.StreamingOutput;

public class DataExportStreamningOutput implements StreamingOutput {

	public static final int SIZE = 2000;
	public static final TimeValue TIMEOUT = TimeValue.timeValueMinutes(1);

	private static final byte[] JSON_START = "{\"id\":\"".getBytes();
	private static final byte[] JSON_SOURCE = "\",\"source\":".getBytes();
	private static final byte[] JSON_END = "}\n".getBytes();

	private SearchResponse response;

	public DataExportStreamningOutput(SearchResponse response) {
		this.response = response;
	}

	@Override
	public void write(OutputStream output) throws IOException {

		SearchHit[] hits = response.getHits().hits();

		if (hits.length > 0) {

			writeHits(output, hits);

			response = Start.get().getElasticClient()//
					.prepareSearchScroll(response.getScrollId())//
					.setScroll(TIMEOUT)//
					.get();

			write(output);
		}
	}

	private void writeHits(OutputStream output, SearchHit[] hits) throws IOException {
		for (SearchHit hit : hits) {
			output.write(JSON_START);
			output.write(hit.id().getBytes());
			output.write(JSON_SOURCE);
			output.write(hit.source());
			output.write(JSON_END);
		}
		output.flush();
	}

}
