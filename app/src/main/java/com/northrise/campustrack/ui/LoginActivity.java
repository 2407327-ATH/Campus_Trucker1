package com.northrise.campustrack.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.northrise.campustrack.data.repo.AuthResult;
import com.northrise.campustrack.databinding.ActivityLoginBinding;
import com.northrise.campustrack.viewmodel.AuthViewModel;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private boolean opened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AuthViewModel model = new ViewModelProvider(this).get(AuthViewModel.class);
        binding.loginButton.setOnClickListener(v -> model.login(
                text(binding.emailInput.getText()),
                text(binding.passwordInput.getText())
        ));
        binding.registerLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        model.loginResult().observe(this, this::render);
    }

    private void render(AuthResult result) {
        if (result == null) {
            return;
        }
        boolean loading = result.state == AuthResult.State.LOADING;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!loading);
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

    static String text(CharSequence value) {
        return value == null ? "" : value.toString();
    }
}
