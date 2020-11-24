package com.dappcraft;

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
import java.util.ArrayList;
import java.util.List;

@Singleton
public class Store {
    Firestore db;

    private static final Logger LOG = Logger.getLogger(Store.class);

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

    public Timestamp write(String collection, String userName, ScoreResult score) {
        try {
            DocumentReference docRef = db.collection(collection).document(userName);
            ApiFuture<WriteResult> result = docRef.set(score);
            return result.get().getUpdateTime();
        } catch (Exception e) {
            LOG.error("Firestore write", e);
            return null;
        }
    }

    public List<ScoreResult> getResults(String collection) {
        try {
            Query query = db.collection(collection).orderBy("score", Query.Direction.DESCENDING).limit(10);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
            List<ScoreResult> res = new ArrayList<>();
            for (QueryDocumentSnapshot document : documents) {
                ScoreResult scoreResult = new ScoreResult(document.getLong("score"), document.getLong("level"), document.getLong("kills"));
                scoreResult.setUserName(document.getId());
                res.add(scoreResult);
            }
            return res;
        } catch (Exception e) {
            LOG.error("Firestore write", e);
            return new ArrayList<>();
        }
    }

    public List<ScoreResult> saveScore(String collection, String userName, ScoreResult score) {
        List<ScoreResult> results = getResults(collection);

        boolean foundUser = false;

        for (ScoreResult result : results) {
            if (result.getUserName().equals(userName)) {
                foundUser = true;
                if (result.getScore() < score.getScore()) {
                    write(collection, userName, score);
                    result.setScore(score.getScore());
                    result.setLevel(score.getLevel());
                    result.setKills(score.getKills());
                }
                break;
            }
        }
        if (!foundUser) {
            write(collection, userName, score);
            results = getResults(collection);
        }
        return results;
    }
}
