package com.dappcraft;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.Session;


@ServerEndpoint("/{type}/{pinCode}")
@ApplicationScoped
public class WSServer {
    Map<String, Session> sceneSessions = new ConcurrentHashMap<>();
//    Map<String, Session> alienSessions = new ConcurrentHashMap<>();
    Map<String, Session> controllerSessions = new ConcurrentHashMap<>();
    Map<String, String> userPins = new ConcurrentHashMap<>();

    @Inject
    Store store;

    private static final Logger LOG = Logger.getLogger(WSServer.class);
    private Gson gson = new Gson();
    @OnOpen
    public void onOpen(Session session, @PathParam("type") String type, @PathParam("pinCode") String pinCode) {
        if(type.equals("scene") || type.equals("alien")) {
            sceneSessions.put(pinCode, session);
            LOG.infov("Open scene socket {0}", pinCode);
        } else if (type.equals("controller")) {
            controllerSessions.put(pinCode, session);
            LOG.infov("Open controller socket {0}", pinCode);
//        } else if(type.equals("alien")) {
//            String address = pinCode;
//            alienSessions.put(address, session);
//            LOG.infov("Open alien socket {0}", address);
        } else {
            LOG.errorv("Open socket error, unknown type {0}, {1}", type, pinCode);
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("type") String type, @PathParam("pinCode") String pinCode) {
        if(type.equals("scene") || type.equals("alien")) {
            sceneSessions.remove(pinCode);
            LOG.infov("Close scene socket {0}", pinCode);
        } else if (type.equals("controller")) {
            controllerSessions.remove(pinCode);
            LOG.infov("Close controller socket {0}", pinCode);
//        } else if(type.equals("alien")) {
//            String address = pinCode;
//            alienSessions.remove(address);
//            LOG.infov("Close alien socket {0}", address);
        } else {
            LOG.errorv("Close socket error, unknown type {0}, {1}", type, pinCode);
        }
    }

    @OnError
    public void onError(Session session, @PathParam("type") String type, @PathParam("pinCode") String pinCode, Throwable throwable) {
        LOG.errorv(throwable, "Socket error, unknown type {0}, {1}", type, pinCode);
        if(type.equals("scene") || type.equals("alien")) {
            sceneSessions.remove(pinCode);
        } else if (type.equals("controller")) {
            controllerSessions.remove(pinCode);
        }
    }

    @OnMessage
    public void onMessage(String message, @PathParam("type") String type, @PathParam("pinCode") String pinCode) {
        if(type.equals("scene") || type.equals("alien")) {
            String collection = "mars_scores";
            if (type.equals("alien")) {
                collection = "alien_scores";
            }
            WsMessage msg = parse(message);
            if (msg.getType().equals("init")) {
                String pin = msg.getPin();
                String username = msg.getUserName();
                if (username.isEmpty()) {
                    LOG.errorv("init data not valid: {0} {1}", pinCode, message);
                    userPins.put(pinCode, "UnknownGuest-" +pinCode);
                    LOG.warnv("User: {0} PIN: {1}", username, pinCode);
                } else {
                    if (!pinCode.equals(pin)) {
                        LOG.errorv("PIN codes not equals: {0} {1}!={2}", username, pin, pinCode);
                    }
                    userPins.put(pinCode, username);
                    LOG.infov("User: {0} PIN: {1}", username, pinCode);
                }
            } else if (msg.getType().equals("score")) {
                String userName = userPins.get(pinCode);
                LOG.infov("Score {0}({1}) - {2} - LEVEL: {3}; KILLS: {4}", userName, pinCode, msg.getScore(), msg.getLevel(), msg.getKills());
                ScoreResult newUserScore = new ScoreResult(msg.getScore().longValue(), msg.getLevel().longValue(), msg.getKills().longValue());
                List<ScoreResult> results = store.saveScore(collection, userName, newUserScore);
                WsMessage resultMsg = new WsMessage();
                resultMsg.setType("scoreTable");
                resultMsg.setScoreTable(results);
                sceneSessions.get(pinCode).getAsyncRemote().sendObject(gson.toJson(resultMsg), result -> {
                    if (result.getException() != null) {
                        LOG.errorv(result.getException(), "Unable to send message to scene for {0}", pinCode);
                    }
                });
            } else if (msg.getType().equals("register")) {
                String userName = userPins.get(pinCode);
                LOG.infov("Score {0}({1}) - {2}", userName, pinCode, message);
            } else {
                LOG.infov("Unknown onMessage scene socket {0}: {1}", pinCode, message);
            }
        } else if (type.equals("controller")) {
//            WsMessage msg = parse(message);
//            LOG.infov("controller msg {0}, {1}", pinCode, msg.getType());
            if (sceneSessions.containsKey((pinCode))) {
//                if (msg.getType().equals("rotate")) {
//                    LOG.infov("send {0}, {1}, {2}, {3}", msg.getQuat()[0], msg.getQuat()[1], msg.getQuat()[2], msg.getQuat()[3]);
//                }
//                    message = gson.toJson(msg.getQuat());
                    sceneSessions.get(pinCode).getAsyncRemote().sendObject(message, result -> {
                        if (result.getException() != null) {
                            LOG.errorv(result.getException(), "Unable to send message to scene for {0}", pinCode);
                        }
                    });
            }
        } else {
            LOG.errorv("onMessage error, unknown type {0}, {1}", type, pinCode);
        }
    }

    private WsMessage parse(String json) {

        return gson.fromJson(json, WsMessage.class);
    }

    private void broadcast(String message) {
        sceneSessions.values().forEach(s -> {
            s.getAsyncRemote().sendObject(message, result -> {
                if (result.getException() != null) {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }
}
