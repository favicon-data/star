package com.favicon.star;

import com.google.api.services.calendar.model.Event;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@RestController
@RequestMapping("/calendar")
public class GoogleCalendarController {
    private final GoogleCalendarService googleCalendarService;

    public GoogleCalendarController(GoogleCalendarService googleCalendarService) {
        this.googleCalendarService = googleCalendarService;
    }

    @GetMapping("/events")
    public List<Event> getEvents(@RequestParam String calendarId) throws GeneralSecurityException, IOException {
        return googleCalendarService.getEvents(calendarId);
    }

    @PostMapping("/event")
    public Event addEvent(@RequestParam String calendarId, @RequestBody Event event) throws GeneralSecurityException, IOException {
        return googleCalendarService.addEvent(calendarId, event);
    }


}
