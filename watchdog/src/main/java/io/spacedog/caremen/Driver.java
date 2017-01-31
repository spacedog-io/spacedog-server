package io.spacedog.caremen;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.sdk.DataObject;

//ignore unknown fields
@JsonIgnoreProperties(ignoreUnknown = true)
// only map to private fields
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
getterVisibility = Visibility.NONE, //
isGetterVisibility = Visibility.NONE, //
setterVisibility = Visibility.NONE)
public class Driver extends DataObject {

	public String status;
	public String credentialsId;
	public String firstname;
	public String lastname;
	public String homeAddress;
	public String companyName;
	public String siret;
	public RIB rib;
}
