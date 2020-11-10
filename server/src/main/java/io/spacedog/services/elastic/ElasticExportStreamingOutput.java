package io.spacedog.services.elastic;

import java.io.IOException;
import java.io.OutputStream;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;

import io.spacedog.server.Server;
import net.codestory.http.payload.StreamingOutput;

public class ElasticExportStreamingOutput implements StreamingOutput {

	public static final int SIZE = 2000;
	public static final TimeValue TIMEOUT = TimeValue.timeValueMinutes(1);

	private static final byte[] JSON_START = "{\"id\":\"".getBytes();
	private static final byte[] JSON_SOURCE = "\",\"source\":".getBytes();
	private static final byte[] JSON_END = "}\n".getBytes();

	private SearchResponse response;

	public ElasticExportStreamingOutput(SearchResponse response) {
		this.response = response;
	}

	@Override
	public void write(OutputStream output) throws IOException {

		SearchHit[] hits = response.getHits().getHits();

		if (hits.length > 0) {
			writeHits(output, hits);
			response = Server.get().elasticClient()//
					.scroll(response.getScrollId(), TIMEOUT);
			write(output);
		}
	}

	private void writeHits(OutputStream output, SearchHit[] hits) throws IOException {
		for (SearchHit hit : hits) {
			output.write(JSON_START);
			output.write(hit.getId().getBytes());
			output.write(JSON_SOURCE);
			output.write(BytesReference.toBytes(hit.getSourceRef()));
			output.write(JSON_END);
		}
		output.flush();
	}

}