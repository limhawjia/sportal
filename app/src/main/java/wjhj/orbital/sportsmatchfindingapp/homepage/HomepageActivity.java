package wjhj.orbital.sportsmatchfindingapp.homepage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import androidx.lifecycle.ViewModelProviders;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.collect.ImmutableList;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sendbird.android.SendBird;

import java.util.List;

import timber.log.Timber;
import wjhj.orbital.sportsmatchfindingapp.R;
import wjhj.orbital.sportsmatchfindingapp.auth.Authentications;
import wjhj.orbital.sportsmatchfindingapp.auth.LoginActivity;
import wjhj.orbital.sportsmatchfindingapp.databinding.HomepageActivityBinding;
import wjhj.orbital.sportsmatchfindingapp.dialogs.SearchFilterDialogFragment;
import wjhj.orbital.sportsmatchfindingapp.dialogs.SportMultiSelectDialogFragment;
import wjhj.orbital.sportsmatchfindingapp.game.AddGameActivity;
import wjhj.orbital.sportsmatchfindingapp.game.Sport;
import wjhj.orbital.sportsmatchfindingapp.homepage.gamespage.GamesSwipeViewFragment;
import wjhj.orbital.sportsmatchfindingapp.homepage.searchpage.SearchFragment;
import wjhj.orbital.sportsmatchfindingapp.homepage.socialpage.SocialSwipeViewFragment;
import wjhj.orbital.sportsmatchfindingapp.repo.GameSearchFilter;
import wjhj.orbital.sportsmatchfindingapp.repo.SportalRepo;
import wjhj.orbital.sportsmatchfindingapp.user.DisplayUserProfileFragment;
import wjhj.orbital.sportsmatchfindingapp.user.UserPreferencesActivity;
import wjhj.orbital.sportsmatchfindingapp.user.UserProfileViewModel;
import wjhj.orbital.sportsmatchfindingapp.user.UserProfileViewModelFactory;

@SuppressWarnings({"FieldCanBeLocal"})
public class HomepageActivity extends AppCompatActivity implements
        SearchFilterDialogFragment.SearchFilterDialogListener,
        SportMultiSelectDialogFragment.SportMultiSelectDialogListener {

    public static final String CURR_USER_TAG = "current_user";
    public static final String DISPLAY_PROFILE_PIC_TAG = "display_profile_pic";
    public static final String HOMEPAGE_DEBUG = "homepage";
    private static final int ADD_GAME_RC = 1;

    private FirebaseUser currUser;
    @SuppressWarnings({"unused"})
    private UserProfileViewModel userProfileViewModel;
    private HomepageActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(HOMEPAGE_DEBUG, "homepage activity created");

        currUser = FirebaseAuth.getInstance().getCurrentUser();

        checkLoggedIn();
        checkProfileSetup();
        connectToChatClient();

        UserProfileViewModelFactory factory = new UserProfileViewModelFactory(currUser.getUid());
        userProfileViewModel = ViewModelProviders.of(this, factory)
                .get(UserProfileViewModel.class);


        binding = DataBindingUtil.setContentView(this, R.layout.homepage_activity);
        Toolbar toolbar = (Toolbar) binding.topToolbar;
        setSupportActionBar(toolbar);

        binding.bottomNav.setOnNavigationItemSelectedListener(navListener);
        ((Toolbar) binding.topToolbar).setOnMenuItemClickListener(menuListener);
        binding.homepageAddGameButton.setOnClickListener(view -> {
            Intent addGameIntent = new Intent(this, AddGameActivity.class);
            startActivityForResult(addGameIntent, ADD_GAME_RC);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        checkLoggedIn();
        checkProfileSetup();
        connectToChatClient();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectFromChatClient();
    }

    private void checkLoggedIn() {
        if (currUser == null) {
            Toast.makeText(this, "Not logged in. Redirecting...", Toast.LENGTH_SHORT)
                    .show();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
        }
    }

    private void checkProfileSetup() {
        SportalRepo repo = SportalRepo.getInstance();
        repo.isProfileSetUp(currUser.getUid())
                .addOnSuccessListener(this, setup -> {
                    if (!setup) {
                        Toast.makeText(this, "Please setup user profile", Toast.LENGTH_SHORT)
                                .show();
                        Intent profileSetUpIntent = new Intent(this, UserPreferencesActivity.class);
                        startActivity(profileSetUpIntent);
                        finish();
                    }
                });
    }

    private void logOut() {
        Authentications auths = new Authentications();
        auths.logOutFirebase();
        auths.logOutGoogle(this);
        Intent logoutIntent = new Intent(this, LoginActivity.class);
        startActivity(logoutIntent);
        finish();
    }

    private void connectToChatClient() {
        SendBird.connect(currUser.getUid(), (user, e) -> {
            if (e != null) {
                Timber.d(e,"Connection error");
            } else {
                Timber.d("Connected to chat client");
            }
        });
    }

    private void disconnectFromChatClient() {
        SendBird.disconnect(() -> Timber.d("Disconnected from chat client"));
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        Fragment fragment = new Fragment();// IMPLEMENT PROPERLY
        String tag = "";
        switch (item.getItemId()) {
            case R.id.nav_home:
                //todo
                tag = "Home";
                break;
            case R.id.nav_games:
                fragment = GamesSwipeViewFragment.newInstance();
                tag = "Games";
                break;
            case R.id.nav_search:
                ImmutableList.Builder<Sport> builder = new ImmutableList.Builder<>();
                LiveData<ImmutableList<Sport>> source = userProfileViewModel.getSportsPreferences();
                userProfileViewModel.getSportsPreferences()
                        .observe(this, new Observer<ImmutableList<Sport>>() {
                            @Override
                            public void onChanged(ImmutableList<Sport> sports) {
                                builder.addAll(sports);
                                source.removeObserver(this);
                            }
                        });
                fragment = SearchFragment.newInstance(builder.build());
                tag = "Search";
                break;
            case R.id.nav_social:
                tag = "Social";
                fragment = SocialSwipeViewFragment.newInstance(currUser.getUid());
                break;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.homepage_fragment_container, fragment, tag)
                .commit();

        return true;
    };

    private Toolbar.OnMenuItemClickListener menuListener = item -> {
        switch (item.getItemId()) {
            case R.id.options_profile:
                openProfilePage();
                break;
            case R.id.options_logout:
                logOut();
                break;
        }
        return true;
    };

    private void openProfilePage() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        Fragment fragment = manager.findFragmentByTag(DISPLAY_PROFILE_PIC_TAG);

        if (fragment == null) {
            transaction.add(R.id.homepage_secondary_fragment_container,
                    DisplayUserProfileFragment.newInstance(currUser.getUid()),
                    DISPLAY_PROFILE_PIC_TAG)
                    .addToBackStack(null);
        } else {
            transaction.replace(R.id.homepage_secondary_fragment_container,
                    fragment, DISPLAY_PROFILE_PIC_TAG);
        }
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_options_menu, menu);
        return true;
    }

    @Override
    public void onSearchFilterDialogPositiveButtonClicked(GameSearchFilter filters) {
        SearchFragment fragment =
                (SearchFragment) getSupportFragmentManager().findFragmentByTag("Search");
        if (fragment != null) {
            fragment.updateFilterFromSearchFilterDialog(filters);
        }
    }

    @Override
    public void onSportMultiSelectDialogPositiveButtonSelected(List<Sport> selection) {
        SearchFragment fragment =
                (SearchFragment) getSupportFragmentManager().findFragmentByTag("Search");
        if (fragment != null) {
            fragment.updateSports(selection);
        }
    }
}
