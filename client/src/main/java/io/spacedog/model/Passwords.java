/**
 * © David Attias 2015
 */
package io.spacedog.model;

import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.xml.bind.DatatypeConverter;

import io.spacedog.utils.Check;
import io.spacedog.utils.Optional7;

public class Passwords {

	public static final String PASSWORD_DEFAULT_REGEX = ".{6,}";

	private static final Random random = new Random();

	public static String checkAndHash(String password) {
		return checkAndHash(password, PASSWORD_DEFAULT_REGEX);
	}

	public static String checkAndHash(String password, String regex) {
		check(password, Optional7.of(regex));
		return hash(password);
	}

	public static void check(String password) {
		Optional7<String> none = Optional7.empty();
		check(password, none);
	}

	public static void check(String password, Optional7<String> regex) {
		Check.matchRegex(regex.orElse(PASSWORD_DEFAULT_REGEX), password, "password");
	}

	public static String random() {
		return Long.toString(random.nextLong());
	}

	/**
	 * ******* README ******* For now, I use hard coded salt and iteration number
	 * (1000). If I decide to change this, I have to maintain this algo and hard
	 * coded values until all password hashed this way are hashed another way. Salt
	 * and iterations could be saved in datastore close to the hashed password.
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

}
