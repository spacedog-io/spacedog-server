package io.spacedog.client.credentials;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class SetPasswordRequest {

	private String password;
	private String passwordResetCode;

	public String passwordResetCode() {
		return passwordResetCode;
	}

	public SetPasswordRequest withPasswordResetCode(String passwordResetCode) {
		this.passwordResetCode = passwordResetCode;
		return this;
	}

	public String password() {
		return password;
	}

	public SetPasswordRequest withPassword(String password) {
		this.password = password;
		return this;
	}
}
