package io.spacedog.client.snapshot;

import java.util.Set;

import org.joda.time.DateTime;

public class SpaceSnapshot implements Comparable<SpaceSnapshot> {

	public String id;
	public String repositoryId;
	public String backendId;
	public String state;
	public DateTime startTime;
	public DateTime endTime;
	public boolean restorable;
	public boolean completed;
	public Set<String> indices;

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof SpaceSnapshot))
			return false;
		SpaceSnapshot snap = (SpaceSnapshot) obj;
		return id.equals(snap.id);
	}

	@Override
	// ordered from latest to oldest snapshots
	public int compareTo(SpaceSnapshot obj) {
		return obj.startTime.compareTo(this.startTime);
	}

	@Override
	public String toString() {
		return id;
	}
}