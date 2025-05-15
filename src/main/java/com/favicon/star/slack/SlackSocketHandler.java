package com.favicon.star.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SlackSocketHandler {

    private final App app;
    private final SocketModeApp socketModeApp;
    private final List<SlackCommandRegister> registers;

    public SlackSocketHandler(App app, SocketModeApp socketModeApp, List<SlackCommandRegister> registers) {
        this.app = app;
        this.socketModeApp = socketModeApp;
        this.registers = registers;
    }

    @PostConstruct
    public void init() throws Exception {
        log.info("SlackSocketHandler 초기화 시작");
        for (SlackCommandRegister register : registers) {
            register.register(app);
            log.info("SlackCommandRegister 등록 완료: {}", register.getClass().getSimpleName());
        }
        socketModeApp.start();
    }
}
