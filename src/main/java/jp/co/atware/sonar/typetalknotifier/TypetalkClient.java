package jp.co.atware.sonar.typetalknotifier;

import com.google.gson.Gson;
import jp.co.atware.sonar.typetalknotifier.model.PostMessageRequest;
import okhttp3.*;

import java.io.IOException;
import java.io.UncheckedIOException;

public class TypetalkClient {
    private final String baseUrl;

    public TypetalkClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    void postMessage(String msg, String topicId, String typetalkToken) {
        final OkHttpClient httpClient = new OkHttpClient();
        final Gson gson = new Gson();

        final PostMessageRequest req = new PostMessageRequest();
        req.message = msg;
        final RequestBody body = RequestBody.create(MediaType.get("application/json"), gson.toJson(req));

        final Request request = new Request.Builder()
                .header("X-Typetalk-Token", typetalkToken)
                .url(baseUrl + "/topics/" + topicId)
                .post(body)
                .build();

        try {
            final Response response = httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
