package jp.co.atware.sonar.typetalknotifier;

import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.QualityGate.Condition;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.util.Arrays;
import java.util.Optional;

import static jp.co.atware.sonar.typetalknotifier.PluginProp.*;

public class TypetalkPostProjectAnalysisTask implements PostProjectAnalysisTask {
	private static final Logger LOG = Loggers.get(TypetalkPostProjectAnalysisTask.class);
	private static final String TYPETALK_URL = "https://typetalk.com/api/v1";

	private final TypetalkClient typetalkClient;
	private final Configuration configuration;
	// I18n can only get name of Core metrics. batch.MetricFinder is only inject in Scanner side.
	private final MetricFinder metricFinder;

	public TypetalkPostProjectAnalysisTask(Configuration configuration, MetricFinder metricFinder) {
		this(new TypetalkClient(TYPETALK_URL), configuration, metricFinder);
	}

	public TypetalkPostProjectAnalysisTask(
			TypetalkClient typetalkClient, Configuration configuration, MetricFinder metricFinder) {
		this.typetalkClient = typetalkClient;
		this.configuration = configuration;
		this.metricFinder = metricFinder;
	}

	@Override
	public void finished(ProjectAnalysis analysis) {
		final String projKey = analysis.getProject().getKey();
		final Optional<Boolean> enabledOpt = configuration.getBoolean(ENABLED.value());
		if (!enabledOpt.isPresent()) {
			LOG.error("Plugin param {} missing", ENABLED.value());
			return;
		}
		LOG.info("Typetalk plugin enabled: {}", enabledOpt.get());
		if (!enabledOpt.get()) {
			return;
		}

		final String msg = createMessage(analysis);
		final String[] projConfIdxes = configuration.getStringArray(PROJECT.value());
		final Optional<String> projConfIdx = Arrays.stream(projConfIdxes).filter(idx ->
				configuration.get(getFieldProperty(PROJECT.value(), idx, PROJECT_KEY.value()))
						.map(projKey::equals)
						.orElse(false)
		).findAny();
		if (!projConfIdx.isPresent()) {
			LOG.info("No project id match project key: {}", projKey);
			return;
		}
		final String projIdx = projConfIdx.get();

		final String ttTopicField = getFieldProperty(PROJECT.value(), projIdx, TYPETALK_TOPIC_ID.value());
		final String typetalkTopicId = configuration.get(ttTopicField)
				.orElseThrow(() -> new RuntimeException("Missing " + TYPETALK_TOPIC_ID.value()));

		final String ttTokenField = getFieldProperty(PROJECT.value(), projIdx, TYPETALK_TOKEN.value());
		final String typetalkToken = configuration.get(ttTokenField)
				.orElseThrow(() -> new RuntimeException("Missing " + TYPETALK_TOKEN.value()));

		LOG.info("TypetalkTopicId: {}", typetalkTopicId);
		typetalkClient.postMessage(msg, typetalkTopicId, typetalkToken);
	}

	private String getFieldProperty(String property, String idx, String field) {
		return String.format("%s.%s.%s", property, idx, field);
	}

	private String createMessage(ProjectAnalysis analysis) {
		final QualityGate qualityGate = analysis.getQualityGate();
		final String projectName = analysis.getProject().getName();
		String overallMsg = String.format("Project [%s](%s) analyzed.", projectName, getProjectUrl(analysis));
		if (qualityGate == null) {
			return overallMsg;
		}

		overallMsg += String.format(" Quality gate status: `%s`%n", qualityGate.getStatus().toString());

		return overallMsg + qualityGate.getConditions().stream()
				.map(this::makeConditionMessage)
				.reduce((str, m) -> String.join("\n", str, m))
				.orElse("No conditions");
	}

	private String getProjectUrl(ProjectAnalysis analysis) {
		return getSonarServerUrl() + "dashboard?id=" + analysis.getProject().getKey();
	}

	private String makeConditionMessage(Condition condition) {
		String conditionName = condition.getMetricKey();
		final Metric metric = metricFinder.findByKey(condition.getMetricKey());
		if (metric != null) {
			conditionName = metric.getName();
		}

		return String.format("> %s %s → `%s`%s", emoji(condition), conditionName, getConditionValue(condition),
				getThresholdDesc(condition));
	}

	private String getThresholdDesc(Condition condition) {
		StringBuilder sb = new StringBuilder();
		if (condition.getWarningThreshold() != null) {
			sb.append(", warning if ")
					.append(getOperator(condition))
					.append(" ")
					.append(condition.getWarningThreshold());
			if (isPercentageMetric(condition)) {
				sb.append("%");
			}
		}
		if (condition.getErrorThreshold() != null) {
			sb.append(", error if ")
					.append(getOperator(condition))
					.append(" ")
					.append(condition.getErrorThreshold());
			if (isPercentageMetric(condition)) {
				sb.append("%");
			}
		}

		return sb.toString();
	}

	private String getConditionValue(Condition condition) {
		if (QualityGate.EvaluationStatus.NO_VALUE.equals(condition.getStatus())) {
			return condition.getStatus().name();
		}

		if (isPercentageMetric(condition)) {
			return String.format("%.2f%%", Double.parseDouble(condition.getValue()));
		} else {
			return condition.getValue();
		}
	}

	private String emoji(Condition condition) {
		switch (condition.getStatus()) {
			case NO_VALUE:
				return ":thinking_face:";
			case OK:
				return ":smile:";
			case WARN:
				return ":nauseated_face:";
			case ERROR:
				return ":rage:";
			default:
				return "";
		}
	}

	private String getOperator(Condition condition) {
		switch (condition.getOperator()) {
			case EQUALS:
				return "==";
			case LESS_THAN:
				return "<";
			case GREATER_THAN:
				return ">";
			case NOT_EQUALS:
				return "!=";
			default:
				return "";
		}
	}

	private boolean isPercentageMetric(Condition condition) {
		switch (condition.getMetricKey()) {
			case CoreMetrics.NEW_COVERAGE_KEY:
			case CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY:
			case CoreMetrics.COVERAGE_KEY:
			case CoreMetrics.NEW_DUPLICATED_LINES_KEY:
				return true;

			default:
				return false;
		}
	}

	private String getSonarServerUrl() {
		// TODO: inject server to get url instead
		return configuration.get("sonar.core.serverBaseURL")
				.map(u -> u.endsWith("/") ? u : u + "/")
				.orElse(null);
	}
}
