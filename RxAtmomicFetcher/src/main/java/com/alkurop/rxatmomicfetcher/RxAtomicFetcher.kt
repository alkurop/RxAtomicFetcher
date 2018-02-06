package com.alkurop.rxatmomicfetcher

import io.reactivex.Notification
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.Arrays

class RxAtomicFetcher<T> @JvmOverloads constructor(
        private val request: Observable<T>,
        private val scheduler: Scheduler? = null
) {
    @Volatile
    private var mCacheSubject = BehaviorSubject.create<Notification<T>>()
    @Volatile
    private var mDataRequested = false
    @Volatile
    private var mPayload: Array<out Any?> = arrayOf()
    private val disposable = CompositeDisposable()

    @Synchronized
    fun load(vararg payload: Any?): Observable<T> {
        if (!Arrays.equals(mPayload, payload)) {
            disposable.clear()
            if (hasCachedValue()) {
                mCacheSubject = BehaviorSubject.create<Notification<T>>()
            }
            mDataRequested = false
            mPayload = payload
        }
        val observable = Observable.fromCallable { loadToCacheIfNeeded() }
            .switchMap {
                mCacheSubject
            }
            .dematerialize<T>()

        return if (scheduler != null) observable.subscribeOn(scheduler) else observable
    }

    private fun loadToCacheIfNeeded() {
        if (!mDataRequested && !hasCachedValue()) {
            mDataRequested = true
            disposable += request
                .materialize()
                .filter({ !it.isOnComplete })
                .take(1)
                .subscribe {
                    mCacheSubject.onNext(it)
                    mDataRequested = false
                }
        }
    }

    private fun hasCachedValue(): Boolean = mCacheSubject.hasValue() && mCacheSubject.value.isOnNext
}

private operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    this.add(disposable)
}
