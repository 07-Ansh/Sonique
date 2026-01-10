package com.liskovsoft.sharedutils.rx;

import androidx.annotation.Nullable;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.OnErrorNotImplementedException;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

 
public class RxHelper {
    private static final String TAG = RxHelper.class.getSimpleName();
    private static @Nullable Scheduler sCachedScheduler;

    private static Scheduler getCachedScheduler() {
        if (sCachedScheduler == null) {
            sCachedScheduler = Schedulers.from(Executors.newCachedThreadPool());
        }

        return sCachedScheduler;
    }

    public static void disposeActions(Disposable... actions) {
        if (actions != null) {
            for (Disposable action : actions) {
                if (isActionRunning(action)) {
                    action.dispose();
                }
            }
        }
    }

    public static void disposeActions(List<Disposable> actions) {
        if (actions != null) {
            for (Disposable action : actions) {
                if (isActionRunning(action)) {
                    action.dispose();
                }
            }
            actions.clear();
        }
    }

     
    public static boolean isAnyActionRunning(Disposable... actions) {
        if (actions != null) {
            for (Disposable action : actions) {
                if (isActionRunning(action)) {
                    return true;
                }
            }
        }

        return false;
    }

     
    public static boolean isAnyActionRunning(List<Disposable> actions) {
        if (actions != null) {
            for (Disposable action : actions) {
                if (isActionRunning(action)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isActionRunning(Disposable action) {
        return action != null && !action.isDisposed();
    }

    public static <T> Disposable execute(Observable<T> observable, @Nullable OnResult<T> onResult, @Nullable OnError onError, @Nullable Runnable onFinish) {
        if (onResult == null) {
            onResult = result -> {};  
        }

        if (onError == null) {
            onError = error -> Log.e(TAG, "Execute error: %s", error.getMessage());
        }

        if (onFinish == null) {
            onFinish = () -> {};
        }

        return observable
                .subscribe(
                        onResult::onResult,
                        onError::onError,
                        onFinish::run
                );
    }

    public static <T> Disposable execute(Observable<T> observable) {
        return execute(observable, null, null, null);
    }

    public static <T> Disposable execute(Observable<T> observable, OnResult<T> onResult, OnError onError) {
        return execute(observable, onResult, onError, null);
    }

    public static <T> Disposable execute(Observable<T> observable, OnError onError) {
        return execute(observable, null, onError, null);
    }

    public static <T> Disposable execute(Observable<T> observable, Runnable onFinish) {
        return execute(observable, null, null, onFinish);
    }

    public static <T> Disposable execute(Observable<T> observable, OnError onError, Runnable onFinish) {
        return execute(observable, null, onError, onFinish);
    }

    public static Disposable startInterval(Runnable callback, int periodSec) {
        return interval(periodSec, TimeUnit.SECONDS)
                .subscribe(
                        period -> callback.run(),
                        error -> Log.e(TAG, "startInterval error: %s", error.getMessage())
                );
    }

    public static Disposable runAsync(Runnable callback) {
        return Completable.fromRunnable(callback)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(() -> {}, error -> Log.e(TAG, error.getMessage()));
    }

    public static Disposable runAsync(Runnable callback, long delayMs) {
        return Completable.fromRunnable(callback)
                .delaySubscription(delayMs, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(() -> {}, error -> Log.e(TAG, error.getMessage()));
    }

    public static Disposable runAsyncUser(Runnable callback) {
        return runAsyncUser(callback, null, null);
    }

    public static Disposable runAsyncUser(Runnable callback, Runnable onFinish) {
        return runAsyncUser(callback, null, onFinish);
    }

    public static Disposable runUser(Runnable callback) {
        return runAsyncUser(() -> {}, null, callback);
    }

    public static Disposable runAsyncUser(Runnable callback, @Nullable OnError onError, @Nullable Runnable onFinish) {
        return Completable.fromRunnable(callback)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            if (onFinish != null) {
                                onFinish.run();
                            }
                        },
                        error -> {
                            if (onError != null) {
                                onError.onError(error);
                            }
                        }
                );
    }

    public static <T> void runBlocking(Observable<T> observable) {
        observable.blockingSubscribe();
    }

     
    public static void setupGlobalErrorHandler() {
        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                Log.e(TAG, "Undeliverable exception received, not sure what to do", e.getCause());
                return;
            }
            if ((e instanceof IllegalStateException) &&
                    ((e.getCause() instanceof SocketException) ||
                     (e.getCause() instanceof SocketTimeoutException) ||
                     (e.getCause() instanceof UnknownHostException))) {
                 
                Log.e(TAG, "Network error", e.getCause());
                return;
            }
            if ((e instanceof IllegalStateException) && (e.getCause() == null)) {
                Log.e(TAG, "Seems that the user forgot to implement error handler", e);
                return;
            }
            if (e instanceof IOException) {
                 
                return;
            }
            if ((e instanceof IllegalStateException) &&
                    (e.getCause() instanceof IOException)) {
                 
                return;
            }
            if (e instanceof InterruptedException) {
                 
                return;
            }
            if (e instanceof NullPointerException && Helpers.equals(e.getStackTrace()[0].getClassName(), "java.net.SocksSocketImpl")) {
                 
                 
                return;
            }
            if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
                 
                Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), e);
                return;
            }
            if (e instanceof IllegalStateException) {
                 
                Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), e);
                return;
            }
        });
    }

    public static <T> Observable<T> create(ObservableOnSubscribe<T> source) {
        return setup(Observable.create(wrapOnSubscribe(source)));
    }

    public static <T> Observable<T> createLong(ObservableOnSubscribe<T> source) {
        return setupLong(Observable.create(wrapOnSubscribe(source)));
    }

    public static <T> Observable<T> fromCallable(Callable<T> callback) {
         
         
        return fromNullable(callback);
    }

    @SafeVarargs
    private static <T> Observable<T> fromMultiCallable(Callable<T>... callbacks) {
        return fromMultiNullable(callbacks);
    }

    public static <T> Observable<T> fromIterable(Iterable<T> source) {
        return setup(Observable.fromIterable(source));
    }

    public static Observable<Void> fromRunnable(Runnable callback) {
        return create(emitter -> {
            callback.run();
            emitter.onComplete();
        });
    }

    private static <T> Observable<T> fromNullable(Callable<T> callback) {
        return create(emitter -> {
            T result = callback.call();

            if (result != null) {
                emitter.onNext(result);
                emitter.onComplete();
            } else {
                 
                 
                onError(emitter, "fromNullable result is null");
                Log.e(TAG, "fromNullable result is null");
            }
        });
    }

    @SafeVarargs
    private static <T> Observable<T> fromMultiNullable(Callable<T>... callbacks) {
        return create(emitter -> {
            boolean success = false;
            for (Callable<T> callback : callbacks) {
                T result = callback.call();

                if (result != null) {
                    emitter.onNext(result);
                    success = true;
                }
            }

            if (success) {
                emitter.onComplete();
            } else {
                 
                 
                onError(emitter, "fromMultiNullable result is null");
                Log.e(TAG, "fromMultiNullable result is null");
            }
        });
    }

    public static Observable<Long> interval(long period, TimeUnit unit) {
        return setupLong(Observable.interval(period, unit));
    }

     
    public static <T> void onError(ObservableEmitter<T> emitter, String msg) {
        emitter.tryOnError(new IllegalStateException(msg));
    }

     
    private static <T> Observable<T> setup(Observable<T> observable) {
         
         
        return observable
                .subscribeOn(getCachedScheduler())
                .observeOn(AndroidSchedulers.mainThread());
    }

     
    private static <T> Observable<T> setupLong(Observable<T> observable) {
         
         
         
        return observable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

     
    private static <T> ObservableOnSubscribe<T> wrapOnSubscribe(ObservableOnSubscribe<T> source) {
        return emitter -> {
            try {
                source.subscribe(emitter);
            } catch (Exception e) {
                 
                 
                if (emitter.isDisposed()) {
                     
                     
                     
                    e.printStackTrace();
                } else {
                    throw e;
                }
            }
        };
    }
}
