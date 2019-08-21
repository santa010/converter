package cuk.anze.converter.screens.conversion

import android.content.Context
import android.net.NetworkInfo
import com.nhaarman.mockitokotlin2.*
import cuk.anze.converter.model.ConversionRatesResponse
import cuk.anze.converter.model.CurrencyInfo
import cuk.anze.converter.rest.ConversionService
import cuk.anze.converter.utils.network.NetworkObserverMock
import io.reactivex.Observable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import junit.framework.TestCase.assertEquals

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

class ConversionPresenterTest {

    private val testScheduler = TestScheduler()
    private val defaultCurrencyTicker = "EUR"
    private val defaultUserBaseValue = 1.0
    private val defaultRates = mapOf(
        "AUD" to 1.6092,
        "BGN" to 1.9471,
        "BRL" to 4.7706,
        "CAD" to 1.527,
        "CHF" to 1.1225,
        "CNY" to 7.9099
    )

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var conversionView: ConverterContract.View

    @Mock
    private lateinit var conversionService: ConversionService

    @Mock
    private lateinit var defaultRatesResponse: ConversionRatesResponse

    private lateinit var networkObserver: NetworkObserverMock

    private lateinit var presenter: ConversionPresenter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        whenever(conversionView.getApplicationContext()).thenReturn(context)
        whenever(defaultRatesResponse.base).thenReturn(defaultCurrencyTicker)
        whenever(defaultRatesResponse.rates).thenReturn(defaultRates)
        whenever(conversionService.getLatestConversionRates(defaultCurrencyTicker)).thenReturn(Observable.just(defaultRatesResponse))

        RxJavaPlugins.setIoSchedulerHandler { testScheduler }
        RxJavaPlugins.setComputationSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setNewThreadSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        networkObserver = NetworkObserverMock()
        networkObserver.pushNetworkState(NetworkInfo.State.CONNECTED)

        presenter = ConversionPresenter(conversionService, networkObserver)
    }

    @Test
    internal fun shouldGetCurrencyInfoOnSubscribe() {
        presenter.onSubscribe(conversionView, defaultCurrencyTicker, defaultUserBaseValue)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        argumentCaptor<List<CurrencyInfo>>().apply {
            verify(conversionView, times(1)).displayConversionRates(capture())
            verify(conversionView, never()).displayError(any())
            checkWithDefaultRates(firstValue, 1.0, defaultCurrencyTicker)
        }
    }

    @Test
    fun shouldCalculateRatesBasedOnUserValue() {
        val userValue = 2.5

        presenter.onSubscribe(conversionView, defaultCurrencyTicker, defaultUserBaseValue)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        presenter.calculateCurrencyValuesForBase(defaultCurrencyTicker, userValue)

        argumentCaptor<List<CurrencyInfo>>().apply {
            verify(conversionView, times(2)).displayConversionRates(capture())
            verify(conversionView, never()).displayError(any())
            checkWithDefaultRates(secondValue, userValue, defaultCurrencyTicker)
        }
    }

    @Test
    fun shouldSetNullValuesForGivenNullValue() {
        val userValue = null

        presenter.onSubscribe(conversionView, defaultCurrencyTicker, defaultUserBaseValue)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        presenter.calculateCurrencyValuesForBase(defaultCurrencyTicker, userValue)

        argumentCaptor<List<CurrencyInfo>>().apply {
            verify(conversionView, times(2)).displayConversionRates(capture())
            verify(conversionView, never()).displayError(any())
            checkWithDefaultRates(secondValue, userValue, defaultCurrencyTicker)
        }
    }

    @Test
    fun shouldUseTwoStepConversion() {
        val userValue = 3.5
        val ticker = "BRL"

        presenter.onSubscribe(conversionView, defaultCurrencyTicker, defaultUserBaseValue)
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        presenter.calculateCurrencyValuesForBase(ticker, userValue)

        argumentCaptor<List<CurrencyInfo>>().apply {
            verify(conversionView, times(2)).displayConversionRates(capture())
            verify(conversionView, never()).displayError(any())

            val oldBaseValue = 1.0 * userValue / defaultRates.getValue(ticker)
            assertEquals(7, secondValue.size)
            assertCurrencyInfo(ticker, userValue, secondValue[0])
            assertCurrencyInfo("EUR", oldBaseValue, secondValue[1])
            assertCurrencyInfo("AUD", oldBaseValue * defaultRates.getValue("AUD"), secondValue[2])
            assertCurrencyInfo("BGN", oldBaseValue * defaultRates.getValue("BGN"), secondValue[3])
            assertCurrencyInfo("CAD", oldBaseValue * defaultRates.getValue("CAD"), secondValue[4])
            assertCurrencyInfo("CHF", oldBaseValue * defaultRates.getValue("CHF"), secondValue[5])
            assertCurrencyInfo("CNY", oldBaseValue * defaultRates.getValue("CNY"), secondValue[6])
        }
    }

    private fun checkWithDefaultRates(currencyInfoList: List<CurrencyInfo>, userValue: Double?, baseTicker: String) {
        assertEquals(defaultRates.size + 1, currencyInfoList.size)
        // first entry should be the base currency (that user can change)
        assertCurrencyInfo(baseTicker, userValue, currencyInfoList[0])
        // check the rest
        defaultRates.keys.forEachIndexed { index, ticker ->
            val calculatedValue = userValue?.let { defaultRates.getValue(ticker) * it }
            assertCurrencyInfo(ticker, calculatedValue, currencyInfoList[index + 1])
        }
    }

    private fun assertCurrencyInfo(ticker: String, value: Double?, currencyInfo: CurrencyInfo) {
        assertEquals(currencyInfo.ticker, ticker)
        assertEquals(currencyInfo.baseValue, value)
    }
}