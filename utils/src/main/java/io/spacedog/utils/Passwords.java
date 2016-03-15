/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.xml.bind.DatatypeConverter;

import com.google.common.base.Strings;

public class Passwords {

	public static String checkAndHash(String password) {
		checkIfValid(password);
		return hash(password);
	}

	/**
	 * ******* README ******* For now, I use hard coded salt and iteration
	 * number (1000). If I decide to change this, I have to maintain this algo
	 * and har coded values until all password hashed this way are hashed anoter
	 * way. Salt and iterations could be saved in datastore close to the hashed
	 * password.
	 */
	public static String hash(String password) {
		try {
			PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), "hjyuetcslhhjgl".getBytes(), 1000, 64 * 8);
			SecretKeyFactory skf;
			skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			return DatatypeConverter.printHexBinary(skf.generateSecret(spec).getEncoded());
		} catch (Throwable t) {
			throw new RuntimeException("failed to hash password", t);
		}
	}

	public static void checkIfValid(String password) {
		if (Strings.isNullOrEmpty(password))
			throw new IllegalArgumentException("password is null or empty");
		if (password.length() < 6)
			throw new IllegalArgumentException("password must be at least 6 characters long");
	}

	public static boolean isValid(String password) {
		try {
			checkIfValid(password);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
