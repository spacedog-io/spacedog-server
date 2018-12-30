package io.spacedog.services.job;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;

import io.spacedog.client.job.SpaceJob;
import io.spacedog.client.job.SpaceTask;
import io.spacedog.client.job.SpaceTask.Status;
import io.spacedog.jobs.Internals;
import io.spacedog.server.Services;
import io.spacedog.utils.Exceptions;

public class JobService2 {

	public SpaceJob get(String jobName) {
		return Services.settings().get(//
				SpaceJob.internalSettingsId(jobName), //
				SpaceJob.class)//
				.orElseThrow(() -> Exceptions.objectNotFound("job", jobName));
	}

	public void save(SpaceJob job) {
		Services.settings().save(job);
		deleteNextScheduledTaskOf(job.name);
		scheduleNextTaskOf(job);
	}

	public void delete(String jobName) {
		Services.settings().delete(//
				SpaceJob.internalSettingsId(jobName));
		deleteNextScheduledTaskOf(jobName);
	}

	public ObjectNode execute(String jobName, Object jobRequest) {
		throw new UnsupportedOperationException();
	}

	public void deleteExecution(String jobName, String jobExecId) {
		throw new UnsupportedOperationException();
	}

	//
	// Interface
	//

	public ScheduledFuture<?> schedule(Runnable command, //
			long initialDelay, long period, TimeUnit unit) {
		ScheduledFuture<?> future = executor.scheduleAtFixedRate(command, initialDelay, period, unit);
		return future;
	}

	//
	//
	//

	private static final int DEFAULT_TIMEOUT_MILLIS = 1000 * 60 * 4;

	private ScheduledThreadPoolExecutor executor;

	//
	// Job Manager
	//

	public void manageTasks() {
		try {
			List<SpaceTask> tasks = getNextjobTasks(3);
			for (SpaceTask task : tasks) {
				taskIsReady(task);

				long delay = task.delay();
				Runnable executeCommand = () -> JobService2.this.execute(task);
				ScheduledFuture<?> future = executor.schedule(executeCommand, delay, TimeUnit.MILLISECONDS);

				long timeout = delay + DEFAULT_TIMEOUT_MILLIS;
				Runnable completeCommand = () -> completeExecution(task.id(), future);
				executor.schedule(completeCommand, timeout, TimeUnit.MILLISECONDS);
			}

		} catch (Throwable t) {
			alertSuperdogs(t, "error managing job tasks");
		}
	}

	private void completeExecution(String taskId, ScheduledFuture<?> future) {
		try {
			SpaceTask task = load(taskId);
			if (Status.in_progress.equals(task.status)) {
				future.cancel(true);
				taskIsCancelled(task);
				alertSuperdogs("job execution [%s] timed out", taskId);
			}
		} catch (Throwable t) {
			alertSuperdogs(t, "job execution [%s] failed when completing", taskId);
		}
	}

	private void alertSuperdogs(String message, Object... args) {
		message = String.format(message, args);
		Internals.get().notify(message, message);
	}

	private void alertSuperdogs(Throwable t, String title, Object... args) {
		title = String.format(title, args);
		String message = Throwables.getStackTraceAsString(t);
		Internals.get().notify(title, message);
	}

	private SpaceTask load(String taskId) {
		// TODO Auto-generated method stub
		return null;
	}

	private void save(SpaceTask task) {
		// TODO Auto-generated method stub

	}

	private void execute(SpaceTask task) {
		taskInProgress(task);
		doExecute(task);
		taskCompleted(task);
	}

	private void taskIsReady(SpaceTask task) {
		task.status = Status.ready;
		save(task);
	}

	private void taskCompleted(SpaceTask task) {
		task.status = Status.completed;
		task.stopped = DateTime.now();
		save(task);
	}

	private void taskInProgress(SpaceTask task) {
		task.status = Status.in_progress;
		task.started = DateTime.now();
		save(task);
	}

	private void taskIsCancelled(SpaceTask task) {
		task.stopped = DateTime.now();
		task.status = SpaceTask.Status.cancelled;
		save(task);
	}

	private void doExecute(SpaceTask task) {
		// Server.get().executeRequest(request, response);
	}

	private void scheduleNextTaskOf(SpaceJob job) {
		// TODO Auto-generated method stub

	}

	public List<SpaceTask> getNextjobTasks(int size) {
		// TODO Auto-generated method stub
		return null;
	}

	private void deleteNextScheduledTaskOf(String jobName) {
		// TODO Auto-generated method stub

	}

	public JobService2() {
		executor = new ScheduledThreadPoolExecutor(1);
		executor.setMaximumPoolSize(10);
	}

}
