package com.lukasbatteau.sake17;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding.widget.RxTextView;
import com.lukasbatteau.sake17.data.APIServiceImpl;

import rx.android.schedulers.AndroidSchedulers;
import rx.android.view.ViewObservable;

/**
 * A login screen that offers login via email/password.
 */
public class RxSignupActivity extends AppCompatActivity {
    public final static String TAG = "RxSignupActivity";

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };

    /* Activity model */
    RxSignupModel mSignupModel;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private Button mEmailSignInButton;
    private View mProgressView;
    private View mLoginFormView;

    private TextInputLayout mEmailInputLayout;
    private TextInputLayout mPasswordInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rx_signup);

        // Reference to view model
        mSignupModel = new RxSignupModel(this);

        // Set service
        mSignupModel.setAPIService(new APIServiceImpl());

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        mEmailInputLayout = (TextInputLayout) findViewById(R.id.email_layout);
        mPasswordInputLayout = (TextInputLayout) findViewById(R.id.password_layout);

        configureEmailField();

        configurePasswordField();

        configureSignupButton();
    }

    private void configureEmailField() {
        mSignupModel.getUserProfileEmails()
                .map(emails -> new ArrayAdapter<>(RxSignupActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emails))
                .subscribe(mEmailView::setAdapter);

        RxTextView.textChanges(mEmailView).subscribe(
                email -> mSignupModel.setEmail(email.toString()));

        mEmailView.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                mSignupModel.setShouldValidateEmail(true);
                mSignupModel.setEmail(mEmailView.getText().toString());
            }
        });

        mSignupModel.getIsEmailValid()
                .skipUntil(mSignupModel.getShouldValidateEmailObservable())
                .subscribe(isValid -> {
                    if (isValid) {
                        mEmailInputLayout.setError(null);
                    } else {
                        mEmailInputLayout.setError(getString(R.string.error_invalid_email));
                    }
                });

        mSignupModel.getUserExists()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        exists -> {
                            if (exists) {
                                mEmailInputLayout.setError(getString(R.string.error_email_exists));
                            } else {
                                mEmailInputLayout.setError(null);
                            }
                        },
                        error -> {
                            Log.e(TAG, error.toString());
                        });
    }

    private void configurePasswordField() {
        RxTextView.textChanges(mPasswordView).subscribe(
                password -> mSignupModel.setPassword(password.toString()));

        mPasswordView.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                mSignupModel.setShouldValidatePassword(true);
                mSignupModel.setPassword(mPasswordView.getText().toString()); // Trigger validation
            }
        });

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    mEmailSignInButton.performClick();
                    return true;
                }
                return false;
            }
        });

        mSignupModel.getIsPasswordValid()
                .skipUntil(mSignupModel.getShouldValidatePasswordObservable())
                .subscribe(isValid -> {
                    if (isValid) {
                        mPasswordInputLayout.setError(null);
                    } else {
                        mPasswordInputLayout.setError(getString(R.string.error_invalid_password));
                    }
                });
    }

    private void configureSignupButton() {
        mSignupModel.getIsSignUpOK()
                .subscribe(mEmailSignInButton::setEnabled);

        ViewObservable.clicks(mEmailSignInButton)
                .doOnNext(event -> showProgress(true))
                .flatMap(event -> mSignupModel.getExecuteSignUp())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(event -> showProgress(false))
                .subscribe(ok -> finish());
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}