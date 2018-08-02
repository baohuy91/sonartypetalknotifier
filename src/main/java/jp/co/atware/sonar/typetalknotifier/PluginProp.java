package jp.co.atware.sonar.typetalknotifier;

public enum PluginProp {
    ENABLED("ttn.enabled"),
    TYPETALK_TOKEN("ttn.typetalkToken"),
    TYPETALK_TOPIC_ID("ttn.typetalkTopicId");

    private final String value;

    PluginProp(String value) {
        this.value = value;
    }

    public String value() {
       return this.value;
    }
}
