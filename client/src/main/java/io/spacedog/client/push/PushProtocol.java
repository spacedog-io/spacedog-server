package io.spacedog.client.push;

public enum PushProtocol {
	APNS, // Apple Push Notification Service
	APNS_SANDBOX, // Sandbox version of APNS
	ADM, // Amazon Device Messaging
	GCM, // Google Cloud Messaging
	BAIDU, // Baidu CloudMessaging Service
	WNS, // Windows Notification Service
	MPNS; // Microsoft Push Notification Service
}