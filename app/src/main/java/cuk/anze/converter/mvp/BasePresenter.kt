package cuk.anze.converter.mvp

import androidx.annotation.NonNull

interface BasePresenter<V : BaseView> {

    fun onSubscribe(@NonNull view: V, vararg payload: Any)

    fun onUnsubscribe()
}
