/**
 * © David Attias 2015
 */
package io.spacedog.model;

public class InternalDataAclSettings extends ObjectRolePermissions implements Settings {

	private static final long serialVersionUID = -4957218125669892886L;

	public static final String ID = "internaldataacl";

	@Override
	public String id() {
		return ID;
	}
}
