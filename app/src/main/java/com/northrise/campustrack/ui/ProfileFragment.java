package com.northrise.campustrack.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.northrise.campustrack.CampusTrackApp;
import com.northrise.campustrack.data.SessionManager;
import com.northrise.campustrack.databinding.FragmentProfileBinding;
import com.northrise.campustrack.model.Role;
import com.northrise.campustrack.util.AppExecutors;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        SessionManager session = CampusTrackApp.get().session();
        binding.nameText.setText(session.getName());
        binding.emailText.setText(session.getEmail());
        binding.roleText.setText(Role.label(session.getRole()));
        if (session.getStudentNumber() != null && !session.getStudentNumber().isEmpty()) {
            binding.studentText.setVisibility(View.VISIBLE);
            binding.studentText.setText("Student number " + session.getStudentNumber());
        }
        binding.statusText.setText(session.getLastSync());
        binding.syncButton.setOnClickListener(v -> AppExecutors.io(() -> {
            String message = CampusTrackApp.get().repository().sync();
            AppExecutors.main(() -> {
                if (!isAdded()) {
                    return;
                }
                binding.statusText.setText(message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            });
        }));
        binding.alertsButton.setOnClickListener(v -> ((MainActivity) requireActivity()).showAlerts());
        binding.logoutButton.setOnClickListener(v -> {
            CampusTrackApp.get().session().clear();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        return binding.getRoot();
    }
}
