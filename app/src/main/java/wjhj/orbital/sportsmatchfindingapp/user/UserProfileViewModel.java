package wjhj.orbital.sportsmatchfindingapp.user;

import android.location.Location;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import wjhj.orbital.sportsmatchfindingapp.game.Difficulty;
import wjhj.orbital.sportsmatchfindingapp.game.Game;
import wjhj.orbital.sportsmatchfindingapp.game.GameStatus;
import wjhj.orbital.sportsmatchfindingapp.game.Sport;
import wjhj.orbital.sportsmatchfindingapp.repo.SportalRepo;

public class UserProfileViewModel extends ViewModel {

    private SportalRepo repo;
    private UserProfile currUser;

    public UserProfileViewModel(String userUid) {
        repo = new SportalRepo();
        String id =repo.generateGameUid();
        Game game = Game.builder()
                .withGameName("Frisbee at UTOWN")
                .withDescription("6pm 6/7/2019 be there or be square")
                .withSport(Sport.FRISBEE)
                .withLocation(new Location(""))
                .withMinPlayers(10)
                .withMaxPlayers(18)
                .withSkillLevel(Difficulty.BEGINNER)
                .withStartTime(LocalDateTime.now())
                .withEndTime(LocalDateTime.of(2019, 7, 6, 18, 0))
                .withUid(id)
                .build();


        repo.updateGame(id, game.withParticipatingUids(userUid));

        repo.getUser(userUid)
            .addOnSuccessListener(userProfile -> currUser = userProfile);
    }

    public UserProfile getCurrUser() {
        return currUser;
    }


    public MutableLiveData<List<Game>> getConfirmedGames() {
        //TESTS
        List<Game> games = new ArrayList<>();
        Game game1 = Game.builder()
                .withGameName("SOCCER :D")
                .withDescription("TMR 6pm OR DIE")
                .withSport(Sport.BADMINTON)
                .withLocation(new Location(""))
                .withMinPlayers(4)
                .withMaxPlayers(6)
                .withSkillLevel(Difficulty.INTERMEDIATE)
                .withStartTime(LocalDateTime.now())
                .withEndTime(LocalDateTime.of(2019, 7, 1, 12, 12))
                .withUid("iamgame")
                .addParticipatingUids("123123", "1231", "531")
                .build();

        Game game2 = game1.withGameName("BLAA")
                .withDescription("Efef3ff");

        Game game3 = game2.withGameName("PEWWWW")
                .withEndTime(LocalDateTime.now());

        games.add(game1);
        games.add(game2);
        games.add(game3);
        games.add(game1);
        games.add(game2);
        games.add(game3);
        games.add(game1);
        games.add(game2);
        games.add(game3);

        MutableLiveData<List<Game>> liveData = new MutableLiveData<>();
        liveData.setValue(games);
        MutableLiveData<List<Game>> confirmedGames = liveData;

        return confirmedGames;
    }

}
