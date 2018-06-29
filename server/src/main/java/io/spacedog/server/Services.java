package io.spacedog.server;

import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.services.BatchService;
import io.spacedog.services.EmailService;
import io.spacedog.services.JobService;
import io.spacedog.services.SettingsService;
import io.spacedog.services.SmsService;
import io.spacedog.services.StripeService;
import io.spacedog.services.credentials.CredentialsService;
import io.spacedog.services.data.DataService;
import io.spacedog.services.data.SchemaService;
import io.spacedog.services.file.FileService;
import io.spacedog.services.log.LogService;
import io.spacedog.services.push.PushService;

public class Services implements SpaceFields, SpaceParams {

	private static DataService dataService;

	public static DataService data() {
		if (dataService == null)
			dataService = new DataService();
		return dataService;
	}

	private static SettingsService settingsService;

	public static SettingsService settings() {
		if (settingsService == null)
			settingsService = new SettingsService();
		return settingsService;
	}

	private static StripeService stripeService;

	public static StripeService stripe() {
		if (stripeService == null)
			stripeService = new StripeService();
		return stripeService;
	}

	private static PushService pushService;

	public static PushService push() {
		if (pushService == null)
			pushService = new PushService();
		return pushService;
	}

	private static CredentialsService credentialsService;

	public static CredentialsService credentials() {
		if (credentialsService == null)
			credentialsService = new CredentialsService();
		return credentialsService;
	}

	private static SchemaService schemaService;

	public static SchemaService schemas() {
		if (schemaService == null)
			schemaService = new SchemaService();
		return schemaService;
	}

	private static EmailService emailService;

	public static EmailService emails() {
		if (emailService == null)
			emailService = new EmailService();
		return emailService;
	}

	private static FileService fileService;

	public static FileService files() {
		if (fileService == null)
			fileService = new FileService();
		return fileService;
	}

	private static LogService logService;

	public static LogService logs() {
		if (logService == null)
			logService = new LogService();
		return logService;
	}

	private static SmsService smsService;

	public static SmsService sms() {
		if (smsService == null)
			smsService = new SmsService();
		return smsService;
	}

	private static BatchService batchService;

	public static BatchService batch() {
		if (batchService == null)
			batchService = new BatchService();
		return batchService;
	}

	private static JobService jobService;

	public static JobService jobs() {
		if (jobService == null)
			jobService = new JobService();
		return jobService;
	}
}
