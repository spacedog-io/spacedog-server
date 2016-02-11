/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.AccessKey;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.AttachUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.PutUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.User;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.CORSRule;
import com.amazonaws.services.s3.model.CORSRule.AllowedMethods;
import com.google.common.collect.ImmutableList;

/**
 * @author davattias
 */
public class S3Client {

	private static final Regions JOHO_REGION = Regions.EU_CENTRAL_1;
	private static final String JOHO_BUCKET_NAME = "spacedog-joho";
	private static final String JOHO_BACKEND_ID = "joho";

	public static void main(String[] args) {

		try {
			// avoid caching problems on dns lookups
			java.security.Security.setProperty("networkaddress.cache.ttl", "60");

			AWSCredentials davattiasCredentials = new ProfileCredentialsProvider().getCredentials();
			System.out.println("Davattias Access Key Id = " + davattiasCredentials.getAWSAccessKeyId());
			System.out.println("Davattias Secret Key = " + davattiasCredentials.getAWSSecretKey());

			AmazonIdentityManagement iam = new AmazonIdentityManagementClient(davattiasCredentials)
					.withRegion(JOHO_REGION);

			User davattiasUser = iam.getUser(new GetUserRequest().withUserName("davattias")).getUser();
			System.out.println(davattiasUser);

			User johoUser = null;
			try {
				johoUser = iam.getUser(new GetUserRequest().withUserName(JOHO_BACKEND_ID)).getUser();
			} catch (NoSuchEntityException e) {
				johoUser = iam.createUser(new CreateUserRequest(JOHO_BACKEND_ID)).getUser();
			}
			System.out.println(johoUser);

			for (AccessKeyMetadata key : iam.listAccessKeys(new ListAccessKeysRequest().withUserName(JOHO_BACKEND_ID))
					.getAccessKeyMetadata()) {
				iam.deleteAccessKey(new DeleteAccessKeyRequest(JOHO_BACKEND_ID, key.getAccessKeyId()));
			}

			AccessKey accessKey = iam.createAccessKey(new CreateAccessKeyRequest(JOHO_BACKEND_ID)).getAccessKey();
			System.out.println("Joho Access Key Id = " + accessKey.getAccessKeyId());
			System.out.println("Joho Secret Key = " + accessKey.getSecretAccessKey());

			AmazonS3 s3 = new AmazonS3Client(davattiasCredentials, new ClientConfiguration().withGzip(true))
					.withRegion(JOHO_REGION);

			if (!s3.doesBucketExist(JOHO_BUCKET_NAME)) {
				s3.createBucket(JOHO_BUCKET_NAME);
			}

			s3.setBucketCrossOriginConfiguration(JOHO_BUCKET_NAME,
					new BucketCrossOriginConfiguration().withRules(new CORSRule().withMaxAgeSeconds(9999999)
							.withAllowedMethods(ImmutableList.of(AllowedMethods.GET))
							.withAllowedOrigins(ImmutableList.of("*"))));

			iam.attachUserPolicy(new AttachUserPolicyRequest().withUserName(JOHO_BACKEND_ID));
			iam.putUserPolicy(new PutUserPolicyRequest(JOHO_BACKEND_ID, "bucket-policy", ""));
			AccessControlList controlList = s3.getBucketAcl(JOHO_BUCKET_NAME);
			System.out.println(controlList);
			// controlList.grantPermission(new CanonicalGrantee(johoUser.),
			// Permission.Read);
			// controlList.grantPermission(new
			// CanonicalGrantee(johoUser.getUserId()), Permission.Write);
			// System.out.println(controlList);
			// s3.setBucketAcl(JOHO_BUCKET_NAME, controlList);

			// // Manually start a session.
			// // Following duration can be set only if temporary credentials
			// are
			// // requested by an IAM user.
			// GetSessionTokenRequest getSessionTokenRequest = new
			// GetSessionTokenRequest().withDurationSeconds(1000);
			//
			// Credentials sessionCredentials = new
			// AWSSecurityTokenServiceClient(new ProfileCredentialsProvider())
			// .getSessionToken(getSessionTokenRequest).getCredentials();
			//
			// System.out.println("Session Access Key Id = " +
			// sessionCredentials.getAccessKeyId());
			// System.out.println("Session Secret Access Key = " +
			// sessionCredentials.getSecretAccessKey());
			// System.out.println("Session Token = " +
			// sessionCredentials.getSessionToken());
			//
			// // Package the temporary security credentials as
			// // a BasicSessionCredentials object, for an Amazon S3 client
			// object
			// // to use.
			// BasicSessionCredentials tempCredentials = new
			// BasicSessionCredentials(sessionCredentials.getAccessKeyId(),
			// sessionCredentials.getSecretAccessKey(),
			// sessionCredentials.getSessionToken());
			//
			// s3 = new AmazonS3Client(tempCredentials);
			// s3.setRegion(Region.getRegion(JOHO_REGION));

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Done.");
	}
}
