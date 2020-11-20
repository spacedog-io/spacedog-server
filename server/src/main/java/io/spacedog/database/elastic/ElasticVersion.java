/**
 * © David Attias 2020
 */
package io.spacedog.database.elastic;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Utils;

public class ElasticVersion {
	public long seqNo;
	public long primaryTerm;

	private ElasticVersion(long seqNo, long primaryTerm) {
		this.seqNo = seqNo;
		this.primaryTerm = primaryTerm;
	}

	private ElasticVersion(String version) {
		String[] strings = Utils.split(version, ":");
		if (strings.length != 2)
			throw Exceptions.illegalArgument("invalid elasticsearch version [%s]", version);
		this.seqNo = Long.valueOf(strings[0]);
		this.primaryTerm = Long.valueOf(strings[1]);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ElasticVersion) {
			ElasticVersion v = (ElasticVersion) obj;
			return this.seqNo == v.seqNo && this.primaryTerm == v.primaryTerm;
		}
		return false;
	}

	@Override
	public String toString() {
		return toString(seqNo, primaryTerm);
	}

	public static ElasticVersion valueOf(String string) {
		return new ElasticVersion(string);
	}

	public static String toString(long seqNo, long primaryTerm) {
		return seqNo < 0 ? null : seqNo + ":" + primaryTerm;
	}
}