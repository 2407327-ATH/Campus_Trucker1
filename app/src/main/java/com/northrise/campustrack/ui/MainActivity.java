package com.northrise.campustrack.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.northrise.campustrack.CampusTrackApp;
import com.northrise.campustrack.R;
import com.northrise.campustrack.data.SessionManager;
import com.northrise.campustrack.databinding.ActivityMainBinding;
import com.northrise.campustrack.model.Role;
import com.northrise.campustrack.util.AppExecutors;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = CampusTrackApp.get().session();
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setSubtitle(Role.label(session.getRole()));
        binding.toolbar.inflateMenu(R.menu.main);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_sync) {
                sync(true);
                return true;
            }
            return false;
        });
        int menu = Role.LECTURER.equals(session.getRole())
                ? R.menu.nav_lecturer
                : Role.ADMIN.equals(session.getRole()) ? R.menu.nav_admin : R.menu.nav_student;
        binding.bottomNav.getMenu().clear();
        binding.bottomNav.inflateMenu(menu);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            show(fragmentFor(item.getItemId()), false);
            return true;
        });
        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    public void openTab(int menuId) {
        binding.bottomNav.setSelectedItemId(menuId);
    }

    public void showAlerts() {
        show(new AlertsFragment(), true);
    }

    private Fragment fragmentFor(int id) {
        if (id == R.id.nav_courses) {
            return CoursesFragment.newInstance(false);
        }
        if (id == R.id.nav_mark) {
            return CoursesFragment.newInstance(true);
        }
        if (id == R.id.nav_history) {
            return new HistoryFragment();
        }
        if (id == R.id.nav_alerts) {
            return new AlertsFragment();
        }
        if (id == R.id.nav_reports) {
            return new ReportsFragment();
        }
        if (id == R.id.nav_users) {
            return new UsersFragment();
        }
        if (id == R.id.nav_profile) {
            return new ProfileFragment();
        }
        return new DashboardFragment();
    }

    private void show(Fragment fragment, boolean backStack) {
        var transaction = getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment);
        if (backStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    private void sync(boolean toast) {
        AppExecutors.io(() -> {
            String message = CampusTrackApp.get().repository().sync();
            if (toast) {
                AppExecutors.main(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
            }
        });
    }
}
