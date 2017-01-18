package io.spacedog.watchdog;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import io.spacedog.utils.Exceptions;

public class SpaceSuite extends Suite {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface TestOften {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface TestOncePerDay {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Annotations {
		public Class<? extends Annotation>[] value();
	}

	public SpaceSuite(Class<?> suiteClass, RunnerBuilder builder) throws InitializationError {
		super(suiteClass, findTestClasses(suiteClass));
	}

	@Override
	public void run(RunNotifier notifier) {
		super.run(notifier);
	}

	private static Class<?>[] findTestClasses(Class<?> suiteClass) {

		Class<? extends Annotation>[] annotations = checkSuiteAnnotations(suiteClass);

		try {
			List<?> classes = ClassPath.from(suiteClass.getClassLoader())//
					.getTopLevelClasses(suiteClass.getPackage().getName())//
					.stream()//
					.map(SpaceSuite::toClass)//
					.filter(testClass -> isAnnotationPresent(testClass, annotations))//
					.collect(Collectors.toList());

			return classes.toArray(new Class<?>[classes.size()]);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	private static Class<? extends Annotation>[] checkSuiteAnnotations(Class<?> suiteClass) {
		Annotations annotations = suiteClass.getAnnotation(Annotations.class);

		if (annotations == null)
			throw Exceptions.illegalArgument(//
					"Suites running with [%s] require an annotation of type [%s]", //
					SpaceSuite.class.getName(), Annotations.class.getName());

		if (annotations.value().length == 0)
			throw Exceptions.illegalArgument(//
					"Annotation of type [%s] requires at least one value", //
					Annotations.class.getName());

		return annotations.value();
	}

	private static boolean isAnnotationPresent(Class<?> testClass, Class<? extends Annotation>[] annotations) {
		return Arrays.stream(annotations)//
				.filter(annotation -> testClass.isAnnotationPresent(annotation))//
				.findAny()//
				.isPresent();
	}

	private static Class<?> toClass(ClassInfo info) {
		try {
			return Class.forName(info.getName(), false, //
					Thread.currentThread().getContextClassLoader());
		} catch (ClassNotFoundException e) {
			throw Exceptions.runtime(e);
		}
	}
}
