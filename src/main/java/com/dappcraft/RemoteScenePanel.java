package com.dappcraft;

import com.dappcraft.db.UserInfo;
import com.google.gson.Gson;
import io.vertx.core.impl.ConcurrentHashSet;
import org.jboss.logging.Logger;

import javax.inject.Singleton;
import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class RemoteScenePanel {
    private static final Logger LOG = Logger.getLogger(RemoteScenePanel.class);

    public enum SessionType {
        Scene,
        Panel
    }

    private Gson gson = new Gson();

    Map<String, Set<Session>> sceneSessions = new ConcurrentHashMap<>();
    Map<String, Set<Session>> panelSessions = new ConcurrentHashMap<>();
//    Map<String, Timer> sceneTimers = new ConcurrentHashMap<>();
    Map<String, Map<String, Object>> sceneParams = new ConcurrentHashMap<>();
    Map<String, Map<String, Object>> sceneStats = new ConcurrentHashMap<>();

    private Map<String, Object> initSceneParams(String sceneId) {
        Map<String, Object> params;
        if (!sceneParams.containsKey(sceneId)) {
            params = new ConcurrentHashMap<>();
            sceneParams.put(sceneId, params);
            params.put("freq", 1.0 / 60);
            params.put("startPos", 0.);
            params.put("endPos", 10.);
            params.put("sceneParams", "{}");
        } else {
            params = sceneParams.get(sceneId);
        }
        return params;
    }


    public void onOpen(SessionType type, String sceneId, Session session) {
        if (type == SessionType.Scene) {
            if (!sceneSessions.containsKey(sceneId)) {
                ConcurrentHashSet<Session> sessions = new ConcurrentHashSet<>();
                sessions.add(session);
                sceneSessions.put(sceneId, sessions);
                LOG.infov("Open scene socket {0} first time", sceneId);
                Map<String, Object> params = initSceneParams(sceneId);
            } else {
                Set<Session> sessions = sceneSessions.get(sceneId);
                sessions.add(session);
                LOG.infov("Open scene socket {0} connected users {1}", sceneId, sessions.size());
            }
        } else if (type == SessionType.Panel) {
            if (!panelSessions.containsKey(sceneId)) {
                ConcurrentHashSet<Session> sessions = new ConcurrentHashSet<>();
                sessions.add(session);
                panelSessions.put(sceneId, sessions);
                LOG.infov("Open panel socket {0} first time", sceneId);
                Map<String, Object> params = initSceneParams(sceneId);
            } else {
                Set<Session> sessions = panelSessions.get(sceneId);
                sessions.add(session);
                LOG.infov("Open panel socket {0} connected users {1}", sceneId, sessions.size());
            }
        }
    }

    public void onClose(SessionType type, String sceneId, Session session) {
        if (type == SessionType.Scene) {
            Set<Session> sessions = sceneSessions.get(sceneId);
            if (sessions == null) {
                LOG.errorv("Close socket error, unknown sceneId {0}", sceneId);
            } else {
                boolean remove = sessions.remove(session);
                LOG.infov("Close scene socket {0} = {1}", sceneId, remove);
                if (sessions.isEmpty()) {
                    sceneSessions.remove(sceneId);
                    LOG.infov("Scene {0} empty", sceneId, remove);
                }
            }
        } else if (type == SessionType.Panel) {
            Set<Session> sessions = panelSessions.get(sceneId);
            if (sessions == null) {
                LOG.errorv("Close socket error, unknown sceneId {0}", sceneId);
            } else {
                boolean remove = sessions.remove(session);
                LOG.infov("Close panel socket {0} = {1}", sceneId, remove);
                if (sessions.isEmpty()) {
                    panelSessions.remove(sceneId);
                    LOG.infov("Panel {0} empty", sceneId, remove);
                }
            }
        }
    }
    Map<String, UserInfo> users = new ConcurrentHashMap<>();

    public void onMessage(SessionType type, String sceneId, Session session, String message) {
        Map<String, Object> params = sceneParams.get(sceneId);
        if (params == null) {
            LOG.errorv("sceneParams not found: {0}", sceneId);
        }

        WsCmdMessage cmdMessage = gson.fromJson(message, WsCmdMessage.class);
        LOG.infov("Receive cmd {0}-{1}:{2}", cmdMessage.cmd, message);
        WsResult result = new WsResult();
        result.cmd = cmdMessage.cmd;
        if (type == SessionType.Scene) {
            if (cmdMessage.userId == null || cmdMessage.userId.isEmpty()) {
                result.success = false;
                result.error = "Empty userId";
                LOG.errorv("Empty userId: {0}", cmdMessage.cmd);
            } else {
                switch (cmdMessage.cmd) {
                    case "connect":
                        UserInfo userInfo = onUserConnect(cmdMessage.userId, cmdMessage.userName);
                        result.success = userInfo != null;
                        result.userInfo = userInfo;
                        break;
                    case "getSceneParams":
                        String sceneParams = (String) params.get("sceneParams");
                        result.data = sceneParams;
                        result.success = true;
                        break;
                    case "updateStats":
                        break;
                    default:
                        result.success = false;
                        result.error = "Unknown command";
                        LOG.errorv("Unknown command {0} {1}", cmdMessage.cmd, message);
                }
            }
            sendResult(session,result);
        } else if (type == SessionType.Panel) {
            switch (cmdMessage.cmd) {
                case "connect":
//                    UserInfo userInfo = onUserConnect(cmdMessage.userId, cmdMessage.userName);
//                    result.success = userInfo != null;
//                    result.userInfo = userInfo;
                    break;
                case "broadcast":
                    result.success = onPanelBroadcast(sceneId,cmdMessage);
                    break;
                case "getStats":
                    break;
                case "updateParams":
                    LOG.infov("update Param {0} {1}", cmdMessage.step, cmdMessage.data);
                    params.put(cmdMessage.step, cmdMessage.data);
                    break;
                case "updateSceneParams":
                    params.put("sceneParams", cmdMessage.data);
                    break;
                default:
                    result.success = false;
                    result.error = "Unknown command";
                    LOG.errorv("Unknown command {0} {1}", cmdMessage.cmd, message);
            }
        }
    }

    public UserInfo onUserConnect(String userId, String userName) {
        UserInfo userInfo;
        userInfo = users.get(userId);
        if (userInfo == null) {
            userInfo = new UserInfo();
            userInfo.setUserName(userName);
            userInfo.setConnectDate(new Date());
            users.put(userId, userInfo);
        }
        return userInfo;
    }
    private boolean onPanelBroadcast(String sceneId, WsCmdMessage cmdMessage) {
        broadcast(sceneId, cmdMessage.data);
        return true;
    }

    private WsMessage parse(String json) {
        return gson.fromJson(json, WsMessage.class);
    }

    private void sendResult(Session session, WsResult result) {
        String resultMessage = gson.toJson(result);
        LOG.infov("Result cmd {0}-{1}:{2}", result.cmd, result.success, resultMessage);
        sendMessage(session, resultMessage);
    }

    private void sendMessage(Session session, String resultMessage) {
        session.getAsyncRemote().sendObject(resultMessage, res -> {
            if (res.getException() != null) {
                LOG.errorv(res.getException(), "Unable to send message: {0}", resultMessage);
            }
        });
    }

    private void broadcastResult(String sceneId, WsResult result) {
        String resultMessage = gson.toJson(result);
        LOG.infov("Result cmd {0}-{1}:{2}", result.cmd, result.success, resultMessage);
        broadcast(sceneId, resultMessage);
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
