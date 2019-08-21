package cuk.anze.converter.utils.network

import android.content.Context
import android.net.NetworkInfo
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class NetworkObserverMock: NetworkObserver {

    private val behaviourSubject: BehaviorSubject<NetworkInfo.State> = BehaviorSubject.create()
    private var disposable: Disposable? = null

    override fun startObserving(
        context: Context,
        handleConnectedState: (() -> Unit)?,
        handleDisconnectedState: (() -> Unit)?,
        handleError: ((Throwable) -> Unit)?
    ) {
        disposable = behaviourSubject
            .subscribeOn(Schedulers.trampoline())
            .observeOn(Schedulers.trampoline())
            .subscribeWith(object: DisposableObserver<NetworkInfo.State>() {
                override fun onComplete() {
                    // empty
                }

                override fun onNext(state: NetworkInfo.State) {
                    if (state == NetworkInfo.State.CONNECTED) {
                        handleConnectedState?.let { it() }
                    }
                    else if (state == NetworkInfo.State.DISCONNECTED) {
                        handleDisconnectedState?.let { it() }
                    }
                }

                override fun onError(e: Throwable) {
                    handleError?.let { it(e) }
                }
            })
    }

    override fun stopObserving() {
        disposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
    }

    fun pushNetworkState(state: NetworkInfo.State) {
        behaviourSubject.onNext(state)
    }
}