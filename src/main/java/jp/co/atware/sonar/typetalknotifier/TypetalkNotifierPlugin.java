package jp.co.atware.sonar.typetalknotifier;

import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.resources.Qualifiers;

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
				.onQualifiers(Qualifiers.PROJECT)
				.category(CATEGORY)
				.subCategory(SUBCATEGORY)
				.index(0)
				.build());

		// Can not get project's configuration with Qualifiers.PROJECT. Not sure why?
		extensions.add(PropertyDefinition.builder(PluginProp.PROJECT.value())
				.name("Per project configuration")
				.description("Add each project for each typetalk topic.")
				.category(CATEGORY)
				.subCategory(SUBCATEGORY)
				.index(1)
				.fields(
						PropertyFieldDefinition.build(PluginProp.PROJECT_KEY.value())
								.name("Project key")
								.description("The key of the project")
								.build(),
						PropertyFieldDefinition.build(PluginProp.TYPETALK_TOPIC_ID.value())
								.name("Typetalk topic Id")
								.description("The topic which result message will be sent to. e.g. 12345")
								.build(),
						PropertyFieldDefinition.build(PluginProp.TYPETALK_TOKEN.value())
								.type(PropertyType.PASSWORD)
								.name("Typetalk token")
								.description("Authentication token of typetalk, could be bot's client token")
								.build()
				)
				.build());

		// Actual extension
		extensions.add(TypetalkPostProjectAnalysisTask.class);

		context.addExtensions(extensions);
	}
}
