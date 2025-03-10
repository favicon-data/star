package com.favicon.star;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class GoogleCalendarService {
    private static final String APPLICATION_NAME = "STAR Calendar";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String SERVICE_ACCOUNT_KEY_PATH = "src/main/resources/client-secret.json";

    public Calendar getCalendarService() throws GeneralSecurityException, IOException {
        Credential credential = GoogleCredential.fromStream(new FileInputStream(SERVICE_ACCOUNT_KEY_PATH))
                .createScoped(List.of("https://www.googleapis.com/auth/calendar"));
        return new Calendar.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public List<Event> getEvents(String calendarId) throws GeneralSecurityException, IOException {
        Calendar service = getCalendarService();
        Events events = service.events().list(calendarId).execute();
        return events.getItems();
    }

    public Event addEvent(String calendarId, Event event) throws GeneralSecurityException, IOException {
        Calendar service = getCalendarService();
        return service.events().insert(calendarId, event).execute();
    }
}
