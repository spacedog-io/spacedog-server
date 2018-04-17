package io.spacedog.utils;

import java.util.Random;

import org.joda.time.DateTime;

import io.spacedog.client.schema.GeoPoint;

public class RandomUtils {

	private static Random random = new Random();

	public static String nextAddress() {
		return "52 rue Michel Ange, 75016 Paris, France";
	}

	public static String nextKeyword() {
		return "KEY" + nextLong();
	}

	public static boolean nextBoolean() {
		return random.nextBoolean();
	}

	public static int nextInt() {
		return random.nextInt();
	}

	public static long nextLong() {
		return random.nextLong();
	}

	public static float nextFloat() {
		return random.nextFloat();
	}

	public static double nextDouble() {
		return random.nextDouble();
	}

	public static DateTime nextDateTime() {
		long l = nextLong();
		return l > 0 ? DateTime.now().plus(l)//
				: DateTime.now().minus(l);
	}

	public static GeoPoint nextGeoPoint() {
		return new GeoPoint(48 + random.nextDouble(), 2 + random.nextDouble());
	}
}