/**
 * © David Attias 2015
 */
package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FileSettings extends SettingsBase {

	public long sizeLimitInKB = 20000; // 20MB
}
