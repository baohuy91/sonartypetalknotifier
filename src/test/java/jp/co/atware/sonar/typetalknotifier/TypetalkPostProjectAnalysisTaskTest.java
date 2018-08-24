package jp.co.atware.sonar.typetalknotifier;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;

import java.util.Date;

import static jp.co.atware.sonar.typetalknotifier.PluginProp.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.*;

public class TypetalkPostProjectAnalysisTaskTest {
	@Test
	public void finished_WhenAnalyticsTaskFinished_ExpectSendTypetalkMessage() {
		final TypetalkClient typetalkClient = Mockito.mock(TypetalkClient.class);
		final MetricFinder metricFinder = Mockito.mock(MetricFinder.class);
		Mockito.when(metricFinder.findByKey(anyString()))
				.thenAnswer((InvocationOnMock invocation) -> {
					final String key = (String) invocation.getArgument(0);
					return new Metric.Builder(key, key, Metric.ValueType.PERCENT).create();
				});
		final String projIdx = "0";
		final Configuration configuration = new MapSettings()
				.setProperty("sonar.core.serverBaseURL", "http://localhost:9000")
				.setProperty(ENABLED.value(), true)
				.setProperty(PROJECT.value(), projIdx)
				.setProperty(getFieldProperty(PROJECT.value(), projIdx, PROJECT_ID.value()), "key")
				.setProperty(getFieldProperty(PROJECT.value(), projIdx, TYPETALK_TOPIC_ID.value()), "123")
				.setProperty(getFieldProperty(PROJECT.value(), projIdx, TYPETALK_TOKEN.value()), "abc")
				.asConfig();
		final TypetalkPostProjectAnalysisTask task = new TypetalkPostProjectAnalysisTask(typetalkClient,
				configuration, metricFinder);

		simpleAnalysis(task);

		final String expectMsg =
				"Project [ProjA](http://localhost:9000/dashboard?id=key) analyzed. Quality gate status: `OK`\n" +
						"> :smile: new_coverage → `80.83%`, error if < 50.0%";
		Mockito.verify(typetalkClient, times(1))
				.postMessage(eq(expectMsg), eq("123"), eq("abc"));
	}

	@Test
	public void finished_WhenPluginIsDisabled_ExpectNothingIsSent() {
		final TypetalkClient typetalkClient = Mockito.mock(TypetalkClient.class);
		final I18n i18n = Mockito.mock(I18n.class);
		final Configuration configuration = new MapSettings()
				.setProperty(ENABLED.value(), false)
				.asConfig();
		final TypetalkPostProjectAnalysisTask task = new TypetalkPostProjectAnalysisTask(typetalkClient,
				configuration, Mockito.mock(MetricFinder.class));

		simpleAnalysis(task);

		Mockito.verifyZeroInteractions(typetalkClient);
	}

	private static void simpleAnalysis(TypetalkPostProjectAnalysisTask task) {
		PostProjectAnalysisTaskTester.of(task)
				.withCeTask(newCeTaskBuilder()
						.setId("id")
						.setStatus(CeTask.Status.SUCCESS)
						.build())
				.withProject(PostProjectAnalysisTaskTester.newProjectBuilder()
						.setUuid("uuid")
						.setKey("key")
						.setName("ProjA")
						.build())
				.at(new Date())
				.withAnalysisUuid("uuid")
				.withQualityGate(newQualityGateBuilder()
						.setId("id")
						.setName("name")
						.setStatus(QualityGate.Status.OK)
						.add(newConditionBuilder()
								.setMetricKey(CoreMetrics.NEW_COVERAGE_KEY)
								.setOperator(QualityGate.Operator.LESS_THAN)
								.setErrorThreshold("50.0")
								.setOnLeakPeriod(true)
								.build(QualityGate.EvaluationStatus.OK, "80.83333"))
						.build())
				.execute();
	}

	private String getFieldProperty(String property, String idx, String field) {
		return String.format("%s.%s.%s", property, idx, field);
	}
}