package com.reactlibrary;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.zendesk.service.ErrorResponse;
import com.zendesk.service.ZendeskCallback;

import java.io.File;

import androidx.annotation.Nullable;
import zendesk.chat.Chat;
import zendesk.chat.ChatProvider;
import zendesk.chat.ChatRating;
import zendesk.chat.ChatState;
import zendesk.chat.ObservationScope;
import zendesk.chat.Observer;
import zendesk.chat.OfflineForm;

public class ZendeskChatChatModule extends ZendeskChatModule {

    private final ReactApplicationContext reactContext;
    private boolean isDepartmentSet = false;
    private boolean isSessionStarted = false;
    private ChatProvider chatProvider = Chat.INSTANCE.providers().chatProvider();;
    private ObservationScope observationScope = new ObservationScope();
    private ChatState chatState;

    public ZendeskChatChatModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @ReactMethod
    public void setDepartment(String name, Promise promise) {

        if(super.isVisitorInfoSet()) {
            if(chatState.isChatting() == true) {
                chatProvider.setDepartment("Department name", new ZendeskCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        isDepartmentSet = true;
                        promise.resolve("Successful");
                    }

                    @Override
                    public void onError(ErrorResponse errorResponse) {
                        promise.reject("400", (WritableMap) errorResponse);
                    }
                });
            } else {
                promise.reject("400", "can't change department while chatting");
            }
        } else {
            promise.reject("400", "visitor info is not provided");
        }
    }

    @ReactMethod
    public void setDepartment(double departmentID, Promise promise) {

        if(super.isVisitorInfoSet()) {

            if(chatState.isChatting() == true) {
                chatProvider.setDepartment((long) departmentID, new ZendeskCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        isDepartmentSet = true;
                        promise.resolve("Successful");
                    }

                    @Override
                    public void onError(ErrorResponse errorResponse) {
                        promise.reject("400", (WritableMap) errorResponse);
                    }
                });
            } else {
                promise.reject("400", "can't change department while chatting");
            }
        } else {
            promise.reject("400", "visitor info is not provided");
        }
    }

    @ReactMethod
    public void isDepartmentSet(Callback callback) {
        callback.invoke(this.isDepartmentSet);
    }

    public boolean isDepartmentSet() {
        return this.isDepartmentSet;
    }

    @ReactMethod
    public void clearDepartment(Promise promise) {
        if(super.isVisitorInfoSet()) {
            if(chatState.isChatting() == true) {
                chatProvider.clearDepartment(new ZendeskCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        isDepartmentSet = false;
                        promise.resolve("Successful");
                    }

                    @Override
                    public void onError(ErrorResponse errorResponse) {
                        promise.reject("400", (WritableMap) errorResponse);
                    }
                });
            } else {
                promise.reject("400", "can't change department while chatting");
            }
        } else {
            promise.reject("400", "visitor info is not provided");
        }
    }

    @ReactMethod
    public void sendRequestChat(Promise promise) {
        chatProvider.requestChat();
        promise.resolve("Successful");
    }


    @ReactMethod
    public void sendChatRating(String rating, Promise promise) {

        ChatRating rate;
        if(rating == "good" || rating == "GOOD") {
            rate = ChatRating.GOOD;
        } else {
             rate = ChatRating.BAD;
        }

        chatProvider.sendChatRating(rate, new ZendeskCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                isDepartmentSet = false;
                promise.resolve("Successful");
            }

            @Override
            public void onError(ErrorResponse errorResponse) {
                promise.reject("400", (WritableMap) errorResponse);
            }
        });
    }

    @ReactMethod
    public void sendChatComment(String comment, Promise promise) {
        chatProvider.sendChatComment(comment, new ZendeskCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                promise.resolve("Successful");
            }

            @Override
            public void onError(ErrorResponse errorResponse) {
                promise.reject("400", (WritableMap) errorResponse);
            }
        });
    }

    @ReactMethod
    public void sendEmailTranscript(String email, Promise promise) {
        chatProvider.sendEmailTranscript(email, new ZendeskCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                promise.resolve("Successful");
            }

            @Override
            public void onError(ErrorResponse errorResponse) {
                promise.reject("400", (WritableMap) errorResponse);
            }
        });
    }

    @ReactMethod
    public void endChat(Promise promise) {
        chatProvider.endChat(new ZendeskCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                promise.resolve("Successful");
            }

            @Override
            public void onError(ErrorResponse errorResponse) {
                promise.reject("400", (WritableMap) errorResponse);
            }
        });
    }

    @ReactMethod
    public void setTyping(boolean typing) {
        chatProvider.setTyping(typing);
    }

    @ReactMethod
    public void sendMessage(String message, Callback callback) {
        callback.invoke(chatProvider.sendMessage(message));
    }

    //need to test coorectly in react native app
//    @ReactMethod
//    public void sendFile(String f1, Promise promise) {
//
//        File file = Fil;
//        promise.resolve(chatProvider.sendFile(file, ));
//    }

    @ReactMethod
    public void reSendMessage(String failedChatLogId, Callback callback) {
        callback.invoke(chatProvider.resendFailedFile(failedChatLogId,null));
    }

//    @ReactMethod
//    public void resendFailedFile(String failedChatLogId, Callback callback) {
//        callback.invoke(chatProvider.resendFailedFile(failedChatLogId,null);
//    }

    @ReactMethod
    public void deleteFailedMessage(String failedChatLogId , Callback callback) {
        callback.invoke(chatProvider.deleteFailedMessage(failedChatLogId));
    }

    @ReactMethod
    public void sendOfflineForm(String message, Promise promise) {
//     NOTE: that this method should only be used when the Account or Department status is offline.

        if(chatState.getDepartment().getStatus() == chatState.getDepartment().getStatus().OFFLINE) {

            if(super.isAccountStatusOfline()) {

                if(super.isVisitorInfoSet()) {

                    if(this.isDepartmentSet()) {

                        OfflineForm offlineForm = OfflineForm
                                .builder(message)
                                .withDepartment(chatState.getDepartment().getId())
//                                .withDepartment(chatState.getDepartment().getName())
                                .build();

                        chatProvider.sendOfflineForm(offlineForm, new ZendeskCallback<Void>() {
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
                        promise.reject("400", "Set Department first");
                    }
                } else {
                    promise.reject("400", "setVisitorInfo() first");
                }
            } else {
                promise.reject("400", "Account Status is ONLINE");
            }
        } else {
            promise.reject("400", "Department is ONLINE");
        }

    }



    // ===== GET DETAILS ABOUT CURRENT CHAT ====

    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @ReactMethod
    public void observeChatState(Promise promise) {
        chatProvider.observeChatState(observationScope, new Observer<ChatState>() {
            @Override
            public void update(ChatState chatState) {
                sendEvent(reactContext, "ObserveChatState", (WritableMap) chatState);
            }
        });
        promise.resolve("Successful");
    }

    @ReactMethod
    public void cancelObserveChatState(Callback onSuccess) {
        observationScope.cancel();
        onSuccess.invoke("Successful");
    }

    @ReactMethod
    public void getChatState(Callback onSuccess) {
        onSuccess.invoke(chatProvider.getChatState());
    }

    @ReactMethod
    public void getDepartment(Callback onSuccess) {
        onSuccess.invoke(chatState.getDepartment());
    }

    @ReactMethod
    public void isDepartmentOfline(Callback onSuccess) {
        onSuccess.invoke(chatState.getDepartment().getStatus() == chatState.getDepartment().getStatus().OFFLINE);
    }

    @ReactMethod
    public void getChatId(Callback onSuccess) {
        onSuccess.invoke(chatState.getChatId());
    }

    @ReactMethod
    public void getAgents(Callback onSuccess) {
        onSuccess.invoke(chatState.getAgents());
    }

    @ReactMethod
    public void getChatLogs(Callback onSuccess) {
        onSuccess.invoke(chatState.getChatLogs());
    }

    @ReactMethod
    public void getChatSessionStatus(Callback onSuccess) {
//        INITIALIZING, CONFIGURING, STARTED, ENDING, ENDED
        onSuccess.invoke(chatState.getChatSessionStatus());
    }

    @ReactMethod
    public void isChatting(Callback onSuccess) {
        onSuccess.invoke(chatState.isChatting());
    }

    @ReactMethod
    public void getQueuePosition(Callback onSuccess) {
        onSuccess.invoke(chatState.getQueuePosition());
    }

    @ReactMethod
    public void getChatComment(Callback onSuccess) {
        onSuccess.invoke(chatState.getChatComment());
    }

    @ReactMethod
    public void getChatRating(Callback onSuccess) {
        onSuccess.invoke(chatState.getChatRating());
    }

    // other details
//    @ReactMethod
//    public void getInitialMessageQuestion(Callback onSuccess) {
//        onSuccess.invoke(chatState.getChatRating());
//    }

}

