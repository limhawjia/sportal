package wjhj.orbital.sportsmatchfindingapp.homepage;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import wjhj.orbital.sportsmatchfindingapp.databinding.FragmentGamesTabBinding;
import wjhj.orbital.sportsmatchfindingapp.user.UserProfileViewModel;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GamesTabFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GamesTabFragment extends Fragment {

    private static String GAMES_PAGE_DEBUG = "games page";

    private UserProfileViewModel userProfileViewModel;
    private FragmentGamesTabBinding binding;

    public GamesTabFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment GamesTabFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GamesTabFragment newInstance() {
        return new GamesTabFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(GAMES_PAGE_DEBUG, "Created tab");
        userProfileViewModel = ViewModelProviders.of(getActivity()).get(UserProfileViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGamesTabBinding.inflate(inflater, container, false);
        binding.setUserProfile(userProfileViewModel);
        binding.setLifecycleOwner(getActivity());

        RecyclerView recyclerView = binding.confirmedGamesRecyclerView;
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        GamesCardAdapter adapter = new GamesCardAdapter(userProfileViewModel.getConfirmedGames().getValue());
        recyclerView.setAdapter(adapter);

        binding.testFilterButton.setOnClickListener(view -> adapter.remove());

        return binding.getRoot();
    }
}
