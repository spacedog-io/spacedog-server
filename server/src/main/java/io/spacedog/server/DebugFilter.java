package io.spacedog.server;

import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.jobs.Internals;
import net.codestory.http.Context;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

@SuppressWarnings("serial")
public class DebugFilter implements SpaceFilter {

	@Override
	public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {

		Payload payload = nextFilter.get();

		try {
			Debug debug = Server.context().debug();
			if (debug.isTrue())
				payload.withHeader(SpaceHeaders.SPACEDOG_DEBUG, debug.toNode().toString());

		} catch (Exception e) {
			Internals.get().notify("unable to add debug header to payload", e);
		}

		return payload;
	}

}