package com.dappcraft;

import com.google.gson.Gson;
import io.vertx.core.impl.ConcurrentHashSet;
import org.jboss.logging.Logger;

import javax.inject.Singleton;
import javax.websocket.Session;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class SyncMoving {
    private static final Logger LOG = Logger.getLogger(SyncMoving.class);

    private Gson gson = new Gson();

    Map<String, Set<Session>> sceneSessions = new ConcurrentHashMap<>();
    Map<String, Timer> sceneTimers = new ConcurrentHashMap<>();
    Map<String, Map<String, Object>> sceneParams = new ConcurrentHashMap<>();

    private static double fraction(Long timestamp, Double freq) {
        double period = 1000 / freq;
        double fraction = (timestamp % period) / period;
        if (fraction > 0.5) {
            fraction = (1 - fraction) * 2;
        } else {
            fraction = fraction * 2;
        }
        return fraction;
    }

    private int updatePeriod = 1000;


    public void onOpen(Session session, String position, String realm) {
        String sceneId = position + '/' + realm;
        if (!sceneSessions.containsKey(sceneId)) {
            ConcurrentHashSet<Session> sessions = new ConcurrentHashSet<>();
            sessions.add(session);
            sceneSessions.put(sceneId, sessions);
            LOG.infov("Open scene socket {0} first time", sceneId);
            Map<String, Object> params;
            if (!sceneParams.containsKey(sceneId)) {
                params = new ConcurrentHashMap<>();
                sceneParams.put(sceneId, params);
                params.put("freq", 1.0 / 60);
                params.put("startPos", 0.);
                params.put("endPos", 10.);
            } else {
                params = sceneParams.get(sceneId);
            }

            Timer timer = new Timer();
            sceneTimers.put(sceneId, timer);

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (sessions.isEmpty()) return;
                    Double freq = (Double) params.get("freq");
                    Double startPos = (Double) params.get("startPos");
                    Double endPos = (Double) params.get("endPos");

                    long l = System.currentTimeMillis();
                    double fraction = fraction(l, freq);
                    double currPos = (endPos - startPos) * fraction;
                    double nextPos = (endPos - startPos) * fraction(l + updatePeriod, freq);

                    WsMessage msg = new WsMessage();
                    msg.setType("update");
                    msg.setCurrPos(currPos);
                    msg.setNextPos(nextPos);
                    msg.setFraction(fraction);
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
//                        LOG.infov("Broadcast timestamp {0} update {1}, active sessions {2}", l, sceneId, sessions.size());
                }
            }, updatePeriod, updatePeriod);
        } else {
            Set<Session> sessions = sceneSessions.get(sceneId);
            sessions.add(session);
            LOG.infov("Open scene socket {0} connected users {1}", sceneId, sessions.size());
        }
    }

    public void onClose(Session session, String position, String realm) {
        String sceneId = position + '/' + realm;
        Set<Session> sessions = sceneSessions.get(sceneId);
        if (sessions == null) {
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
//    Map<String, String> userScenes = new ConcurrentHashMap<>();

    public void onMessage(Session session, String message, String position, String realm) {
        String sceneId = position + '/' + realm;
        WsMessage msg = parse(message);
        if (msg.getType().equals("init")) {
            String username = msg.getUserName();
            if (username.isEmpty()) {
                LOG.errorv("init data not valid: {0} {1}", sceneId, message);
            } else {
//                    userScenes.put(username, sceneId);
                LOG.infov("User: {0} PIN: {1}", username, sceneId);
            }
        } else if (msg.getType().equals("changeParams")) {
            Map<String, Object> params = sceneParams.get(sceneId);
            if (params == null) {
                LOG.errorv("sceneParams not found: {0}", sceneId);
                return;
            }
            if (msg.getFreq() != null) {
                params.put("freq", msg.getFreq());
            }
            if (msg.getStartPos() != null) {
                params.put("startPos", msg.getStartPos());
            }
            if (msg.getEndPos() != null) {
                params.put("endPos", msg.getEndPos());
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
                    LOG.errorv(result.getException(), "Unable to send message: {0}", sceneId);
                }
            });
        });
    }
}
