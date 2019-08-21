package cuk.anze.converter.utils.network

import android.content.Context

interface NetworkObserver {

    fun startObserving(
        context: Context,
        handleConnectedState: (() -> Unit)? = null,
        handleDisconnectedState: (() -> Unit)? = null,
        handleError: ((Throwable) -> Unit)? = null
    )

    fun stopObserving()
}