package io.spacedog.services;

import org.junit.runner.RunWith;

import io.spacedog.client.SpaceSuite;
import io.spacedog.client.SpaceSuite.Annotations;
import io.spacedog.client.SpaceSuite.TestOncePerDay;

@RunWith(SpaceSuite.class)
@Annotations(TestOncePerDay.class)
public class TestOncePerDaySuite {
}
