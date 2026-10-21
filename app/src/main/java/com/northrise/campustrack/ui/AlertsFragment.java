package com.northrise.campustrack.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.northrise.campustrack.CampusTrackApp;
import com.northrise.campustrack.databinding.FragmentListBinding;
import com.northrise.campustrack.util.AppExecutors;

public class AlertsFragment extends Fragment {
    private FragmentListBinding binding;
    private final AlertAdapter adapter = new AlertAdapter(notification -> AppExecutors.io(() ->
            CampusTrackApp.get().repository().markNotificationRead(notification.id)));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListBinding.inflate(inflater, container, false);
        binding.searchInput.setVisibility(View.GONE);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        binding.emptyText.setText("No alerts. You will see one when attendance drops below 75%.");
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        long userId = CampusTrackApp.get().session().getUserId();
        CampusTrackApp.get().repository().database().notificationDao()
                .observeForUser(userId)
                .observe(getViewLifecycleOwner(), notifications -> {
                    adapter.submit(notifications);
                    binding.emptyText.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }
}
