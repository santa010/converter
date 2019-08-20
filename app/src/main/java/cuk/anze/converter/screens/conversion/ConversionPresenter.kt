package cuk.anze.converter.screens.conversion

import cuk.anze.converter.model.CurrencyInfo
import cuk.anze.converter.rest.ConversionService
import cuk.anze.converter.utils.CurrencyHelper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class ConversionPresenter(
    private val conversionService: ConversionService
): ConverterContract.Presenter {

    private var view: ConverterContract.View? = null
    private lateinit var conversionBaseTicker: String
    private var conversionRatesForBase: Map<String, Double>? = null
    private var disposable: Disposable? = null
    private var givenBaseValue: Double? = 1.0
    private var givenBaseTicker: String? = null

    override fun onSubscribe(view: ConverterContract.View, vararg payload: Any) {
        this.view = view
        conversionBaseTicker = payload[0].toString()

        resumeUpdates()
    }

    override fun onUnsubscribe() {
        pauseUpdates()
        view = null
    }

    override fun calculateCurrencyValuesForBase(baseTicker: String, baseValue: Double?) {
        givenBaseTicker = baseTicker
        givenBaseValue = baseValue

        sendDataToView(baseTicker, baseValue)
        conversionBaseTicker = baseTicker
    }

    override fun pauseUpdates() {
        disposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
    }

    override fun resumeUpdates() {
        disposable = Observable.interval(1, TimeUnit.SECONDS, Schedulers.io())
            .flatMap { conversionService.getLatestConversionRates(conversionBaseTicker.toUpperCase()) }
            .filter {
                it.base.equals(conversionBaseTicker, true) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response ->
                response?.let { conversionRatesResponse ->
                    conversionRatesForBase = conversionRatesResponse.rates

                    givenBaseTicker?.let {  givenBaseTicker ->
                        sendDataToView(givenBaseTicker, givenBaseValue)
                    } ?: sendDataToView(conversionBaseTicker, givenBaseValue)
                }
            }
    }

    private fun sendDataToView(givenBaseTicker: String, givenBaseValue: Double?) {
        conversionRatesForBase?.let { conversionRatesForBase ->
            if (givenBaseTicker.equals(conversionBaseTicker, true)) {
                val currencyInfoList = conversionRatesForBase.map {
                    val value = if (givenBaseValue == null) null else it.value * givenBaseValue
                    createCurrencyInfo(it.key, value)
                }.toCollection(ArrayList())
                currencyInfoList.add(0, createCurrencyInfo(givenBaseTicker, givenBaseValue))
                view?.displayConversionRates(currencyInfoList)
            } else {
                conversionRatesForBase[givenBaseTicker]?.let { conversionRateForProvidedBase ->
                    val actualBaseValue = if (givenBaseValue == null) null else  givenBaseValue / conversionRateForProvidedBase
                    val currencyInfoList = conversionRatesForBase.filter {
                        !it.key.equals(givenBaseTicker, true) && !it.key.equals(conversionBaseTicker, true)
                    }.map {
                        val value = if (actualBaseValue == null) null else it.value * actualBaseValue
                        createCurrencyInfo(it.key, value)
                    }.toCollection(ArrayList())
                    currencyInfoList.add(0, createCurrencyInfo(conversionBaseTicker, actualBaseValue))
                    currencyInfoList.add(0, createCurrencyInfo(givenBaseTicker, givenBaseValue))
                    view?.displayConversionRates(currencyInfoList)

                } ?: view?.displayError("Something went wrong")
            }
        } ?: view?.displayError("Something went wrong")
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