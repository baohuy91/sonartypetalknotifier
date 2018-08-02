package jp.co.atware.sonar.typetalknotifier;

import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

import java.util.ArrayList;

public class TypetalkNotifierPlugin implements Plugin {

    private static final String CATEGORY = "Typetalk";
    private static final String SUBCATEGORY = "Typetalk Notifier";

    @Override
    public void define(Context context) {
        final ArrayList<Object> extensions = new ArrayList<>();

        extensions.add(PropertyDefinition.builder(PluginProp.ENABLED.value())
                .name("Plugin enabled")
                .description("Should this plugin be enabled")
                .defaultValue("false")
                .type(PropertyType.BOOLEAN)
                .category(CATEGORY)
                .subCategory(SUBCATEGORY)
                .index(0)
                .build());

        extensions.add(PropertyDefinition.builder(PluginProp.TYPETALK_TOPIC_ID.value())
                .name("Typetalk topic Id")
                .description("The topic which result message will be sent to. e.g. 12345")
                .type(PropertyType.STRING)
                .category(CATEGORY)
                .subCategory(SUBCATEGORY)
                .index(1)
                .build());

        extensions.add(PropertyDefinition.builder(PluginProp.TYPETALK_TOKEN.value())
                .name("Typetalk token")
                .description("Authentication token of typetalk, could be bot's client token")
                .type(PropertyType.PASSWORD)
                .category(CATEGORY)
                .subCategory(SUBCATEGORY)
                .index(2)
                .build());

        // Actual extension
        extensions.add(TypetalkPostProjectAnalysisTask.class);

        context.addExtensions(extensions);
    }
}
