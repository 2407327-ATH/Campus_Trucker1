package com.northrise.campustrack;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.northrise.campustrack.ui.SplashActivity;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StudentLoginFlowTest {
    @Rule
    public ActivityScenarioRule<SplashActivity> rule = new ActivityScenarioRule<>(SplashActivity.class);

    @Test
    public void seededStudentSeesLowAttendance() {
        onView(isRoot()).perform(waitMs(2500));
        onView(withId(R.id.emailInput)).perform(replaceText("student@nu.ac.zm"), closeSoftKeyboard());
        onView(withId(R.id.passwordInput)).perform(replaceText("Student@123"), closeSoftKeyboard());
        onView(withId(R.id.loginButton)).perform(ViewActions.click());
        onView(isRoot()).perform(waitMs(6000));
        onView(withText("Hello, Chipo Phiri")).check(matches(isDisplayed()));
        onView(withText("57.1%")).check(matches(isDisplayed()));
        onView(withText("1 course(s) are below the 75% minimum.")).check(matches(isDisplayed()));
    }

    private static ViewAction waitMs(long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait " + millis + "ms";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }
}
