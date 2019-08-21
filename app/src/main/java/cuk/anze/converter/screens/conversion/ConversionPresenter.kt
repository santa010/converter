package cuk.anze.converter.screens.conversion

import cuk.anze.converter.model.ConversionRatesResponse
import cuk.anze.converter.model.CurrencyInfo
import cuk.anze.converter.rest.ConversionService
import cuk.anze.converter.utils.CurrencyHelper
import cuk.anze.converter.utils.network.NetworkObserver
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class ConversionPresenter(
    private val conversionService: ConversionService,
    private val networkObserver: NetworkObserver
) : ConverterContract.Presenter {

    private var view: ConverterContract.View? = null
    private var apiDisposable: Disposable? = null
    private var connected = false

    private var conversionRatesResponse: ConversionRatesResponse? = null
    private lateinit var requestingTicker: String
    private var userBaseValue: Double? = 1.0
    private var userBaseTicker: String? = null

    override fun onSubscribe(view: ConverterContract.View, vararg payload: Any) {
        this.view = view
        requestingTicker = payload[0] as String
        userBaseValue = payload[1] as Double

        networkObserver.startObserving(
            view.getApplicationContext(),
            { connected = true;  startApiUpdates() },
            { connected = false; stopApiUpdates() },
            { this.view?.displayError("Can not establish connection") }
        )
    }

    override fun onUnsubscribe() {
        stopApiUpdates()
        networkObserver.stopObserving()
        view = null
    }

    override fun calculateCurrencyValuesForBase(baseTicker: String, baseValue: Double?) {
        userBaseTicker = baseTicker
        userBaseValue = baseValue
        sendDataToView(baseTicker, baseValue)
        requestingTicker = baseTicker
    }

    override fun startApiUpdates() {
        if (!connected) {
            return
        }

        apiDisposable?.let {
            if (!it.isDisposed) {
                return
            }
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
                    conversionRatesResponse = response
                    val ticker = userBaseTicker?.let { it } ?: response.base
                    sendDataToView(ticker, userBaseValue)
                }

                override fun onError(e: Throwable) {
                    // empty
                }
            })
    }

    override fun stopApiUpdates() {
        apiDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
    }

    private fun sendDataToView(userBaseTicker: String, userBaseValue: Double?) {
        view?.let { view ->
            conversionRatesResponse?.let { conversionRatesResponse ->
                if (userBaseTicker.equals(conversionRatesResponse.base, true)) {
                    // If the provided user currency matches the base currency of our conversion rates map
                    // we can use a normal conversion
                    val currencyInfoList = oneStepConversionList(
                        conversionRatesResponse.rates,
                        userBaseTicker,
                        userBaseValue
                    )
                    view.displayConversionRates(currencyInfoList)
                } else {
                    // If the provided user currency is different from our current base
                    // we have to do a two step conversion
                    conversionRatesResponse.rates[userBaseTicker]?.let { conversionRateForUserBase ->
                        val currencyInfoList = twoStepConversionList(
                            conversionRatesResponse.rates,
                            userBaseTicker,
                            userBaseValue,
                            conversionRateForUserBase,
                            conversionRatesResponse.base
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