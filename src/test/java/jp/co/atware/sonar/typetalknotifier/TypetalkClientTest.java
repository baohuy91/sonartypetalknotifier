package jp.co.atware.sonar.typetalknotifier;

import jp.co.atware.sonar.typetalknotifier.model.PostMessageRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.internal.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class TypetalkClientTest {
    private MockWebServer server;
    private TypetalkClient sut;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        sut = new TypetalkClient(server.url("/api").toString());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void postMessage_WhenHaveMessage_ExpectHttpCall() throws Exception {
        final MockResponse mockResponse = new MockResponse()
                .setResponseCode(200);
        server.enqueue(mockResponse);

        sut.postMessage("hi", "topic123", "FAKE_TOKEN");

        final RecordedRequest request = server.takeRequest(100, TimeUnit.MILLISECONDS);
        final Gson gson = new Gson();
        final PostMessageRequest reqModel = gson.fromJson(request.getBody().readUtf8(), PostMessageRequest.class);

        assertEquals("FAKE_TOKEN", request.getHeader("X-Typetalk-Token"));
        assertEquals("/api/topics/topic123", request.getPath());
        assertEquals("hi", reqModel.message);
    }
}