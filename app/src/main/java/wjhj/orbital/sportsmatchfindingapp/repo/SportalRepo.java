package wjhj.orbital.sportsmatchfindingapp.repo;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.mapbox.geojson.Point;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wjhj.orbital.sportsmatchfindingapp.game.Game;
import wjhj.orbital.sportsmatchfindingapp.game.GameStatus;
import wjhj.orbital.sportsmatchfindingapp.user.UserProfile;

public class SportalRepo implements ISportalRepo {
    private static final String DATA_DEBUG = "SportalRepo";

    @Override
    public Task<Void> addUser(String uid, UserProfile userProfile) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        UserProfileDataModel dataModel = toUserProfileDataModel(userProfile);

        return db.collection("Users")
                .document(uid)
                .set(dataModel)
                .addOnSuccessListener(aVoid -> Log.d(DATA_DEBUG, uid + " added"))
                .addOnFailureListener(e -> Log.d(DATA_DEBUG, uid + " add failed", e));
    }

    @Override
    public Task<Void> updateUser(String uid, UserProfile userProfile) {
        UserProfileDataModel dataModel = toUserProfileDataModel(userProfile);
        return updateDocument(uid, "Users", dataModel);
    }

    @Override
    public LiveData<UserProfile> getUser(String userUid) {
        LiveData<UserProfileDataModel> dataModelLiveData = convertToLiveData(
                getDocumentFromCollection(userUid, "Users"), UserProfileDataModel.class);

        return Transformations.map(dataModelLiveData, this::toUserProfile);
    }

    //TODO: CHANGE TO LIVE DATA
    @Override
    public Task<List<UserProfile>> selectUsersStartingWith(String field, String queryText) {
        return queryStartingWith("Users", field, queryText)
                .continueWith(task ->
                        toUserProfiles(task.getResult().toObjects(UserProfileDataModel.class)));
    }

    @Override
    public Task<List<UserProfile>> selectUsersArrayContains(String field, String queryText) {
        return queryArrayContains("Users", field, queryText)
                .continueWith(task ->
                        toUserProfiles(task.getResult().toObjects(UserProfileDataModel.class)));
    }

    @Override
    public Task<Void> deleteUser(String userUid) {
        return deleteDocument(userUid, "Users");
    }

    // Should be called when building a game to get the uid for the game.
    @Override
    public String generateGameUid() {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection("Games")
                .document()
                .getId();
    }

    @Override
    public Task<Void> addGame(String gameUid, Game game) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        GameDataModel dataModel = toGameDataModel(game);

        return db.collection("Games")
                .document(gameUid)
                .set(dataModel)
                .addOnSuccessListener(documentReference -> {
                    Log.d(DATA_DEBUG, "Add game complete.");
                    // If game added successfully, also add game to all users participating
                    addGameToUser(game.getCreatorUid(), gameUid);
                    for (String user : game.getParticipatingUids()) {
                        addGameToUser(user, gameUid);
                    }
                })
                .addOnFailureListener(e -> Log.d(DATA_DEBUG, "Add game failed.", e));

    }

    @Override
    public Task<Void> updateGame(String gameId, Game game) {
        GameDataModel dataModel = toGameDataModel(game);
        return updateDocument(gameId, "Games", dataModel);
    }

    @Override
    public LiveData<Game> getGame(String gameID) {
        LiveData<GameDataModel> dataModelLiveData =
                convertToLiveData(getDocumentFromCollection(gameID, "Games"), GameDataModel.class);

        return Transformations.map(dataModelLiveData, this::toGame);
    }

    @Override
    public Task<List<Game>> selectGamesStartingWith(String field, String queryText) {
        return queryStartingWith("Games", field, queryText)
                .continueWith(task -> toGames(task.getResult().toObjects(GameDataModel.class)));
    }

    @Override
    public Task<List<Game>> selectGamesArrayContains(String field, String queryText) {
        return queryArrayContains("Games", field, queryText)
                .continueWith(task -> toGames(task.getResult().toObjects(GameDataModel.class)));
    }

    @Override
    public Task<Void> deleteGame(String gameId) {
        return deleteDocument(gameId, "Games");
    }

    // HELPER METHODS
    private void addGameToUser(String userUid, String gameID) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users")
                .document(userUid)
                .update("games.pending", FieldValue.arrayUnion(gameID))
                .addOnSuccessListener(aVoid -> Log.d(DATA_DEBUG, "Added game to " + userUid))
                .addOnFailureListener(e -> Log.d(DATA_DEBUG, "Add game to " + userUid + " failed", e));
    }

    private Task<Void> updateDocument(String docId, String collectionPath, Object dataModel) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        return db.collection(collectionPath)
                .document(docId)
                .set(dataModel, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(DATA_DEBUG, docId + " updateDocument success"))
                .addOnFailureListener(e -> Log.d(DATA_DEBUG, docId + "updateDocument failure", e));
    }

    private DocumentReference getDocumentFromCollection(String docID, String collectionPath) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        return db.collection(collectionPath)
                .document(docID);
    }

    private <T> LiveData<T> convertToLiveData(DocumentReference docRef, Class<T> valueType) {
        MutableLiveData<T> liveData = new MutableLiveData<>();

        // docRef.get().addOnSuccessListener(res -> liveData.postValue(res.toObject(valueType)));
        docRef.addSnapshotListener((value, err) -> {
            if (err != null) {
                Log.d(DATA_DEBUG, "database snapshot error", err);
            } else if (value.exists()) {
                liveData.postValue(value.toObject(valueType));
            } else {
                Log.d(DATA_DEBUG, "document does not exist");
            }
        });
        return liveData;
    }


    private Task<QuerySnapshot> queryStartingWith(String collection, String field, String queryText) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        return db.collection(collection)
                .orderBy(field)
                .startAt(queryText)
                .endAt(queryText + "\uf8ff") // StackOverflow hacks...
                .get()
                .addOnSuccessListener(snapshot -> Log.d(DATA_DEBUG, snapshot + " retrieved"))
                .addOnFailureListener(e -> Log.d(DATA_DEBUG, "query collection failure", e));
    }

    private Task<QuerySnapshot> queryArrayContains(String collection, String field, String queryText) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        return db.collection(collection)
                .whereArrayContains(field, queryText)
                .get()
                .addOnSuccessListener(snapshot -> Log.d(DATA_DEBUG, snapshot + " retrieved"))
                .addOnFailureListener(e -> Log.d(DATA_DEBUG, "query collection failure", e));
    }

    private Task<Void> deleteDocument(String docID, String collectionPath) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        return db.collection(collectionPath)
                .document(docID)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(DATA_DEBUG, docID + " successfully deleted!"))
                .addOnFailureListener(e -> Log.d(DATA_DEBUG, "Error deleting document", e));
    }

    // METHODS TO CONVERT BETWEEN DOMAIN MODEL AND DATA MODEL
    private UserProfileDataModel toUserProfileDataModel(UserProfile userProfile) {
        return new UserProfileDataModel(userProfile);
    }

    private UserProfile toUserProfile(UserProfileDataModel dataModel) {
        Map<GameStatus, List<String>> newMap = new HashMap<>();
        Map<String, List<String>> oldMap = dataModel.getGames();
        for (String s : oldMap.keySet()) {
            List<String> games = oldMap.get(s);
            if (games != null) {
                newMap.put(GameStatus.fromString(s), games);
            }
        }

        return UserProfile.builder()
                .withDisplayName(dataModel.getDisplayName())
                .withGender(dataModel.getGender())
                .withBirthday(LocalDate.parse(dataModel.getBirthday()))
                .withUid(dataModel.getUid())
                .addAllPreferences(dataModel.getPreferences())
                .putAllGames(newMap)
                .build();
    }

    private List<UserProfile> toUserProfiles(List<UserProfileDataModel> dataModels) {
        List<UserProfile> newList = new ArrayList<>();
        for (UserProfileDataModel dataModel : dataModels) {
            newList.add(toUserProfile(dataModel));
        }
        return newList;
    }

    private GameDataModel toGameDataModel(Game game) {
        return new GameDataModel(game);
    }

    private Game toGame(GameDataModel dataModel) {
        GeoPoint geoPoint = dataModel.getLocation();
        Point location = Point.fromLngLat(geoPoint.getLongitude(), geoPoint.getLatitude());

        return Game.builder()
                .withGameName(dataModel.getGameName())
                .withDescription(dataModel.getDescription())
                .withSport(dataModel.getSport())
                .withLocation(location)
                .withPlaceName(dataModel.getPlaceName())
                .withMinPlayers(dataModel.getMinPlayers())
                .withMaxPlayers(dataModel.getMaxPlayers())
                .withSkillLevel(dataModel.getSkillLevel())
                .withStartTime(LocalDateTime.parse(dataModel.getStartTime()))
                .withEndTime(LocalDateTime.parse(dataModel.getEndTime()))
                .withUid(dataModel.getUid())
                .withCreatorUid(dataModel.getCreatorUid())
                .addAllParticipatingUids(dataModel.getParticipatingUids())
                .build();
    }

    private List<Game> toGames(List<GameDataModel> dataModels) {
        List<Game> newList = new ArrayList<>();
        for (GameDataModel dataModel : dataModels) {
            newList.add(toGame(dataModel));
        }
        return newList;
    }
}
