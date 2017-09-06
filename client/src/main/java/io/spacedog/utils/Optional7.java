package io.spacedog.utils;

import java.util.NoSuchElementException;
import java.util.Objects;

public final class Optional7<T> {

	private static final Optional7<?> EMPTY = new Optional7<>();

	private final T value;

	private Optional7() {
		this.value = null;
	}

	public static <T> Optional7<T> empty() {
		@SuppressWarnings("unchecked")
		Optional7<T> t = (Optional7<T>) EMPTY;
		return t;
	}

	private Optional7(T value) {
		this.value = Objects.requireNonNull(value);
	}

	public static <T> Optional7<T> of(T value) {
		return new Optional7<>(value);
	}

	@SuppressWarnings("unchecked")
	public static <T> Optional7<T> ofNullable(T value) {
		return (Optional7<T>) (value == null ? empty() : of(value));
	}

	public T get() {
		if (value == null) {
			throw new NoSuchElementException("No value present");
		}
		return value;
	}

	public boolean isPresent() {
		return value != null;
	}

	public T orElse(T other) {
		return value != null ? value : other;
	}

	public <X extends Throwable> T orElseThrow(X exception) throws X {
		if (value != null) {
			return value;
		} else {
			throw exception;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Optional7)) {
			return false;
		}

		Optional7<?> other = (Optional7<?>) obj;
		return Objects.equals(value, other.value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public String toString() {
		return value != null ? String.format("Optional[%s]", value) : "Optional.empty";
	}
}
