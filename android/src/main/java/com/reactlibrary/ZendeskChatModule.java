package com.reactlibrary;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.zendesk.service.ErrorResponse;
import com.zendesk.service.ZendeskCallback;

import java.util.List;

import androidx.annotation.Nullable;
import zendesk.chat.Account;
import zendesk.chat.AccountProvider;
import zendesk.chat.Chat;
import zendesk.chat.ObservationScope;
import zendesk.chat.Observer;
import zendesk.chat.ProfileProvider;
import zendesk.chat.VisitorInfo;

public class ZendeskChatModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private boolean isAccountInitialized = false;
    private boolean isVisitorInfoSet = false;
    private boolean isAccountFetched = false;
    private ProfileProvider profileProvider = Chat.INSTANCE.providers().profileProvider();
    private AccountProvider accountProvider;
    private ObservationScope observationScope = new ObservationScope();
    private VisitorInfo visitorInfo;

    public ZendeskChatModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "ZendeskChat";
    }

    @ReactMethod
    public void initialize(String accKey, String appID) {

        Chat.INSTANCE.init(this.reactContext, accKey, appID);
        accountProvider = Chat.INSTANCE.providers().accountProvider();
        this.isAccountInitialized = true;
    }

    @ReactMethod
    public void isAccountInitialized(Callback bool) {
        bool.invoke(this.isAccountInitialized);
    }

    public boolean isAccountInitialized() {
        return this.isAccountInitialized;
    }

    @ReactMethod
    public void setVisitorInfo(ReadableMap details, Promise promise) {

        if(this.isAccountInitialized() == false) {
            promise.reject("400", "Account needs to be initialized first");
        } else {
            String name = details.getString("name");
            String email = details.getString("email");
            String phoneNumber = details.getString("phoneNumber");

            if(name instanceof  String && email instanceof String && phoneNumber instanceof String) {

                phoneNumber = phoneNumber.replace("+", "");

                visitorInfo = VisitorInfo.builder()
                        .withName(name)
                        .withEmail(email)
                        .withPhoneNumber(phoneNumber) // numeric string
                        .build();

                profileProvider.setVisitorInfo(visitorInfo, new ZendeskCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        isVisitorInfoSet = true;
                        promise.resolve("Successful");
                    }

                    @Override
                    public void onError(ErrorResponse errorResponse) {
                        promise.reject("400", (WritableMap) errorResponse);
                    }
                });
            } else {
                promise.reject("400", "name, email & phoneNumber required");
            }
        }
    }

    @ReactMethod
    public void isVisitorInfoSet(Callback bool) {
        bool.invoke(this.isVisitorInfoSet);
    }

    public boolean isVisitorInfoSet() {
        return this.isVisitorInfoSet;
    }

    @ReactMethod
    public void getVisitorInfo(Promise promise) {
        if(this.isVisitorInfoSet() == true) {
            promise.resolve(this.profileProvider.getVisitorInfo());
        } else {
            promise.reject("400", "visitor info is not provided");
        }
    }

    @ReactMethod
    public void addVisitorTags(ReadableArray tags, Promise promise) {
        if(this.isVisitorInfoSet() == true) {

            this.profileProvider.addVisitorTags((List<String>) tags, new ZendeskCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    promise.resolve("Successful");
                }

                @Override
                public void onError(ErrorResponse errorResponse) {
                    promise.reject("400", (WritableMap) errorResponse);
                }
            });
        } else {
            promise.reject("400", "visitor info is not provided");
        }
    }

    @ReactMethod
    public void removeVisitorTags(ReadableArray tags, Promise promise) {
        if(this.isVisitorInfoSet() == true) {

            this.profileProvider.removeVisitorTags((List<String>) tags, new ZendeskCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    isVisitorInfoSet = true;
                    promise.resolve("Successful");
                }

                @Override
                public void onError(ErrorResponse errorResponse) {
                    promise.reject("400", (WritableMap) errorResponse);
                }
            });

        } else {
            promise.reject("400", "visitor info is not provided");
        }
    }


    // ======= ACCOUNT INFO ==========

    @ReactMethod
    public void getCachedAccount(Callback onSuccess, Callback onFailure) {
        if(this.isAccountFetched == true) {
            onSuccess.invoke(accountProvider.getAccount());
        } else {
            onFailure.invoke("fetch getAccount() first");
        }
    }

    @ReactMethod
    public void getAccount(Promise promise) {
        accountProvider.getAccount(new ZendeskCallback<Account>() {
            @Override
            public void onSuccess(Account account) {
                isAccountFetched = true;
                promise.resolve(account);
            }

            @Override
            public void onError(ErrorResponse errorResponse) {
                promise.reject("400", (WritableMap) errorResponse);
            }
        });
    }

    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @ReactMethod
    public void observeChatState(Promise promise) {
        accountProvider.observeAccount(observationScope, new Observer<Account>() {
            @Override
            public void update(Account account) {
                sendEvent(reactContext, "ObserveAccountState", (WritableMap) account);
            }
        });
        promise.resolve("Successful");
    }

    @ReactMethod
    public void getDepartments(Promise promise) {

        if(this.isAccountFetched == true) {
            promise.resolve(accountProvider.getAccount().getDepartments());
        } else {
            promise.reject("400", "fetch getAccount() first");
        }
    }

    @ReactMethod
    public void getAccountStatus(Promise promise) {

        if(this.isAccountFetched == true) {
            promise.resolve(accountProvider.getAccount().getStatus());
        } else {
            promise.reject("400", "fetch getAccount() first");
        }
    }

    public boolean isAccountStatusOfline() {

        if(this.isAccountFetched == true) {
            return accountProvider.getAccount().getStatus() == accountProvider.getAccount().getStatus().OFFLINE;
        }

        return true;
    }
}
