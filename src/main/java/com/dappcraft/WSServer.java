package com.dappcraft;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import io.vertx.core.impl.ConcurrentHashSet;
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
import java.util.Timer;


@ServerEndpoint("/{position}/{realm}")
@ApplicationScoped
public class WSServer {
    Map<String, Set<Session>> sceneSessions = new ConcurrentHashMap<>();
    Map<String, String> userScenes = new ConcurrentHashMap<>();
    Map<String, Timer> sceneTimers = new ConcurrentHashMap<>();


    private static final Logger LOG = Logger.getLogger(WSServer.class);
    private Gson gson = new Gson();
    private Random rnd = new Random();

    @OnOpen
    public void onOpen(Session session, @PathParam("position") String position, @PathParam("realm") String realm) {
        String sceneId = position + '/' + realm;
        if (!sceneSessions.containsKey(sceneId)) {
            ConcurrentHashSet<Session> sessions = new ConcurrentHashSet<>();
            sessions.add(session);
            sceneSessions.put(sceneId, sessions);
            LOG.infov("Open scene socket {0} first time", sceneId);
            Timer timer = new Timer();
            sceneTimers.put(sceneId, timer);

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (sessions.size() == 0) return;
                    long l = System.currentTimeMillis();

                    WsMessage msg = new WsMessage();
                    msg.setType("update");
                    msg.setTimestamp(l);
                    String message = gson.toJson(msg);
                    sessions.forEach(s -> {
                        if (s.isOpen()) {
                            s.getAsyncRemote().sendObject(message, result -> {
                                if (result.getException() != null) {
                                    LOG.errorv(result.getException(), "Unable to send message: {0}", sceneId);
                                }
                            });
                        }
                    });
                    LOG.infov("Broadcast timestamp {0} update {1}, active sessions {2}", l, sceneId, sessions.size());
                }
            }, 1000, 1000);
        } else {
            Set<Session> sessions = sceneSessions.get(sceneId);
            sessions.add(session);
            LOG.infov("Open scene socket {0} connected users {1}", sceneId, sessions.size());
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("position") String position, @PathParam("realm") String realm) {
        String sceneId = position + '/' + realm;
        Set<Session> sessions = sceneSessions.get(sceneId);
        if(sessions == null) {
            LOG.errorv("Close socket error, unknown sceneId {0}", sceneId);
        } else {
            boolean remove = sessions.remove(session);
            LOG.infov("Close scene socket {0} = {1}", sceneId, remove);
            if (sessions.isEmpty()) {
               sceneSessions.remove(sceneId);
                LOG.infov("Scene {0} empty", sceneId, remove);
                sceneTimers.get(sceneId).cancel();
                sceneTimers.remove(sceneId);
            }
        }
    }

    @OnError
    public void onError(Session session, @PathParam("position") String position, @PathParam("realm") String realm, Throwable throwable) {
        LOG.errorv(throwable, "Socket error, unknown position {0}, {1}", position, realm);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("position") String position, @PathParam("realm") String realm) {
        String sceneId = position + '/' + realm;
        WsMessage msg = parse(message);
        if (msg.getType().equals("init")) {
            String username = msg.getUserName();
            if (username.isEmpty()) {
                LOG.errorv("init data not valid: {0} {1}", sceneId, message);
            } else {
                userScenes.put(username, sceneId);
                LOG.infov("User: {0} PIN: {1}", username, sceneId);
            }
        }
    }

    private WsMessage parse(String json) {
        return gson.fromJson(json, WsMessage.class);
    }

    private void broadcast(String sceneId, String message) {
        sceneSessions.get(sceneId).forEach(s -> {
            s.getAsyncRemote().sendObject(message, result -> {
                if (result.getException() != null) {
                    LOG.errorv( result.getException(), "Unable to send message: {0}", sceneId);
                }
            });
        });
    }
}
