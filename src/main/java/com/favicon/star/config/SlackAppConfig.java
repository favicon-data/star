package com.favicon.star.config;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackAppConfig {

    @Value("${slack.app.token}")
    private String slackAppToken;
    @Value("${slack.bot.token}")
    private String slackBotToken;
    @Value("${slack.signing.secret}")
    private String signingSecret;

    @Bean
    public App slackApp() throws Exception {
        AppConfig config = new AppConfig();
        config.setSingleTeamBotToken(slackBotToken);
        config.setSigningSecret(signingSecret);
        return new App(config);
    }

    @Bean
    public SocketModeApp socketModeApp(App app) throws Exception {
        return new SocketModeApp(slackAppToken, app);
    }
}
