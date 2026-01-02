package com.liskovsoft.sharedutils.misc;

import com.liskovsoft.sharedutils.helpers.Helpers;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WeakHashSet<T> {
    private final List<WeakReference<T>> mWeakReferences = new CopyOnWriteArrayList<>(); // ConcurrentModificationException fix
    private boolean mIsBlocked;
    private boolean mIsStopped;

    public interface OnItem<T> {
        void onItem(T item);
    }

    public boolean add(T item) {
        if (item != null && !contains(item)) {
            cleanup();
            mWeakReferences.add(new WeakReference<>(item));
            return true;
        }

        return false;
    }

    public boolean add(int idx, T item) {
        if (item != null && !contains(item)) {
            cleanup();
            mWeakReferences.add(idx, new WeakReference<>(item));
            return true;
        }

        return false;
    }

    public void remove(T item) {
        if (item != null) {
            Helpers.removeIf(mWeakReferences, next -> item.equals(next.get()));
        }
    }

    public int size() {
        return mWeakReferences.size();
    }

    public void forEach(OnItem<T> onItem) {
        if (mIsBlocked)
            return;

        mIsBlocked = true;
        mIsStopped = false;
        try {
            for (WeakReference<T> reference : mWeakReferences) {
                if (mIsStopped)
                    break;

                if (reference.get() != null) {
                    onItem.onItem(reference.get());
                }
            }
        } finally {
            mIsBlocked = false;
        }
    }

    public boolean contains(T item) {
        return Helpers.containsIf(mWeakReferences, next -> item.equals(next.get()));
    }

    public void clear() {
        mWeakReferences.clear();
    }

    public boolean isEmpty() {
        return mWeakReferences.isEmpty();
    }

    private void cleanup() {
        Helpers.removeIf(mWeakReferences, item -> item.get() == null);
    }

    public List<T> asList() {
        List<T> result = new ArrayList<>();

        for (WeakReference<T> reference : mWeakReferences) {
            if (reference.get() != null) {
                result.add(reference.get());
            }
        }

        return result;
    }

    public void stopForEach() {
        mIsStopped = true;
    }
}
