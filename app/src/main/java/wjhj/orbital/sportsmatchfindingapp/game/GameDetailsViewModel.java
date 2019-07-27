package wjhj.orbital.sportsmatchfindingapp.game;

import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.List;
import java.util.Optional;

import wjhj.orbital.sportsmatchfindingapp.repo.SportalRepo;
import wjhj.orbital.sportsmatchfindingapp.user.UserProfile;

public class GameDetailsViewModel extends ViewModel {
    private DateTimeFormatter dateformatter = DateTimeFormatter.ofPattern("dd LLLL yyyy");
    private DateTimeFormatter timeformatter = DateTimeFormatter.ofPattern("HH:mm a");

    private LiveData<Game> mGame;
    private LiveData<List<String>> mParticipants;
    private LiveData<Integer> mNumParticipating;
    private LiveData<Integer> mMaxPlayers;
    private LiveData<Integer> mMinPlayers;
    private MediatorLiveData<Boolean> mReady;
    private LiveData<Sport> mSport;
    private LiveData<String> mDate;
    private LiveData<String> mTime;
    private LiveData<String> mSkillLevel;
    private LiveData<String> mDuration;
    private MediatorLiveData<Integer> mProgress;
    private LiveData<UserProfile> mOwner;
    private LiveData<String> mDescription;
    private LiveData<String> mDayOfWeek;
    private LiveData<String> mDaysLeft;
    private LiveData<String> mOwnerName;
    private LiveData<Uri> mOwnerDisplayUri;
    private LiveData<Boolean> mIsOwner;
    private LiveData<Boolean> mIsParticpant;
    private MutableLiveData<Boolean> mIsDisabled;

    public GameDetailsViewModel(String gameUid) {
        mGame = SportalRepo.getInstance().getGame(gameUid);
        mParticipants = Transformations.map(mGame, Game::getParticipatingUids);
        mNumParticipating = Transformations.map(mParticipants, List::size);
        mMaxPlayers = Transformations.map(mGame, Game::getMaxPlayers);
        mMinPlayers = Transformations.map(mGame, Game::getMinPlayers);
        mReady = new MediatorLiveData<>();
        mReady.addSource(mMinPlayers, min -> {
            Integer numParticipating = mNumParticipating.getValue();
            if (numParticipating != null && numParticipating < min) {
                mReady.setValue(false);
            } else {
                mReady.setValue(true);
            }
        });
        mReady.addSource(mNumParticipating, num -> {
            Integer minPlayers = mMinPlayers.getValue();
            if (minPlayers != null && num >= minPlayers) {
                mReady.setValue(true);
            } else {
                mReady.setValue(false);
            }
        });
        mSport = Transformations.map(mGame, Game::getSport);
        mDate = Transformations.map(mGame, game -> dateformatter.format(game.getDate()));
        mTime = Transformations.map(mGame, game -> timeformatter.format(game.getTime()));
        mDuration = Transformations.map(mGame, game -> toDurationString(game.getDuration()));
        mSkillLevel = Transformations.map(mGame, game -> game.getSkillLevel().toString());
        mProgress = new MediatorLiveData<>();
        mProgress.addSource(mNumParticipating, num -> {
            Integer min = mMinPlayers.getValue();
            if (min != null) {
                int progress = num * 100 / min;
                mProgress.setValue(progress);
            } else {
                mProgress.setValue(0);
            }
        });
        mProgress.addSource(mMinPlayers, min -> {
            Integer num = mNumParticipating.getValue();
            if (num != null) {
                int progress = num * 100 / min;
                mProgress.setValue(progress);
            } else {
                mProgress.setValue(0);
            }
        });
        mOwner = Transformations
                .switchMap(mGame, game -> SportalRepo.getInstance().getUser(game.getCreatorUid()));
        mDescription = Transformations.map(mGame, game -> game.getDescription().or(""));
        mDayOfWeek = Transformations.map(mGame, game -> game.getDate().getDayOfWeek().toString());
        mDaysLeft = Transformations.map(mGame, game -> getDaysLeft(game.getDate()));
        mOwnerName = Transformations.map(mOwner, UserProfile::getDisplayName);
        mOwnerDisplayUri = Transformations.map(mOwner, UserProfile::getDisplayPicUri);
        mIsOwner = Transformations.map(mOwner, owner -> {
            return FirebaseAuth.getInstance().getUid().equals(owner.getUid());
        });
        mIsParticpant = Transformations.map(mParticipants, participants -> {
            return participants.contains(FirebaseAuth.getInstance().getUid());
        });
        mIsDisabled = new MutableLiveData<>();
        mIsDisabled.setValue(false);
    }

    public LiveData<Game> getGame() {
        return mGame;
    }

    public LiveData<List<String>> getParticipants() {
        return mParticipants;
    }

    public LiveData<Integer> getParticipating() {
        return mNumParticipating;
    }

    public LiveData<Integer> getMaxPlayers() {
        return mMaxPlayers;
    }

    public LiveData<Integer> getMinPlayers() {
        return mMinPlayers;
    }

    public LiveData<Boolean> getReady() {
        return mReady;
    }

    public LiveData<Sport> getSport() { return mSport; }

    public LiveData<String> getDate() { return mDate; }

    public LiveData<String> getTime() { return mTime ;}

    public LiveData<String> getDuration() { return mDuration; }

    public LiveData<String> getSkillLevel() { return mSkillLevel; }

    public LiveData<Integer> getProgress() {
        return mProgress;
    }

    public LiveData<UserProfile> getOwner() {
        return mOwner;
    }

    public LiveData<String> getDaysLeft() {
        return mDaysLeft;
    }

    public LiveData<String> getDescription() {
        return mDescription;
    }

    public LiveData<String> getDayOfWeek() {
        return mDayOfWeek;
    }

    public LiveData<String> getOwnerName() { return mOwnerName; }

    public LiveData<Uri> getOwnerDisplayUri() {
        return mOwnerDisplayUri;
    }

    public LiveData<Boolean> getIsOwner() {
        return mIsOwner;
    }

    public LiveData<Boolean> getIsParticipant() {
        return mIsParticpant;
    }

    private String toDurationString(Duration duration) {
        if (duration == null) {
            return null;
        }
        long hours = duration.getSeconds() / 3600;
        long minutes = (duration.getSeconds() % 3600) / 60;
        return hours + " h  " + minutes + " m";
    }

    private String getDaysLeft(LocalDate date) {
        long daysLeft = LocalDate.now().until(date, ChronoUnit.DAYS);
        if (daysLeft == 0) {
            return "TODAY";
        } else {
            return daysLeft + " DAYS LEFT";
        }
    }

    private void onButtonClicked() {
        Boolean isParticipant = mIsParticpant.getValue();
        String gameUid = mGame.getValue().getUid();
        if (gameUid != null) {
            if (isParticipant != null) {
                mIsDisabled.setValue(true);
                SportalRepo.getInstance().addUserToGame(FirebaseAuth.getInstance().getUid(), gameUid)
                        .addOnCompleteListener(x -> mIsDisabled.setValue(false));
            } else {
                mIsDisabled.setValue(true);
            }
        }
    }
}
