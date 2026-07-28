package com.narrativeplatform.shared.integrations;

import com.narrativeplatform.configuration.AppProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class ResendClient {
    private final AppProperties.Resend properties;
    private final RestClient restClient;

    public ResendClient(final AppProperties appProperties, final RestClient.Builder builder) {
        this.properties = appProperties.resend();
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    public boolean sendInvite(final String to, final String partyName, final String invitedBy, final String inviteUrl) {
        if (!properties.configured() || to == null || to.isBlank()) {
            return false;
        }
        final var html = """
                <div style=\"font-family:serif;color:#102b52\">
                  <h1>You were invited to %s</h1>
                  <p>%s invited you to join their narrative party.</p>
                  <p><a href=\"%s\">Accept invitation</a></p>
                  <p>This one-use invitation expires automatically.</p>
                </div>
                """.formatted(
                        HtmlUtils.htmlEscape(partyName),
                        HtmlUtils.htmlEscape(invitedBy),
                        HtmlUtils.htmlEscape(inviteUrl)
                );
        final Map<String, Object> body = Map.of(
                "from", properties.fromEmail(),
                "to", new String[]{to},
                "subject", "Convite para " + partyName,
                "html", html
        );
        restClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        return true;
    }
}
