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
    Map<String, Set<Session>> sceneSessions = new ConcurrentHashMap<>();
    Map<String, String> userScenes = new ConcurrentHashMap<>();
    Map<String, Timer> sceneTimers = new ConcurrentHashMap<>();
    Map<String, Map<String, Object>> sceneParams = new ConcurrentHashMap<>();

    @Inject
    Store store;

    @Inject
    @RestClient
    TelegramApiService telegramApiService;

    @Inject
    @RestClient
    BroxusService broxusService;

    String broxusApiKey = "imEUINZFjEaRIbNiDPI4Yk1oj7Oo-u0TaX3WIURXEEok-z99ibq_yG-XMsY8Rm1p";
    String broxusSecretKey = "7g-OoAojQ-24TIDKxRkhlVlRfmzHhjSBn6kGWRLNlxJd6VdIwNKHCTvl0hMXKEBG";
    String broxusWorkspaceId = "f6e28519-e6b5-4fc1-a6a7-e0dd7dd02bd7";

    private static final Logger LOG = Logger.getLogger(WSServer.class);
    private Gson gson = new Gson();
    private Random rnd = new Random();

    private int updatePeriod = 1000;

    @PostConstruct
    public void init() {
        LOG.infov("init");
        //            setupPrizes();

        for (Prize prize : store.getPrizes().values()) {
            LOG.infov("prize {0}TON - {1}/{2} ", prize.getAmount(), prize.getCount(), prize.getInitCount());
        }
        broxus();
    }

    private static double fraction(Long timestamp, Double freq) {
        double period = 1000 / freq;
        double fraction = (timestamp % period) / period;
        if (fraction > 0.5) {
            fraction = (1-fraction) * 2;
        } else {
            fraction = fraction * 2;
        }
        return fraction;
    }

    public String getSignByUrlAndBody(String url, String body, Long timestamp) throws NoSuchAlgorithmException, InvalidKeyException {
        String algorithm  = "HmacSHA256";

        SecretKeySpec signingKey = new SecretKeySpec(broxusSecretKey.getBytes(), algorithm);

        Mac mac = Mac.getInstance(algorithm);
        mac.init(signingKey);

        String tsBodyUrl = String.valueOf(timestamp) + url + body;
        byte[] result = mac.doFinal((tsBodyUrl).getBytes());

        String base64 = Base64.getEncoder().encodeToString(result);

        return base64;
    }

    public List<Balance> getBalance() {
        RequestBalance req = new RequestBalance();
        req.workspaceId = broxusWorkspaceId;

        try {
            long timestamp = System.currentTimeMillis();
            String body = gson.toJson(req);
            String sign = getSignByUrlAndBody("/v1/users/balances", body, timestamp);
            List<Balance> balances = broxusService.usersBalances(broxusApiKey, String.valueOf(timestamp), sign, body);
            return balances;
        } catch (Exception e) {
            LOG.errorv( e, "getSignByUrlAndBody: {0}", gson.toJson(req));
            return null;
        }
    }

    public String transfer(String telegramUserId, Double amount) {
        RequestTransfer req = new RequestTransfer();
        req.id = UUID.randomUUID().toString();
        req.currency = "TON";
        req.fromWorkspaceId = broxusWorkspaceId;
        req.fromAddressType = "system";
        req.fromUserAddress = "cow";
        req.toWorkspaceId = "3725653a-8ee6-4285-80a0-76cac17b29f9";
        req.toAddressType = "telegram";
        req.toUserAddress = telegramUserId;
        req.value = amount;

        try {
            long timestamp = System.currentTimeMillis();
            String body = gson.toJson(req);
            String sign = getSignByUrlAndBody("/v1/transfer", body, timestamp);
            ResponseTransfer transfer = broxusService.transfer(broxusApiKey, String.valueOf(timestamp), sign, body);
            LOG.infov("transfer to {0} amount: {1} id: {2}", req.toUserAddress, req.value, transfer.id);
            return transfer.id;
        } catch (Exception e) {
            LOG.errorv( e, "getSignByUrlAndBody: {0}", gson.toJson(req));
            return null;
        }
    }

    public String getAddress() {
        RequestAddressesRenew req = new RequestAddressesRenew();
        req.currency = "TON";
        req.workspaceId = broxusWorkspaceId;
        req.addressType = "system";
        req.userAddress = "cow";

        try {
            long timestamp = System.currentTimeMillis();
            String body = gson.toJson(req);
            String sign = getSignByUrlAndBody("/v1/static_addresses/renew", body, timestamp);
            ResponseAddressesRenew addressesRenew = broxusService.addressesRenew(broxusApiKey, String.valueOf(timestamp), sign, body);
            LOG.infov("addressesRenew: {0}", addressesRenew.blockchainAddress);
            return addressesRenew.blockchainAddress;
        } catch (Exception e) {
            LOG.errorv( e, "addressesRenew: {0}", gson.toJson(req));
            return "";
        }
    }

    private Double checkTonBalance() {
        List<Balance> balances = getBalance();
        Double tonBalance = 0.;
        if (balances != null) {
            for (Balance balance : balances) {
                LOG.infov("balance {0}, {1} {2}, {3} frozen: {4} total: {5}", balance.addressType, balance.userAddress, balance.available, balance.currency, balance.frozen, balance.total);
                if (balance.currency.equals("TON")) {
                    tonBalance = balance.available;
                }
            }
        }
        return tonBalance;
    }

    public void broxus() {
        List<Workspace> result = broxusService.workspaces(broxusApiKey);

        if (!result.isEmpty()) {
            LOG.infov("broxusService {0}, {1} {2}", result.size(), result.get(0).id, result.get(0).name);
            checkTonBalance();



//            getAddress();

        }
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

    public boolean onClaimReward(String userId, WsResult result) {
        UserInfo userInfo = store.getUser(userId);

        if(!userInfo.getRewardClaimed() && userInfo.getReward() != null && userInfo.getClaimApproved()) {
            Double balance = checkTonBalance();
            if (balance < userInfo.getReward()) {
                LOG.errorv("No balance {0} for claim reward {1}", balance, userInfo.getReward());
                result.error = "Try to claim later";
                return false;
            }

            String trxId = transfer(userInfo.getTelegramId(), userInfo.getReward());
            if (trxId != null) {
                userInfo.setRewardClaimed(true);
                userInfo.setClaimDate(new Date());
                userInfo.setClaimTransactionId(trxId);
                boolean res = store.updateUser(userId, userInfo) != null;
                LOG.infov("ClaimReward for {0} - {1}; trx={2}", userId, res, trxId);
                return res;
            } else {
                result.error = "Broxus api fail; Try to claim later";
            }
        } else {
            result.error = "Already claimed, go to @broxusbot";
        }
        LOG.warnv("ClaimReward fail for {0} claimed: {1}; reward: {2}; approved: {3}", userId, userInfo.getRewardClaimed(), userInfo.getReward(), userInfo.getClaimApproved());
        return false;
    }

    private void setupPrizes() {
        Map<Integer, Integer> p = new HashMap<>();
        p.put(1000, 1);
        p.put(100, 30);
        p.put(50, 100);
        p.put(25, 200);
        p.put(10, 500);
        p.put(5, 1200);

        Prize prize = new Prize();

        for (Map.Entry<Integer, Integer> entry : p.entrySet()) {
            int amount = entry.getKey();
            int count = entry.getValue();
//            prize.setAmount((double) amount);
            prize.setAmount((double) amount * 0.001);
            prize.setInitCount(count);
            prize.setCount(count);
            store.updatePrize(String.valueOf(amount), prize);
        }
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("position") String position, @PathParam("realm") String realm) {
        if (position.equals("0")) {
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
        } else {
            LOG.infov("Open connection {0} {1}", realm, position);
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("position") String position, @PathParam("realm") String realm) {
        if (position.equals("0")) {
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
    }

    @OnError
    public void onError(Session session, @PathParam("position") String position, @PathParam("realm") String realm, Throwable throwable) {
        LOG.errorv(throwable, "Socket error, unknown position {0}, {1}", position, realm);
    }

    @OnMessage
    public void onMessage(Session session, String message, @PathParam("position") String position, @PathParam("realm") String realm) {
        if (position.equals("0")) {
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
                    case "claimReward":
                        result.success = onClaimReward(cmdMessage.userId, result);
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
