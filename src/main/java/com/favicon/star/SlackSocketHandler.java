package com.favicon.star;

import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.element.BlockElements;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewClose;
import com.slack.api.model.view.ViewSubmit;
import com.slack.api.model.view.ViewTitle;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class SlackSocketHandler {
    private final ScheduleService scheduleService;
    private final SlackNotifier slackNotifier;

    public SlackSocketHandler(ScheduleService scheduleService, SlackNotifier slackNotifier) {
        this.scheduleService = scheduleService;
        this.slackNotifier = slackNotifier;
    }

    @PostConstruct
    public void init() throws Exception {
        try {
            log.info("🔵 SlackSocketHandler 초기화 중...");
            App app = new App();

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
                //            return ctx.ack("✅ DM을 전송했습니다.");
                return ctx.ack();
            });

            app.command("/schedule", (req, ctx) -> {
                log.info("🟡 /schedule 명령어 호출됨");
                String triggerId = req.getPayload().getTriggerId();
                if (triggerId == null || triggerId.isEmpty()) {
                    return ctx.ack("⚠️ 모달을 열 수 없습니다. 다시 시도해주세요.");
                }

                ctx.ack();

                View modalView = View.builder()
                        .type("modal")
                        .callbackId("schedule_modal")
                        .title(ViewTitle.builder().type("plain_text").text("일정 관리").build())
                        .submit(ViewSubmit.builder().type("plain_text").text("확인").build())
                        .close(ViewClose.builder().type("plain_text").text("취소").build())
                        .blocks(buildModalBlocks())
                        .build();

                ViewsOpenResponse response = ctx.client().viewsOpen(r -> r.triggerId(triggerId).view(modalView));
                if (!response.isOk()) {
                    log.error("Error opening modal: {}", response.getError());
                }

                return ctx.ack();
            });

            app.viewSubmission("schedule_modal", (req, ctx) -> {
                var values = req.getPayload().getView().getState().getValues();
                String action = values.get("action_block").get("action").getSelectedOption().getValue();
                String response;

                switch (action) {
                    case "add":
                        String date = values.get("date_block").get("date").getSelectedDate();
                        String time = values.get("start_time_block").get("start_time").getSelectedTime();
                        String content = values.get("content_block").get("content").getValue();
                        if (date == null || time == null) {
                            response = "❌ 날짜와 시간을 모두 입력해주세요.";
                            break;
                        }
                        LocalDateTime startDateTime = LocalDateTime.parse(date + "T" + time);
                        scheduleService.addSchedule(content, startDateTime);
                        response = "✅ 일정이 추가되었습니다.";
                        break;
                    case "remove":
                        String removeContent = values.get("content_block").get("content").getValue();
                        scheduleService.removeSchedule(removeContent);
                        response = "✅ 일정이 삭제되었습니다.";
                        break;
                    case "list":
                        List<String> schedules = scheduleService.listSchedules();
                        response = schedules.isEmpty() ? "📭 등록된 일정이 없습니다." : String.join("\n", schedules);
                        break;
                    default:
                        response = "❌ 지원하지 않는 명령어입니다.";
                }

                slackNotifier.sendMessage(response);
                return ctx.ack();
            });

            log.info("🟢 Slack 명령어 핸들러 등록 완료");

            new SocketModeApp(app).start();
            log.info("🔵 SocketModeApp 실행 완료");
        } catch (Exception e) {
            log.error("❌ SlackSocketHandler 초기화 중 오류 발생: ", e);
        }
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

    private List<LayoutBlock> buildModalBlocks() {
        return Blocks.asBlocks(
                Blocks.section(s -> s.text(com.slack.api.model.block.composition.BlockCompositions.plainText("원하는 작업을 선택하세요."))),
                Blocks.input(i -> i.blockId("action_block")
                        .label(com.slack.api.model.block.composition.BlockCompositions.plainText("작업 선택"))
                        .element(BlockElements.staticSelect(select -> select.actionId("action")
                                .options(List.of(
                                        com.slack.api.model.block.composition.BlockCompositions.option(com.slack.api.model.block.composition.BlockCompositions.plainText("일정 추가"), "add"),
                                        com.slack.api.model.block.composition.BlockCompositions.option(com.slack.api.model.block.composition.BlockCompositions.plainText("일정 삭제"), "remove"),
                                        com.slack.api.model.block.composition.BlockCompositions.option(com.slack.api.model.block.composition.BlockCompositions.plainText("일정 조회"), "list")
                                ))
                        ))
                ),
                Blocks.input(i -> i.blockId("date_block")
                        .label(com.slack.api.model.block.composition.BlockCompositions.plainText("날짜"))
                        .element(BlockElements.datePicker(p -> p.actionId("date")))
                        .optional(true)
                ),
                Blocks.input(i -> i.blockId("start_time_block")
                        .label(com.slack.api.model.block.composition.BlockCompositions.plainText("시작 시간"))
                        .element(BlockElements.timePicker(p -> p.actionId("start_time")))
                        .optional(true)
                ),
                Blocks.input(i -> i.blockId("content_block")
                        .label(com.slack.api.model.block.composition.BlockCompositions.plainText("일정 내용"))
                        .element(BlockElements.plainTextInput(t -> t.actionId("content")))
                        .optional(true)
                )
        );
    }

}
