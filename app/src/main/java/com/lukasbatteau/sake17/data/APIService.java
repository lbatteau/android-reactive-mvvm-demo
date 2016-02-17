package com.lukasbatteau.sake17.data;

import rx.Observable;

/**
 * Created by lbatteau on 12/02/16.
 */
public interface APIService {
    Observable<Boolean> register(String email, String password);
    Observable<Boolean> doesUserExist(String email);
}
