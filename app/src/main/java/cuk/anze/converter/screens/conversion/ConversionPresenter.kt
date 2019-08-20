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
) : ConverterContract.Presenter {

    private var view: ConverterContract.View? = null
    private lateinit var conversionBaseTicker: String
    private var conversionRatesForBase: Map<String, Double>? = null
    private var disposable: Disposable? = null
    private var userBaseValue: Double? = 1.0
    private var userBaseTicker: String? = null

    override fun onSubscribe(view: ConverterContract.View, vararg payload: Any) {
        this.view = view
        conversionBaseTicker = payload[0] as String
        userBaseValue = payload[1] as Double

        resumeUpdates()
    }

    override fun onUnsubscribe() {
        pauseUpdates()
        view = null
    }

    override fun calculateCurrencyValuesForBase(baseTicker: String, baseValue: Double?) {
        userBaseTicker = baseTicker
        userBaseValue = baseValue

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
            .filter { it.base.equals(conversionBaseTicker, true) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response ->
                response?.let { conversionRatesResponse ->
                    conversionRatesForBase = conversionRatesResponse.rates
                    userBaseTicker?.let { givenBaseTicker ->
                        sendDataToView(givenBaseTicker, userBaseValue)
                    } ?: sendDataToView(conversionBaseTicker, userBaseValue)
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