package com.dappcraft;

import com.dappcraft.db.Prize;
import com.dappcraft.db.UserInfo;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.jboss.logging.Logger;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Singleton
public class Store {
    Firestore db;
    private static final Logger LOG = Logger.getLogger(Store.class);

    String prizeCollection = "ton-airdrop-prizes";
    String userCollection = "ton-airdrop-users";

    Store() {
        try {
            connect();
        } catch (IOException e) {
            LOG.error("Firestore connect", e);
        }
    }

    public void connect() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream serviceAccount = classloader.getResourceAsStream("cloud_keys/google_cloud_dcl.json");
        if (serviceAccount == null) {
            LOG.error("serviceAccount not found");
            return;
        }
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
        FirebaseOptions options = FirebaseOptions.builder()
                .setProjectId("imperial-terra-174810")
                .setCredentials(credentials)
                .build();
        db = FirestoreClient.getFirestore(FirebaseApp.initializeApp(options, "imperial-terra-174810"));
        LOG.info("Firestore connected");
    }

    public Timestamp updateUser(String id, UserInfo info) {
        try {
            DocumentReference docRef = db.collection(userCollection).document(id);
            ApiFuture<WriteResult> result = docRef.set(info);
            return result.get().getUpdateTime();
        } catch (Exception e) {
            LOG.error("Firestore write", e);
            return null;
        }
    }

    public UserInfo getUser(String id) {
        try {
            DocumentReference docRef = db.collection(userCollection).document(id);
            ApiFuture<DocumentSnapshot> result = docRef.get();
            return result.get().toObject(UserInfo.class);
        } catch (Exception e) {
            LOG.error("Firestore get", e);
            return null;
        }
    }

    public Timestamp updatePrize(String id, Prize info) {
        try {
            DocumentReference docRef = db.collection(prizeCollection).document(id);
            ApiFuture<WriteResult> result = docRef.set(info);
            return result.get().getUpdateTime();
        } catch (Exception e) {
            LOG.error("Firestore write", e);
            return null;
        }
    }

    public Prize getPrize(String id) {
        try {
            DocumentReference docRef = db.collection(prizeCollection).document(id);
            ApiFuture<DocumentSnapshot> result = docRef.get();
            return result.get().toObject(Prize.class);
        } catch (Exception e) {
            LOG.error("Firestore get", e);
            return null;
        }
    }

    public String randomPrize(String userId) {
        try {
            final CollectionReference prizeCollectionRef = db.collection(prizeCollection);
            final DocumentReference userDocRef = db.collection(userCollection).document(userId);
            ApiFuture<String> futureTransaction = db.runTransaction(transaction -> {
                DocumentSnapshot userDocSnapshot = transaction.get(userDocRef).get();
                UserInfo user = userDocSnapshot.toObject(UserInfo.class);

                if (user.getJoinGroup() && !user.getRewardClaimed() && user.getReward() == null && user.getTelegramId() != null) {
                    ApiFuture<QuerySnapshot> querySnapshotApiFuture = transaction.get(prizeCollectionRef.limit(100));

                    Map<DocumentReference, Prize> results = new HashMap<>();
                    List<QueryDocumentSnapshot> documents = querySnapshotApiFuture.get().getDocuments();
                    for (QueryDocumentSnapshot document : documents) {
                        results.put(document.getReference(), document.toObject(Prize.class));
                    }
                    int allPrizeCount = 0;
                    for (Prize pr : results.values()) {
                        allPrizeCount += pr.getCount();
                    }

                    if (allPrizeCount <= 0) {
                        throw new Exception("Sorry! No prizes.");
                    }

                    int nextInt = new Random().nextInt(allPrizeCount);
                    int sumCount = 0;
                    DocumentReference resultPrizeId = null;
                    for (Map.Entry<DocumentReference, Prize> pr : results.entrySet()) {
                        int prizeCount = pr.getValue().getCount().intValue();
                        sumCount += prizeCount;
                        if (prizeCount > 0 && sumCount > nextInt) {
                            resultPrizeId = pr.getKey();
                            break;
                        }
                    }

                    if (resultPrizeId != null) {
                        Prize resultPrize = results.get(resultPrizeId);
                        Double randomReward = resultPrize.getAmount();
                        user.setReward(randomReward);
                        resultPrize.setCount(resultPrize.getCount() - 1);

                        transaction.set(userDocRef, user);
                        transaction.set(resultPrizeId, resultPrize);
                        return resultPrizeId.getId();
                    } else {
                        throw new Exception("Error in random selection: " + allPrizeCount + "; " + nextInt);
                    }
                } else {
                    throw new Exception("User do not complete all rules for reward");
                }
            });
            return futureTransaction.get();
        } catch (Exception e) {
            LOG.error("randomPrize", e);
            return null;
        }
    }

    public boolean findDuplicatesTelegram(String userId) {
        try {
            UserInfo user = getUser(userId);
            String telegramId = user.getTelegramId();
            if (telegramId == null) return true;
            ApiFuture<QuerySnapshot> future = db.collection(userCollection).whereEqualTo("telegramId", telegramId).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            if (documents.size() <= 1) return false;

            LOG.errorv("Found users with same Telegram Id {0} for user Id {1}", telegramId, userId);
            for (QueryDocumentSnapshot document : documents) {
                UserInfo userInfo = document.toObject(UserInfo.class);
                LOG.errorv("Duplicates Telegram Id {0}: User Id {1}", telegramId, document.getId());
                if (userInfo.getRewardClaimed()) return true;
                if (userInfo.getReward() != null) return true;
            }
            return false;
        } catch (Exception e) {
            LOG.error("Firestore findDuplicatesTelegram", e);
            return true;
        }
    }

    public Map<String, Prize> getPrizes() {
        try {
            Map<String, Prize> results = new HashMap<>();
            ApiFuture<QuerySnapshot> future = db.collection(prizeCollection).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            for (QueryDocumentSnapshot document : documents) {
                results.put(document.getId(), document.toObject(Prize.class));
            }
            return results;
        } catch (Exception e) {
            LOG.error("Firestore get", e);
            return new HashMap<>();
        }
    }
}
