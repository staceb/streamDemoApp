package com.maciek.streamDemo.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class WebRTCCommunicationHandler extends TextWebSocketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebRTCCommunicationHandler.class);
    private static final Gson GSON = new GsonBuilder().create();
    private static final String PRESENTER = "presenter";
    private static final String VIEWER = "viewer";
    private static final String HELLO_MESSAGE = "helloMessage";
    private Map<String, WebSocketSession> sessions;

    @Autowired
    WebRTCCommunicationHandler() {
        this.sessions = new ConcurrentHashMap<>();
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        this.sessions.values().forEach(webSocketSession -> {
            try {
                webSocketSession.sendMessage(new PingMessage());
            } catch (IOException e) {
                LOGGER.error("Exception on sending ping message", e);
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonObject jsonObject = GSON.fromJson(message.getPayload(), JsonObject.class);
        String payload = jsonObject.toString();
        System.out.println(String.format("Incoming message: {%s}", payload));
        JsonElement helloMessage = jsonObject.get(HELLO_MESSAGE);
        if (helloMessage != null) {
            String helloMessageAsString = helloMessage.getAsString();
            if (helloMessageAsString.equals(PRESENTER)) {
                System.out.println("Adding presenter");
                addSession(PRESENTER, session);
            } else if (helloMessageAsString.startsWith(VIEWER)) {
                System.out.println("Adding viewer");
                addSession(helloMessageAsString, session);
            }
        }
        //presenter -> viewer
        String id = session.getId();
        WebSocketSession presenter = sessions.get(PRESENTER);
        WebSocketSession viewer = sessions.get(VIEWER);
        if (presenter != null && presenter.getId().equals(id)) {
            System.out.println("Sending to viewer");
            viewer.sendMessage(message);
            //viewer -> presenter
        } else if (presenter != null && viewer != null && viewer.getId().equals(id)) {
            System.out.println("Sending to presenter");
            presenter.sendMessage(message);
        }
    }

    private void addSession(String sessionName, WebSocketSession session) {
        this.sessions.put(sessionName, session);
    }

    private List<WebSocketSession> getAllViewers() {
        Set<Map.Entry<String, WebSocketSession>> sessionEntries = sessions.entrySet();
        return sessionEntries
                .stream()
                .filter(entry -> VIEWER.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
}
