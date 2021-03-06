package ru.roborox.itunesconnect.api.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import ru.roborox.itunesconnect.api.analytics.model.AuthServiceConfig;
import ru.roborox.itunesconnect.api.analytics.model.SigninRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static ru.roborox.itunesconnect.api.common.Utils.createExecutor;

public class ItunesConnectLoginApi {
    public static final String SIGNIN_PATH = "/auth/signin";
    public static final String APP_CONFIG_PATH = "/app/config";
    public static final String SESSION_PATH = "/session";

    public static final ObjectMapper objectMapper = new ObjectMapper();

    private final String itunesConnectHostname;
    private final String olympusUrl;

    public ItunesConnectLoginApi(String itunesConnectHostname, String olympusUrl) {
        this.itunesConnectHostname = itunesConnectHostname;
        this.olympusUrl = olympusUrl;
    }

    public ConnectTokens login(String login, String password) throws IOException, URISyntaxException {
        final TokensCookieStore cookieStore = new TokensCookieStore();
        final Executor executor = createExecutor(cookieStore);
        connect(executor, login, password);
        final Optional<ConnectTokens> result = cookieStore.getTokens();
        if (result.isPresent()) {
            return result.get();
        } else {
            throw new IOException("Unable to login: needed cookies not found");
        }
    }

    private void connect(Executor executor, String login, String password) throws IOException, URISyntaxException {
        final AuthServiceConfig config = getConfig(executor);
        signin(executor, config.getAuthServiceUrl(), config.getAuthServiceKey(), login, password);
        session(executor);
    }

    private void signin(Executor executor, String authServiceUrl, String authServiceKey, String login, String password) throws IOException {
        final SigninRequest request = new SigninRequest(login, password, false);
        executor.execute(Request.Post(authServiceUrl + SIGNIN_PATH).bodyString(objectMapper.writeValueAsString(request), ContentType.APPLICATION_JSON).addHeader("X-Apple-Widget-Key", authServiceKey));
    }

    private void session(Executor executor) throws IOException {
        executor.execute(Request.Get(olympusUrl + SESSION_PATH));
    }

    private AuthServiceConfig getConfig(Executor executor) throws URISyntaxException, IOException {
        final URI configUrl = new URIBuilder(olympusUrl + APP_CONFIG_PATH).addParameter("hostname", itunesConnectHostname).build();
        return objectMapper.readValue(executor.execute(Request.Get(configUrl)).returnContent().asString(), AuthServiceConfig.class);
    }
}
