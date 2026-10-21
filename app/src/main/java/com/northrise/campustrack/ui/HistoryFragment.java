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

import com.google.android.material.chip.ChipGroup;
import com.northrise.campustrack.CampusTrackApp;
import com.northrise.campustrack.R;
import com.northrise.campustrack.databinding.FragmentListBinding;
import com.northrise.campustrack.model.HistoryRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {
    private FragmentListBinding binding;
    private final List<HistoryRow> all = new ArrayList<>();
    private final HistoryAdapter adapter = new HistoryAdapter();
    private String status = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListBinding.inflate(inflater, container, false);
        binding.chipGroup.setVisibility(View.VISIBLE);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        binding.emptyText.setText("No attendance records yet.");
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        binding.chipGroup.setOnCheckedStateChangeListener((ChipGroup group, List<Integer> checkedIds) -> {
            int id = checkedIds.isEmpty() ? R.id.chipAll : checkedIds.get(0);
            if (id == R.id.chipPresent) status = "PRESENT";
            else if (id == R.id.chipLate) status = "LATE";
            else if (id == R.id.chipAbsent) status = "ABSENT";
            else status = "ALL";
            filter();
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        long userId = CampusTrackApp.get().session().getUserId();
        CampusTrackApp.get().repository().database().reportDao()
                .observeHistory(userId)
                .observe(getViewLifecycleOwner(), rows -> {
                    all.clear();
                    all.addAll(rows);
                    filter();
                });
    }

    private void filter() {
        String needle = LoginActivity.text(binding.searchInput.getText()).trim().toLowerCase(Locale.US);
        List<HistoryRow> shown = new ArrayList<>();
        for (HistoryRow row : all) {
            boolean statusOk = "ALL".equals(status) || status.equals(row.status);
            String haystack = (row.courseCode + " " + row.courseTitle).toLowerCase(Locale.US);
            if (statusOk && (needle.isEmpty() || haystack.contains(needle))) {
                shown.add(row);
            }
        }
        adapter.submit(shown);
        binding.emptyText.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
