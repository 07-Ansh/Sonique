package com.liskovsoft.mediaserviceinterfaces.oauth;

import java.util.List;

import io.reactivex.Observable;

public interface SignInService {
    void signOut();
    boolean isSigned();
    List<Account> getAccounts();
    Account getSelectedAccount();
    void selectAccount(Account account);
    void removeAccount(Account account);
    void setOnChange(Runnable onChange);

     
     
    Observable<SignInCode> signInObserve();
    Observable<Void> signOutObserve();
    Observable<Boolean> isSignedObserve();
    Observable<List<Account>> getAccountsObserve();
}
