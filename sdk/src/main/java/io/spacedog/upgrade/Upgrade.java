package io.spacedog.upgrade;

public interface Upgrade<T extends UpgradeEnv> {

	String from();

	String to();

	void upgrade(T env);
}
