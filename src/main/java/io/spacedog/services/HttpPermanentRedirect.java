/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerSocketProcessor;
import org.simpleframework.transport.connect.SocketConnection;

public class HttpPermanentRedirect implements Container {

	private String destinationHostname;
	private SocketConnection socketConnection;

	private HttpPermanentRedirect(int portToRedirect, String destinationHostname) throws IOException {

		// TODO Create an url from the destinationHostname to make sure
		// we got the scheme and everything we need
		this.destinationHostname = destinationHostname;

		// pools of 1 thread should be sufficient for this task
		socketConnection = new SocketConnection( //
				new ContainerSocketProcessor(this, 1, 1));
		socketConnection.connect(new InetSocketAddress(portToRedirect));
	}

	public static HttpPermanentRedirect start(int portToRedirect, String destinationHostname) throws IOException {
		return new HttpPermanentRedirect(portToRedirect, destinationHostname);
	}

	@Override
	public void handle(Request request, Response response) {
		try {
			response.setValue("Location", destinationHostname + request.getPath());
			response.setStatus(Status.MOVED_PERMANENTLY);
			response.setContentLength(0);
			response.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void stop() throws IOException {
		socketConnection.close();
	}
}