package cuk.anze.converter.screens.conversion

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.screen_converter)

        presenter = ConversionPresenter(conversionService)
        adapter = ConversionAdapter(presenter)

        rv_currencyList.adapter = adapter
        val lmanager = LinearLayoutManager(
            this,
            RecyclerView.VERTICAL,
            false
        )
        lmanager.isSmoothScrollbarEnabled = true
        rv_currencyList.layoutManager =  lmanager
        adapter.manager = lmanager

        skeleton = rv_currencyList.applySkeleton(R.layout.conversion_row_skeleton, 12)
        skeleton.showShimmer = true
        skeleton.shimmerDurationInMillis = 300
        skeleton.maskCornerRadius = 0f

        skeleton.showSkeleton()
    }

    override fun onResume() {
        super.onResume()
        presenter.onSubscribe(this, "EUR", 1.0)
    }

    override fun onPause() {
        presenter.onUnsubscribe()
        super.onPause()
    }

    override fun displayConversionRates(currencyInfoList: List<CurrencyInfo>) {
        if (rv_currencyList.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
            adapter.updateCurencyValues(currencyInfoList)
        }
        if (skeleton.isSkeleton()) {
            skeleton.showOriginal()
        }
    }

    override fun displayError(errorMsg: String) {
        // TODO display error
    }
}

fun Double.roundTo(decimalPlaces: Int): Double {
    val tenToPower = Math.pow(10.0, decimalPlaces.toDouble())
    val int = (this * tenToPower).toInt()
    return int.toDouble() / tenToPower
}
