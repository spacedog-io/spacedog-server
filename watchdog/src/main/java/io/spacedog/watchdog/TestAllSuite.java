package io.spacedog.watchdog;

import org.junit.runner.RunWith;

import io.spacedog.watchdog.SpaceSuite.Annotations;
import io.spacedog.watchdog.SpaceSuite.TestOften;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@RunWith(SpaceSuite.class)
@Annotations({ TestOften.class, TestOncePerDay.class })
public class TestAllSuite {
}
