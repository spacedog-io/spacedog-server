package io.spacedog.services.caremen;

public interface CourseStatus {

	String NEW_IMMEDIATE = "new-immediate";
	String NEW_SCHEDULED = "new-scheduled";
	String SCHEDULED_ASSIGNED = "scheduled-assigned";
	String NO_DRIVER_AVAILABLE = "no-driver-available";
	String DRIVER_IS_COMING = "driver-is-coming";
	String READY_TO_LOAD = "ready-to-load";
	String IN_PROGRESS = "in-progress";
	String CANCELLED = "cancelled";
	String COMPLETED = "completed";
}
