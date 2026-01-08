package com.liskovsoft.mediaserviceinterfaces;

import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import io.reactivex.Observable;

import java.util.List;

public interface SignInService {
    interface OnAccountChange {
        void onAccountChanged(Account account);
    }
    boolean isSigned();
    List<Account> getAccounts();
    Account getSelectedAccount();
    void addOnAccountChange(OnAccountChange listener);
    void selectAccount(Account account);
    void removeAccount(Account account);
    String printDebugInfo();

     
     
    Observable<String> signInObserve();
}
