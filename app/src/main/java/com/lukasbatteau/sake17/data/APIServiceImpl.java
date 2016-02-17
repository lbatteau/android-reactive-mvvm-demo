package com.lukasbatteau.sake17.data;

import java.util.concurrent.TimeUnit;

import rx.Observable;

/**
 * Created by lbatteau on 12/02/16.
 */
public class APIServiceImpl implements APIService {
    @Override
    public Observable<Boolean> register(String email, String password) {
        return Observable.just(true).delay(2, TimeUnit.SECONDS);
    }

    @Override
    public Observable<Boolean> doesUserExist(String email) {
        return Observable.just(email)
                .map(s -> s.equals("foo@example.com"))
                .delay(250, TimeUnit.MILLISECONDS);
    }
}
