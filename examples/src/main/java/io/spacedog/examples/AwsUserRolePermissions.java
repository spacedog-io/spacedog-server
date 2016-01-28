/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.profile.ProfilesConfigFile;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;

public class AwsUserRolePermissions {

	public static void main(String[] args) {
		AWSCredentials davattiasCredentials = new ProfilesConfigFile().getCredentials("default");
		System.out.println("Davattias Access Key Id = " + davattiasCredentials.getAWSAccessKeyId());
		System.out.println("Davattias Secret Key = " + davattiasCredentials.getAWSSecretKey());

		AmazonIdentityManagementClient iamClient = new AmazonIdentityManagementClient();
		System.out.println("Current user = " + iamClient.getUser().getUser().toString());

		AWSSecurityTokenServiceClient stsClient = new AWSSecurityTokenServiceClient();
		stsClient.configureRegion(Regions.EU_WEST_1);
		AssumeRoleResult result = stsClient.assumeRole(new AssumeRoleRequest()//
				.withRoleArn("arn:aws:iam::309725721660:role/front-server")//
				.withRoleSessionName("toto"));

		System.out.println("Front-server Access Key Id = " + result.getCredentials().getAccessKeyId());
		System.out.println("Front-server Secret Key = " + result.getCredentials().getSecretAccessKey());

		AmazonS3Client s3 = new AmazonS3Client();
		System.out.println(s3.listBuckets());

		s3 = new AmazonS3Client(new BasicSessionCredentials(//
				result.getCredentials().getAccessKeyId(), //
				result.getCredentials().getSecretAccessKey(), //
				result.getCredentials().getSessionToken()));

		System.out.println(s3.listBuckets());

	}
}
