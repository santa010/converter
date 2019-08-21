package cuk.anze.converter.screens.conversion

import android.content.Context
import android.net.NetworkInfo
import com.github.pwittchen.reactivenetwork.library.rx2.Connectivity
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import cuk.anze.converter.model.ConversionRatesResponse
import cuk.anze.converter.model.CurrencyInfo
import cuk.anze.converter.rest.ConversionService
import cuk.anze.converter.utils.CurrencyHelper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class ConversionPresenter(
    private val conversionService: ConversionService
) : ConverterContract.Presenter {

    private var view: ConverterContract.View? = null
    private var networkDisposable: Disposable? = null
    private var apiDisposable: Disposable? = null
    private var isConnected = false

    private lateinit var conversionBaseTicker: String
    private var conversionRatesForBase: Map<String, Double>? = null
    private lateinit var requestingTicker: String
    private var userBaseValue: Double? = 1.0
    private var userBaseTicker: String? = null

    override fun onSubscribe(view: ConverterContract.View, vararg payload: Any) {
        this.view = view
        conversionBaseTicker = payload[0] as String
        userBaseValue = payload[1] as Double
        requestingTicker = conversionBaseTicker

        startObservingNetwork(view.getApplicationContext())
    }

    override fun onUnsubscribe() {
        disposeDisposable(apiDisposable)
        disposeDisposable(networkDisposable)

        view = null
    }

    override fun calculateCurrencyValuesForBase(baseTicker: String, baseValue: Double?) {
        userBaseTicker = baseTicker
        userBaseValue = baseValue

        sendDataToView(baseTicker, baseValue)
        requestingTicker = baseTicker
    }

    override fun pauseUpdates() {
        disposeDisposable(apiDisposable)
    }

    override fun resumeUpdates() {
        if (isConnected) {
            startRequestingApi()
        }
    }

    private fun startObservingNetwork(context: Context) {
        if (isDisposableInUse(networkDisposable)) {
            return
        }

        networkDisposable = ReactiveNetwork.observeNetworkConnectivity(context)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object: DisposableObserver<Connectivity>() {
                override fun onComplete() {
                    // empty
                }

                override fun onNext(connectivity: Connectivity) {
                    if (connectivity.state() == NetworkInfo.State.CONNECTED) {
                        isConnected = true
                        startRequestingApi()
                    }
                    else if (connectivity.state() == NetworkInfo.State.DISCONNECTED) {
                        isConnected = false
                        pauseUpdates()
                    }
                }

                override fun onError(e: Throwable) {
                    view?.displayError("Could not establish a connection.")
                }
            })
    }

    private fun startRequestingApi() {
        if (isDisposableInUse(apiDisposable)) {
            return
        }

        apiDisposable = Observable.interval(1, TimeUnit.SECONDS, Schedulers.io())
            .flatMap { conversionService.getLatestConversionRates(requestingTicker.toUpperCase()) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object: DisposableObserver<ConversionRatesResponse>() {
                override fun onComplete() {
                    // empty
                }

                override fun onNext(response: ConversionRatesResponse) {
                    conversionBaseTicker = response.base
                    conversionRatesForBase = response.rates
                    val ticker = userBaseTicker?.let { it } ?: conversionBaseTicker
                    sendDataToView(ticker, userBaseValue)
                }

                override fun onError(e: Throwable) {
                    // empty
                }
            }
        )
    }

    private fun isDisposableInUse(disposable: Disposable?): Boolean {
        disposable?.let {
            if (!it.isDisposed) {
                return true
            }
        }

        return false
    }

    private fun disposeDisposable(disposable: Disposable?) {
        disposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
    }

    private fun sendDataToView(userBaseTicker: String, userBaseValue: Double?) {
        view?.let { view ->
            conversionRatesForBase?.let { conversionRatesForBase ->
                if (userBaseTicker.equals(conversionBaseTicker, true)) {
                    // If the provided user currency matches the base currency of our conversion rates map
                    // we can use a normal conversion
                    val currencyInfoList = oneStepConversionList(
                        conversionRatesForBase,
                        userBaseTicker,
                        userBaseValue
                    )
                    view.displayConversionRates(currencyInfoList)
                } else {
                    // If the provided user currency is different from our current base
                    // we have to do a two step conversion
                    conversionRatesForBase[userBaseTicker]?.let { conversionRateForUserBase ->
                        val currencyInfoList = twoStepConversionList(
                            conversionRatesForBase,
                            userBaseTicker,
                            userBaseValue,
                            conversionRateForUserBase,
                            conversionBaseTicker
                        )
                        view.displayConversionRates(currencyInfoList)

                    } ?: view.displayError("Something went wrong")
                }
            } ?: view.displayError("Something went wrong")
        }
    }

    /**
     * TODO document
     */
    private fun oneStepConversionList(
        conversionRates: Map<String, Double>,
        userBaseTicker: String,
        userBaseValue: Double?
    ): List<CurrencyInfo> {
        val currencyInfoList = conversionRates
            .map {
                val value = if (userBaseValue == null) null else it.value * userBaseValue
                createCurrencyInfo(it.key, value)
            }
            .toCollection(ArrayList())
        currencyInfoList.add(0, createCurrencyInfo(userBaseTicker, userBaseValue))

        return currencyInfoList
    }

    /**
     * TODO document
     */
    private fun twoStepConversionList(
        conversionRates: Map<String, Double>,
        userBaseTicker: String,
        userBaseValue: Double?,
        conversionRateForUserBase: Double,
        conversionBaseTicker: String
    ): List<CurrencyInfo> {
        val actualBaseValue = if (userBaseValue == null) null else userBaseValue / conversionRateForUserBase
        val currencyInfoList = conversionRates.filter {
            !it.key.equals(userBaseTicker, true) && !it.key.equals(conversionBaseTicker, true)
        }.map {
            val value = if (actualBaseValue == null) null else it.value * actualBaseValue
            createCurrencyInfo(it.key, value)
        }.toCollection(ArrayList())
        currencyInfoList.add(0, createCurrencyInfo(conversionBaseTicker, actualBaseValue))
        currencyInfoList.add(0, createCurrencyInfo(userBaseTicker, userBaseValue))

        return currencyInfoList
    }

    private fun createCurrencyInfo(ticker: String, value: Double?): CurrencyInfo {
        return CurrencyInfo(
            ticker,
            CurrencyHelper.getImageUrlForTicker(ticker),
            CurrencyHelper.getNameForTicker(ticker),
            value
        )
    }
}