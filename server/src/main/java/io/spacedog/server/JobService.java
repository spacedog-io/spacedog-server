/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.index.IndexResponse;
import org.joda.time.DateTime;

import com.google.common.base.Throwables;

import io.spacedog.client.job.SpaceJob;
import io.spacedog.client.job.SpaceTask;
import io.spacedog.client.job.SpaceTask.Status;
import io.spacedog.jobs.Internals;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/1/jobs")
public class JobService extends SpaceService {

	private static final int DEFAULT_TIMEOUT_MILLIS = 1000 * 60 * 4;

	private ScheduledThreadPoolExecutor executor;

	//
	// Routes
	//

	@Get("/:name")
	@Get("/:name/")
	public Payload getJob(String name) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		return SettingsService.get()//
				.get(SpaceJob.internalSettingsId(name));
	}

	@Put("/:name")
	@Put("/:name/")
	public Payload putJob(String name, SpaceJob job) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		IndexResponse response = SettingsService.get().saveAsObject(job);
		deleteNextScheduledTaskOf(name);
		scheduleNextTaskOf(job);
		return ElasticPayload.saved("/1", response).build();
	}

	@Delete("/:name")
	@Delete("/:name/")
	public Payload deleteJob(String name) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		boolean deleted = SettingsService.get()//
				.doDelete(SpaceJob.internalSettingsId(name));
		deleteNextScheduledTaskOf(name);
		return JsonPayload.ok().withFields("deleted", deleted).build();
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
	// Job Manager
	//

	public void manageTasks() {
		try {
			List<SpaceTask> tasks = getNextjobTasks(3);
			for (SpaceTask task : tasks) {
				taskIsReady(task);

				long delay = task.delay();
				Runnable executeCommand = () -> JobService.this.execute(task);
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

	//
	// Singleton
	//

	private static JobService singleton = new JobService();

	public static JobService get() {
		return singleton;
	}

	private JobService() {
		executor = new ScheduledThreadPoolExecutor(1);
		executor.setMaximumPoolSize(10);
	}

}