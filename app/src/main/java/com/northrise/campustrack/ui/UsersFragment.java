package com.northrise.campustrack.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.northrise.campustrack.CampusTrackApp;
import com.northrise.campustrack.databinding.FragmentListBinding;
import com.northrise.campustrack.model.Role;
import com.northrise.campustrack.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsersFragment extends Fragment {
    private FragmentListBinding binding;
    private final List<User> all = new ArrayList<>();
    private final UserAdapter adapter = new UserAdapter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListBinding.inflate(inflater, container, false);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        binding.emptyText.setText("No users match your search.");
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CampusTrackApp.get().repository().database().userDao().observeAll()
                .observe(getViewLifecycleOwner(), users -> {
                    all.clear();
                    all.addAll(users);
                    filter(LoginActivity.text(binding.searchInput.getText()));
                });
    }

    private void filter(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.US);
        List<User> shown = new ArrayList<>();
        for (User user : all) {
            String haystack = (user.name + " " + user.email + " " + Role.label(user.role)).toLowerCase(Locale.US);
            if (needle.isEmpty() || haystack.contains(needle)) {
                shown.add(user);
            }
        }
        adapter.submit(shown);
        binding.emptyText.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
