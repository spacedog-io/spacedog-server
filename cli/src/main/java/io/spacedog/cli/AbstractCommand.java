package io.spacedog.cli;

import com.beust.jcommander.Parameter;

public abstract class AbstractCommand<T extends AbstractCommand<T>> {

	@Parameter(names = { "-v", "--verbose" }, //
			required = false, //
			description = "output all requests debug traces")
	private boolean verbose;

	public AbstractCommand() {
	}

	public boolean verbose() {
		return this.verbose;
	}

	@SuppressWarnings("unchecked")
	public T verbose(boolean verbose) {
		this.verbose = verbose;
		return (T) this;
	}
}
