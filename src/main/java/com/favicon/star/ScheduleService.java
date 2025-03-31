package com.favicon.star;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

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
        boolean isRecurring = message.contains("[정기]");

        schedules.put(scheduleId, time);
        scheduleNotification(message, time);

        if (isRecurring) {
            taskScheduler.schedule(() -> addRecurringSchedule(message, time),
                    java.util.Date.from(time.plusWeeks(1).atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }
    }

    private void addRecurringSchedule(String message, LocalDateTime time) {
        LocalDateTime nextTime = time.plusWeeks(1);
        String scheduleId = message + "_" + nextTime;

        if (schedules.values().stream().noneMatch(t -> t.isEqual(nextTime))) {
            schedules.put(scheduleId, nextTime);
            scheduleNotification(message, nextTime);

            taskScheduler.schedule(() -> addRecurringSchedule(message, nextTime),
                    java.util.Date.from(nextTime.plusWeeks(1).atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }
    }

    private void scheduleNotification(String message, LocalDateTime time) {
        LocalDateTime reminderTime = time.minusMinutes(10);

        taskScheduler.schedule(() -> {
            slackNotifier.sendMessage("⏳ 일정 시작 10분 전: " + message);
        }, java.util.Date.from(reminderTime.atZone(java.time.ZoneId.systemDefault()).toInstant()));

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
