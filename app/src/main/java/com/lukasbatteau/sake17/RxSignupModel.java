package com.lukasbatteau.sake17;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import com.lukasbatteau.sake17.data.APIService;

import java.util.List;
import java.util.regex.Pattern;

import rx.Observable;
import rx.android.content.ContentObservable;
import rx.subjects.BehaviorSubject;

/**
 * Created by lbatteau on 12/02/16.
 */
public class RxSignupModel {
    public final static String TAG = "RxSignupModel";

    public static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\._%\\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{1,25})+"
    );

    // Reactive properties
    BehaviorSubject<String> mEmail = BehaviorSubject.create();
    BehaviorSubject<String> mPassword = BehaviorSubject.create();

    BehaviorSubject<Boolean> mShouldValidateEmail = BehaviorSubject.create();
    BehaviorSubject<Boolean> mShouldValidatePassword = BehaviorSubject.create();

    Observable<Boolean> mExecuteSignUp;
    Observable<Boolean> mUserExists;
    Observable<Boolean> mIsEmailValid;
    Observable<Boolean> mIsPasswordValid;
    Observable<Boolean> mIsSignUpOK;
    Observable<List<String>> mUserProfileEmails;

    // Service for API calls
    APIService mAPIService;

    // Activity context
    Context mContext;

    public RxSignupModel(Context context) {
        mContext = context;

        mIsEmailValid = mEmail
                .map(this::isEmailValid)
                .doOnNext(valid -> Log.d(TAG, "Email valid: " + valid));

        mIsPasswordValid = mPassword
                .map(this::isPasswordValid)
                .doOnNext(valid -> Log.d(TAG, "Password valid: " + valid));

        mUserExists = Observable.defer(() -> mEmail
                .filter(this::isEmailValid)
                .flatMap(mAPIService::doesUserExist))
                .doOnNext(exists -> Log.d(TAG, "User exists: " + exists));

        mIsSignUpOK = Observable.combineLatest(mIsEmailValid, mIsPasswordValid, mUserExists,
                (isEmailValid, isPasswordValid, userExists) ->
                        isEmailValid && isPasswordValid && !userExists)
                .startWith(false)
                .doOnNext(ok -> Log.d(TAG, "Signup OK: " + ok));

        mExecuteSignUp = Observable.defer(() ->
                mIsSignUpOK
                        .filter(isEnabled -> isEnabled)
                        .flatMap(isEnabled -> mAPIService.register(getEmail(), getPassword())));

        mUserProfileEmails = Observable.defer(this::loadEmails);
    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    private Observable<List<String>> loadEmails() {
        return ContentObservable.fromCursor(queryEmails())
                .map(cursor -> cursor.getString(ProfileQuery.ADDRESS))
                .toList();
    }

    private Cursor queryEmails() {
        return mContext.getContentResolver().query(
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(
                        ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY
                ),
                ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                        .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    private boolean isEmailValid(String email) {
        return EMAIL_ADDRESS_PATTERN.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    public String getEmail() {
        return mEmail.getValue();
    }

    public void setEmail(String email) {
        mEmail.onNext(email);
    }

    public String getPassword() {
        return mPassword.getValue();
    }

    public void setPassword(String password) {
        mPassword.onNext(password);
    }

    public Observable<Boolean> getShouldValidateEmailObservable() {
        return mShouldValidateEmail.asObservable();
    }

    public boolean getShouldValidateEmail() {
        return mShouldValidateEmail.getValue();
    }

    public void setShouldValidateEmail(boolean validateEmail) {
        mShouldValidateEmail.onNext(validateEmail);
    }

    public boolean getShouldValidatePassword() {
        return mShouldValidatePassword.getValue();
    }

    public Observable<Boolean> getShouldValidatePasswordObservable() {
        return mShouldValidatePassword.asObservable();
    }

    public void setShouldValidatePassword(boolean validatePassword) {
        mShouldValidatePassword.onNext(validatePassword);
    }

    public Observable<Boolean> getIsEmailValid() {
        return mIsEmailValid;
    }

    public Observable<Boolean> getIsPasswordValid() {
        return mIsPasswordValid;
    }

    public Observable<Boolean> getIsSignUpOK() {
        return mIsSignUpOK;
    }

    public Observable<Boolean> getExecuteSignUp() {
        return mExecuteSignUp;
    }

    public Observable<Boolean> getUserExists() {
        return mUserExists;
    }

    public APIService getAPIService() {
        return mAPIService;
    }

    public void setAPIService(APIService APIService) {
        mAPIService = APIService;
    }

    public Observable<List<String>> getUserProfileEmails() {
        return mUserProfileEmails;
    }
}
