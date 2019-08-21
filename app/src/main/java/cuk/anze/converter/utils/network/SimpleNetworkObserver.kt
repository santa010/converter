package cuk.anze.converter.utils.network

import android.content.Context
import android.net.NetworkInfo
import com.github.pwittchen.reactivenetwork.library.rx2.Connectivity
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers

class SimpleNetworkObserver: NetworkObserver {

    private var disposable: Disposable? = null

    override fun startObserving(
        context: Context,
        handleConnectedState: (() -> Unit)?,
        handleDisconnectedState: (() -> Unit)?,
        handleError: ((Throwable) -> Unit)?
    ) {
        disposable =  ReactiveNetwork.observeNetworkConnectivity(context)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object: DisposableObserver<Connectivity>() {
                override fun onComplete() {
                    // empty
                }

                override fun onNext(connectivity: Connectivity) {
                    if (connectivity.state() == NetworkInfo.State.CONNECTED) {
                        handleConnectedState?.let { it() }
                    }
                    else if (connectivity.state() == NetworkInfo.State.DISCONNECTED) {
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
}