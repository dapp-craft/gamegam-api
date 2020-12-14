package com.dappcraft;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.dappcraft.broxus.*;
import com.google.gson.Gson;
import io.vertx.core.impl.ConcurrentHashSet;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

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
import java.util.Timer;


@ServerEndpoint("/{position}/{realm}")
@ApplicationScoped
public class WSServer {
    Map<String, Set<Session>> sceneSessions = new ConcurrentHashMap<>();
    Map<String, String> userScenes = new ConcurrentHashMap<>();
    Map<String, Timer> sceneTimers = new ConcurrentHashMap<>();
    Map<String, Map<String, Object>> sceneParams = new ConcurrentHashMap<>();

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

    public boolean transfer(String telegramUserId, Double amount) {
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
            return true;
        } catch (Exception e) {
            LOG.errorv( e, "getSignByUrlAndBody: {0}", gson.toJson(req));
            return false;
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

    public void broxus() {
        List<Workspace> result = broxusService.workspaces(broxusApiKey);

        if (!result.isEmpty()) {
            LOG.infov("broxusService {0}, {1} {2}", result.size(), result.get(0).id, result.get(0).name);

            List<Balance> balances = getBalance();
            if (balances != null) {
                for (Balance balance : balances) {
                    LOG.infov("balance {0}, {1} {2}, {3}", balance.addressType, balance.userAddress, balance.available, balance.currency);
                }
            }

//            getAddress();

        }
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("position") String position, @PathParam("realm") String realm) {

        CheckResult check = telegramApiService.check("TONCRYSTAL", "sanya_gk");
//        CheckResult check = telegramApiService.check("TONCRYSTAL", "alexxkk");
        LOG.infov("telegramApiService {0} {1}", check.result, check.user_id);

        broxus();

        transfer(check.user_id.toString(), 0.001);

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
                params.put("freq", 1.0/60);
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
                    double nextPos = (endPos - startPos) * fraction(l+updatePeriod, freq);

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
                    LOG.infov("Broadcast timestamp {0} update {1}, active sessions {2}", l, sceneId, sessions.size());
                }
            }, updatePeriod, updatePeriod);
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
                    LOG.errorv( result.getException(), "Unable to send message: {0}", sceneId);
                }
            });
        });
    }
}
