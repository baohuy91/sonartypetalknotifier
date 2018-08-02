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

import java.util.Date;
import java.util.Locale;

import static jp.co.atware.sonar.typetalknotifier.PluginProp.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.*;

public class TypetalkPostProjectAnalysisTaskTest {
    @Test
    public void finished_WhenAnalyticsTaskFinished_ExpectSendTypetalkMessage() {
        final TypetalkClient typetalkClient = Mockito.mock(TypetalkClient.class);
        final I18n i18n = Mockito.mock(I18n.class);
        Mockito.when(i18n.message(any(Locale.class), anyString(), anyString()))
                .thenAnswer((InvocationOnMock invocation) -> (String) invocation.getArguments()[2]);
        final Configuration configuration = new MapSettings()
                .setProperty("sonar.core.serverBaseURL", "http://localhost:9000")
                .setProperty(ENABLED.value(), true)
                .setProperty(TYPETALK_TOPIC_ID.value(), "123")
                .setProperty(TYPETALK_TOKEN.value(), "abc")
                .asConfig();
        final TypetalkPostProjectAnalysisTask task = new TypetalkPostProjectAnalysisTask(typetalkClient,
                configuration, i18n);

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
                configuration, i18n);

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
}