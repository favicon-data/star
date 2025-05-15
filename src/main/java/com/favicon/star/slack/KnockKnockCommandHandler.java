package com.favicon.star.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class KnockKnockCommandHandler implements SlackCommandRegister{

    @Override
    public void register(App app) {
        app.command("/knockknock", (req, ctx) -> {
            log.info("🟡 /knockknock 명령어 호출됨");
            String text = req.getPayload().getText();
            if (text == null || text.isEmpty()) {
                return ctx.ack("⚠️ 올바른 형식으로 입력하세요: `/send_dm @user1 @user2 type1`");
            }

            String[] parts = text.split(" ");
            if (parts.length < 2) {
                return ctx.ack("⚠️ 사용자와 메시지 유형을 입력하세요.");
            }

            String messageType = parts[parts.length - 1];
            List<String> userMentions = List.of(parts).subList(0, parts.length - 1);
            for (String mention : userMentions) {
                String userId = extractUserId(mention, app);
                if (userId != null) {
                    sendDirectMessage(ctx, userId, messageType);
                }
            }

            log.info("✅ DM 전송 완료");
            return ctx.ack();
        });
    }

    private void sendDirectMessage(SlashCommandContext ctx, String userId, String messageType) {
        String message = getMessageByType(messageType);
        if (message == null) return;

        try {
            ChatPostMessageResponse response = ctx.client().chatPostMessage(r -> r
                    .channel(userId)
                    .text(message)
            );
            if (!response.isOk()) {
                log.error("DM 전송 실패: {}", response.getError());
            }
        } catch (Exception e) {
            log.error("DM 전송 중 예외 발생: ", e);
        }
    }

    private String getMessageByType(String type) {
        return switch (type) {
            case "PR" -> "📢 PR이 올라왔어요! 리뷰 부탁드려요 💬";
            case "pr" -> "📢 PR이 올라왔어요! 리뷰 부탁드려요 💬";
            case "LATE" -> "🔔 회의가 시작했어요! 빠른 참가 부탁드려요 🏃🏻";
            case "late" -> "🔔 회의가 시작했어요! 빠른 참가 부탁드려요 🏃🏻";
            default -> null;
        };
    }

    private String extractUserId(String mention, App app) {
        try {
            UsersListResponse response = app.client().usersList(r -> r);
            String username = mention.substring(1);
            if (response.isOk()) {
                return response.getMembers().stream()
                        .filter(user -> username.equals(user.getName()))
                        .map(user -> user.getId())
                        .findFirst()
                        .orElse(null);
            } else {
                log.error("❌ users.list API 호출 실패: {}", response.getError());
            }
        } catch (IOException | SlackApiException e) {
            log.error("❌ Slack API 호출 중 오류 발생: ", e);
        }
        return null;
    }
}
