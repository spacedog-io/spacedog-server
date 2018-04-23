package io.spacedog.client.lambda;

import io.spacedog.client.settings.SettingsBase;

public class LambdaSettings extends SettingsBase {

	public enum Type {
		awsLambda;
	}

	public class AwsLambda {
		public String testUrl;
		public String lambdaArn;
		public String name;
	}

	public Type type;
	public AwsLambda awsLambda = new AwsLambda();

}
