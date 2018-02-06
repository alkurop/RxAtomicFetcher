package com.alkurop.rxatmomicfetcher

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.subjects.BehaviorSubject
import org.junit.Test

class RxAtomicFetcherTest {

    @Test
    fun loadUpdatesCacheOnFirstCall() {
        val request = Observable.just(0, 1)
        val fetcher = RxAtomicFetcher(request)
        fetcher.load().test().assertValue(0)
    }

    @Test
    fun loadCanUpdateCacheIfWasError() {
        val runtimeException = RuntimeException("test")


        val request = Observable.create<Int>(object : ObservableOnSubscribe<Int> {
            internal var count: Int = 0

            override fun subscribe(subscriber: ObservableEmitter<Int>) {
                if (count == 0) {
                    subscriber.onError(runtimeException)
                    count++
                } else {
                    subscriber.onNext(1)
                    subscriber.onComplete()
                }
            }
        })

        val fetcher = RxAtomicFetcher(request)

        fetcher.load().test()
            .assertError(runtimeException)
        fetcher.load().test()
            .assertValue(1)
    }

    @Test
    fun loadReturnsSameDataForMultipleCalls() {
        val request = Observable.just(0, 1)
        val fetcher = RxAtomicFetcher(request)

        fetcher.load().test().assertValue(0)
        fetcher.load().test().assertValue(0)
    }

    @Test
    fun loadNewRequestOnPayloadChanged() {
        val request = BehaviorSubject.create<Int>()
        val fetcher = RxAtomicFetcher(request)
        request.onNext(0)
        fetcher.load(0).test().assertValue(0)
        request.onNext(1)
        fetcher.load(1).test().assertValue(1)
    }

    @Test
    fun returnOldValueIfPayloadSame() {
        val request = Observable.just(0, 1)
        val fetcher = RxAtomicFetcher(request)

        fetcher.load(0).test().assertValue(0)
        fetcher.load(0).test().assertValue(0)
    }

    @Test
    fun returnOldValueIfPayloadEmpty() {
        val request = Observable.just(0, 1)
        val fetcher = RxAtomicFetcher(request)

        fetcher.load(null).test().assertValue(0)
        fetcher.load(null).test().assertValue(0)
    }
}
