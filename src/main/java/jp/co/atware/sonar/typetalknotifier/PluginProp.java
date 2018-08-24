package jp.co.atware.sonar.typetalknotifier;

public enum PluginProp {
	ENABLED("ttn.enabled"),
	PROJECT("ttn.project"),
	PROJECT_KEY("projectKey"),
	TYPETALK_TOKEN("typetalkToken"),
	TYPETALK_TOPIC_ID("typetalkTopicId");

	private final String value;

	PluginProp(String value) {
		this.value = value;
	}

	public String value() {
		return this.value;
	}
}
