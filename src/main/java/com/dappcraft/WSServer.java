package com.dappcraft;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.dappcraft.broxus.*;
import com.dappcraft.db.Prize;
import com.dappcraft.db.Result;
import com.dappcraft.db.UserInfo;
import com.google.gson.Gson;
import io.vertx.core.impl.ConcurrentHashSet;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.Session;
import javax.ws.rs.WebApplicationException;
import java.util.Timer;


@ServerEndpoint("/{position}/{realm}")
@ApplicationScoped
public class WSServer {
//    @Inject
    SyncMoving syncMoving;

    @Inject
    RemoteScenePanel remoteScenePanel;

//    @Inject
    Store store;

//    @Inject
//    @RestClient
    TelegramApiService telegramApiService;

//    @Inject
//    @RestClient
    DclApiService dclApiService;

    private static final Logger LOG = Logger.getLogger(WSServer.class);
    private Gson gson = new Gson();
    private Random rnd = new Random();


    @PostConstruct
    public void init() {
        LOG.infov("init");
    }

    public UserInfo onUserConnect(String userId, String userName) {
        UserInfo userInfo;
        userInfo = store.getUser(userId);
        if (userInfo == null) {
            userInfo = new UserInfo();
            userInfo.setUserName(userName);
            userInfo.setConnectDate(new Date());
            userInfo.setJoinGroup(false);
            userInfo.setRewardClaimed(false);
            store.updateUser(userId, userInfo);
        }
        return userInfo;
    }

    public boolean onCompleteStep(String userId, String step) {
        UserInfo userInfo = store.getUser(userId);
        userInfo.setQuestStep(step);
        return store.updateUser(userId, userInfo) != null;
    }

    public boolean onCheckGroupTask(String userId, String telegramName) {
        try {
//            CheckResult check = new CheckResult();
//            check.user_id = "0";
//            check.setResult(true);
            CheckResult check = telegramApiService.check("TONCRYSTAL", telegramName);

            LOG.infov("telegramApiService check {0} {1}", check.getResult(), check.user_id);
            if (check.getResult()) {
                UserInfo userInfo = store.getUser(userId);
                if(userInfo.getReward() == null && !userInfo.getRewardClaimed()) {
                    userInfo.setTelegramName(telegramName);
                    userInfo.setTelegramId(check.user_id);
                    userInfo.setJoinGroup(true);
                    store.updateUser(userId, userInfo);
                } else {
                    LOG.errorv("Try to change telegram account {0}>{1} after get reward {2}", userInfo.getTelegramId(), telegramName, userInfo.getReward());
                }
            }
            return check.getResult();
        } catch (WebApplicationException e) {
            LOG.errorv(e, "CheckGroup fail {0} - (1}", userId, telegramName);
            return false;
        }
    }

    public void onGetRandomReward(String userId, String debugCode, WsResult result) {
        if (!store.findDuplicatesTelegram(userId)) {
            Result reward = store.randomPrize(userId);
            if (reward != null) {
                if (reward.prize != null) {
                    result.reward = reward.user.getReward();
                    result.success = true;
                    result.claimApproved = reward.user.getClaimApproved();
                    result.error = reward.message;
                    LOG.infov("randomPrize for {0} - {1}", userId, result.reward);
                } else {
                    result.success = false;
                    result.error = reward.message;
                }
            } else {
                result.success = false;
                result.error = "Get reward fail";
            }
        } else {
            result.success = false;
            result.error = "Reward already claimed for this telegram user!";
        }
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("position") String position, @PathParam("realm") String realm) {
        if (position.equals("0")) {
            syncMoving.onOpen(session,position,realm);
        } else if (position.equals("dc_scene") || position.equals("dc_panel")) {
            RemoteScenePanel.SessionType type = RemoteScenePanel.SessionType.Scene;
            if (position.equals("dc_panel")) type = RemoteScenePanel.SessionType.Panel;
            String sceneId = realm;
            remoteScenePanel.onOpen(type, sceneId, session);
        } else {
            LOG.infov("Open connection {0} {1}", realm, position);
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("position") String position, @PathParam("realm") String realm) {
        if (position.equals("0")) {
            syncMoving.onClose(session,position,realm);
        } else if (position.equals("dc_scene") || position.equals("dc_panel")) {
            RemoteScenePanel.SessionType type = RemoteScenePanel.SessionType.Scene;
            if (position.equals("dc_panel")) type = RemoteScenePanel.SessionType.Panel;
            String sceneId = realm;
            remoteScenePanel.onClose(type, sceneId, session);
        }
    }

    @OnError
    public void onError(Session session, @PathParam("position") String position, @PathParam("realm") String realm, Throwable throwable) {
        LOG.errorv(throwable, "Socket error, unknown position {0}, {1}", position, realm);
    }

    @OnMessage
    public void onMessage(Session session, String message, @PathParam("position") String position, @PathParam("realm") String realm) {
        if (position.equals("0")) {
            syncMoving.onMessage(session,message,position,realm);
        } else if (position.equals("dc_scene") || position.equals("dc_panel")) {
            RemoteScenePanel.SessionType type = RemoteScenePanel.SessionType.Scene;
            if (position.equals("dc_panel")) type = RemoteScenePanel.SessionType.Panel;
            String sceneId = realm;
            remoteScenePanel.onMessage(type, sceneId, session, message);
        } else {
            WsCmdMessage cmdMessage = gson.fromJson(message, WsCmdMessage.class);
            LOG.infov("Receive cmd {0}-{1}:{2}", cmdMessage.cmd, message);
            WsResult result = new WsResult();
            result.cmd = cmdMessage.cmd;
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
                    case "completeStep":
                        result.success = onCompleteStep(cmdMessage.userId, cmdMessage.step);
                        break;
                    case "checkGroup":
                        result.success = onCheckGroupTask(cmdMessage.userId, cmdMessage.telegramName);
                        break;
                    case "getReward":
                        onGetRandomReward(cmdMessage.userId, cmdMessage.debugCode, result);
                        break;
                    case "signMessage":
                        result.success = onSignMessage(cmdMessage.userId, cmdMessage.signature, cmdMessage.messageHex, result);
                        break;
                    default:
                        result.success = false;
                        result.error = "Unknown command";
                        LOG.errorv("Unknown command {0} {1}", cmdMessage.cmd, message);
                }
            }
            String resultMessage = gson.toJson(result);
            LOG.infov("Result cmd {0}-{1}:{2}", result.cmd, result.success, resultMessage);
            session.getAsyncRemote().sendObject(resultMessage, res -> {
                if (res.getException() != null) {
                    LOG.errorv( res.getException(), "Unable to send message: {0}", resultMessage);
                }
            });
        }
    }

    private boolean onSignMessage(String userId, String signature, String messageHex, WsResult result) {
        UserInfo user = store.getUser(userId);
        if (user != null) {
            for (DclUserInfo userInfo : dclApiService.getUserInfo(userId.toLowerCase())) {
                if (userInfo.metadata != null && userInfo.metadata.avatars != null && !userInfo.metadata.avatars.isEmpty()) {
                    DclAvatar dclAvatar = userInfo.metadata.avatars.get(0);
                    LOG.infov("userInfo {0}", dclAvatar.name);
                    user.setUserName(dclAvatar.name);
                    if (dclAvatar.hasClaimedName && !dclAvatar.name.contains("TheUnity")) { // && !dclAvatar.name.contains("#") && !dclAvatar.name.contains("Guest-")
                        user.setHasClaimedName(true);
                    }
//                for (String wearable : dclAvatar.avatar.wearables) {
//                    // dcl://halloween_2020/hwn_2020_dracula_mouth
//                    // dcl://dappcraft_moonminer/moonminer_pants_lower_body
//                    LOG.infov("wearable {0}", wearable);
//                }
                }
            }

            user.setHasClaimedName(true);

            if (user.getHasClaimedName()) {
                //TODO signature verify
                if (signature != null  && messageHex != null && messageHex.length() >= 16) {
                    if (signature.length() == 262 || signature.length() == 132) {
                        store.updateUser(userId, user);
                        return true;
                    }
                }
                result.error = "Signature is not valid";
                return false;
            } else {
                result.error = "Do not have claimed DCL Name";
                return false;
            }
        }
        result.error = "User not found";
        return false;
    }
}
