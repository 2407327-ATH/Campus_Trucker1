package com.northrise.campustrack.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.northrise.campustrack.CampusTrackApp;
import com.northrise.campustrack.data.repo.AuthResult;
import com.northrise.campustrack.data.repo.CampusRepository;
import com.northrise.campustrack.util.AppExecutors;

public class AuthViewModel extends AndroidViewModel {
    private final CampusRepository repository;
    private final MutableLiveData<AuthResult> loginResult = new MutableLiveData<>();
    private final MutableLiveData<AuthResult> registerResult = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = ((CampusTrackApp) application).repository();
    }

    public LiveData<AuthResult> loginResult() {
        return loginResult;
    }

    public LiveData<AuthResult> registerResult() {
        return registerResult;
    }

    public void login(String email, String password) {
        loginResult.setValue(AuthResult.loading());
        AppExecutors.io(() -> loginResult.postValue(repository.login(email, password)));
    }

    public void register(String name, String email, String password, String confirm, String role, String studentNumber) {
        registerResult.setValue(AuthResult.loading());
        AppExecutors.io(() -> registerResult.postValue(
                repository.register(name, email, password, confirm, role, studentNumber)
        ));
    }
}
