package io.spacedog.watchdog;

import org.junit.runner.RunWith;
import org.junit.runner.notification.RunListener;

import io.spacedog.watchdog.SpaceSuite.Annotations;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@RunWith(SpaceSuite.class)
@Annotations(TestOften.class)
public class TestOftenSuite extends RunListener {
}
