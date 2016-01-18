/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfilesConfigFile;

public class AwsCredentials {

	public static void main(String[] args) {
		AWSCredentials davattiasCredentials = new ProfilesConfigFile().getCredentials("default");
		System.out.println("Davattias Access Key Id = " + davattiasCredentials.getAWSAccessKeyId());
		System.out.println("Davattias Secret Key = " + davattiasCredentials.getAWSSecretKey());
	}
}
