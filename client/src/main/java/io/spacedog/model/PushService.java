package io.spacedog.model;

public enum PushService {
	APNS, // Apple Push Notification Service
	APNS_SANDBOX, // Sandbox version of APNS
	ADM, // Amazon Device Messaging
	GCM, // Google Cloud Messaging
	BAIDU, // Baidu CloudMessaging Service
	WNS, // Windows Notification Service
	MPNS; // Microsoft Push Notification Service
}