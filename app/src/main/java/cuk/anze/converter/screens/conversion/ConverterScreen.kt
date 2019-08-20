package cuk.anze.converter.screens.conversion

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.google.android.material.snackbar.Snackbar
import cuk.anze.converter.R
import cuk.anze.converter.model.CurrencyInfo
import cuk.anze.converter.rest.ConversionService
import kotlinx.android.synthetic.main.screen_converter.*

class ConverterScreen: AppCompatActivity(), ConverterContract.View {

    private val conversionService by lazy {
        ConversionService.create()
    }

    private lateinit var presenter: ConverterContract.Presenter
    private lateinit var adapter: ConversionAdapter
    private lateinit var skeleton: Skeleton

    private var baseTicker = "EUR"
    private var baseValue = 1.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.screen_converter)

        presenter = ConversionPresenter(conversionService)
        adapter = ConversionAdapter(presenter)

        rv_currencyList.adapter = adapter
        rv_currencyList.layoutManager =  LinearLayoutManager(this)

        skeleton = rv_currencyList.applySkeleton(R.layout.conversion_row_skeleton, 12)
        skeleton.showShimmer = true
        skeleton.shimmerDurationInMillis = 300
        skeleton.maskCornerRadius = 0f

        skeleton.showSkeleton()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState?.let { bundle ->
            bundle.getString("baseTicker")?.let { baseTicker = it }
            if (bundle.containsKey("baseValue")) {
                baseValue = bundle.getDouble("baseValue")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.onSubscribe(this, baseTicker, baseValue)
    }

    override fun onPause() {
        presenter.onUnsubscribe()

        adapter.getBaseCurrency()?.let { baseCurrency ->
            baseTicker = baseCurrency.ticker
            baseCurrency.baseValue?.let { baseValue = it }
        }
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.let { bundle ->
            adapter.getBaseCurrency()?.let { baseCurrency ->
                bundle.putString("baseTicker", baseCurrency.ticker)
                baseCurrency.baseValue?.let { bundle.putDouble("baseValue", it) }
            }
        }
    }

    override fun displayConversionRates(currencyInfoList: List<CurrencyInfo>) {
        if (rv_currencyList.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
            adapter.updateCurrencyValues(currencyInfoList)
        }
        if (skeleton.isSkeleton()) {
            skeleton.showOriginal()
        }
    }

    override fun displayError(errorMsg: String) {
        Snackbar.make(root, errorMsg, Snackbar.LENGTH_LONG).show()
    }
}
