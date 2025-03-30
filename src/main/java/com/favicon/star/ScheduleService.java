package com.favicon.star;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScheduleService {
    private final Map<String, LocalDateTime> schedules = new ConcurrentHashMap<>();
    private final ThreadPoolTaskScheduler taskScheduler;
    private final SlackNotifier slackNotifier;

    public ScheduleService(SlackNotifier slackNotifier) {
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.initialize();
        this.slackNotifier = slackNotifier;
    }


    public void addSchedule(String message, LocalDateTime time) {
        String scheduleId = message + "_" + time;
        schedules.put(scheduleId, time);

        LocalDateTime reminderTime = time.minusMinutes(10);
        taskScheduler.schedule(() -> {
            slackNotifier.sendMessage("⏳ 일정 시작 10분 전: " + message);
        }, java.util.Date.from(reminderTime.atZone(java.time.ZoneId.systemDefault()).toInstant()));

        // 일정 시작 알림
        taskScheduler.schedule(() -> {
            slackNotifier.sendMessage("🚀 일정 시작: " + message);
        }, java.util.Date.from(time.atZone(java.time.ZoneId.systemDefault()).toInstant()));

    }


    public void removeSchedule(String message) {
        schedules.remove(message);
    }

    public List<String> listSchedules() {
        List<String> list = new ArrayList<>();
        schedules.forEach((message, time) -> list.add(time + " - " + message));
        return list;
    }
}
