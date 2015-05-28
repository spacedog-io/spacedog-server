package com.magiclabs.restapi;

import com.google.common.base.Strings;

public class Account {

	@SuppressWarnings("serial")
	public static class InvalidAccountException extends RuntimeException {
		public InvalidAccountException(String message) {
			super(message);
		}
	}

	public String id;

	public void checkUserInputValidity() {
		if (Strings.isNullOrEmpty(id))
			throw new InvalidAccountException(
					"account id must not be null or empty");

		if (id.indexOf('-') != -1)
			throw new InvalidAccountException(
					"account id must not cointain any '-' character");

		if (id.indexOf('/') != -1)
			throw new InvalidAccountException(
					"account id must not cointain any '/' character");
	}
}
