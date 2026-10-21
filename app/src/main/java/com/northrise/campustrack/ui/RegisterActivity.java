package com.northrise.campustrack.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.northrise.campustrack.R;
import com.northrise.campustrack.data.repo.AuthResult;
import com.northrise.campustrack.databinding.ActivityRegisterBinding;
import com.northrise.campustrack.model.Role;
import com.northrise.campustrack.viewmodel.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private boolean opened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.roleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                binding.studentLayout.setVisibility(checkedId == R.id.roleStudent ? View.VISIBLE : View.GONE);
            }
        });
        AuthViewModel model = new ViewModelProvider(this).get(AuthViewModel.class);
        binding.registerButton.setOnClickListener(v -> model.register(
                LoginActivity.text(binding.nameInput.getText()),
                LoginActivity.text(binding.emailInput.getText()),
                LoginActivity.text(binding.passwordInput.getText()),
                LoginActivity.text(binding.confirmInput.getText()),
                selectedRole(),
                LoginActivity.text(binding.studentInput.getText())
        ));
        binding.loginLink.setOnClickListener(v -> finish());
        model.registerResult().observe(this, this::render);
    }

    private void render(AuthResult result) {
        if (result == null) {
            return;
        }
        boolean loading = result.state == AuthResult.State.LOADING;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.registerButton.setEnabled(!loading);
        if (result.state == AuthResult.State.ERROR) {
            binding.errorText.setText(result.message);
            binding.errorText.setVisibility(View.VISIBLE);
        } else {
            binding.errorText.setVisibility(View.GONE);
        }
        if (result.state == AuthResult.State.SUCCESS && !opened) {
            opened = true;
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    private String selectedRole() {
        int checked = binding.roleGroup.getCheckedButtonId();
        if (checked == R.id.roleLecturer) {
            return Role.LECTURER;
        }
        if (checked == R.id.roleAdmin) {
            return Role.ADMIN;
        }
        return Role.STUDENT;
    }
}
