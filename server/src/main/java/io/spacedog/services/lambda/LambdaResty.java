/**
 * © David Attias 2015
 */
package io.spacedog.services.lambda;

import java.nio.ByteBuffer;
import java.util.Map;

import org.elasticsearch.rest.RestRequest.Method;

import com.amazonaws.http.SdkHttpMetadata;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;

import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.client.lambda.LambdaSettings;
import io.spacedog.server.Server;
import io.spacedog.server.ServerConfig;
import io.spacedog.server.SpaceFilter;
import io.spacedog.server.SpaceResty;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

@Prefix("/1/services")
public class LambdaResty extends SpaceResty {

	private final static AWSLambda lambda = AWSLambdaClient.builder()//
			.withRegion(ServerConfig.awsRegionOrDefault())//
			.build();

	static AWSLambda lambda() {
		return lambda;
	}

	//
	// Routes
	//

	public SpaceFilter filter() {

		return new SpaceFilter() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean matches(String uri, Context context) {
				// only accepts /1/services/* uris
				return uri.startsWith("/1/services") //
						&& (uri.length() > 11 || uri.charAt(11) == '/');
			}

			@Override
			public Payload apply(String uri, Context context, PayloadSupplier nextFilter) {

				LambdaSettings settings = null;
				LambdaRequest lambdaRequest = new LambdaRequest();
				lambdaRequest.method = Method.valueOf(context.method());
				lambdaRequest.path = toServiceSubPath(uri);
				lambdaRequest.parameters = extractParameters(context);
				lambdaRequest.headers = extractHeaders(context);
				lambdaRequest.payload = extractBody(context);

				InvokeRequest request = new InvokeRequest()//
						.withFunctionName(Server.backend().id() //
								+ '-' + settings.awsLambda.name)//
						.withPayload(lambdaRequest.toJsonString());

				InvokeResult result = lambda.invoke(request);

				SdkHttpMetadata metadata = result.getSdkHttpMetadata();
				return new Payload(metadata.getHttpHeaders().get(SpaceHeaders.CONTENT_TYPE), //
						result.getPayload().array(), metadata.getHttpStatusCode());
			}

		};
	}

	//
	// Implementation
	//

	public class LambdaRequest {
		public Method method;
		public String path;
		public Map<String, String> parameters;
		public Map<String, String> headers;
		public String payload;

		public ByteBuffer toJsonString() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	protected String extractBody(Context context) {
		// TODO Auto-generated method stub
		return null;
	}

	protected Map<String, String> extractParameters(Context context) {
		return null;
	}

	private Map<String, String> extractHeaders(Context context) {
		// TODO Auto-generated method stub
		return null;
	}

	private String toServiceSubPath(String uri) {
		return Utils.trimUntil(uri, "/services");
	}
}