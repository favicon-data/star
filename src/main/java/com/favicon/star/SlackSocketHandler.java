package com.favicon.star;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.element.BlockElements;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewClose;
import com.slack.api.model.view.ViewSubmit;
import com.slack.api.model.view.ViewTitle;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
        App app = new App();

        app.command("/schedule", (req, ctx) -> {
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

        new SocketModeApp(app).start();
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
