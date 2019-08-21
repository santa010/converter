package cuk.anze.converter.screens.conversion

import android.content.Context
import cuk.anze.converter.model.CurrencyInfo
import cuk.anze.converter.mvp.BasePresenter
import cuk.anze.converter.mvp.BaseView

interface ConverterContract {

    interface View: BaseView {

        fun getApplicationContext(): Context

        fun displayConversionRates(currencyInfoList: List<CurrencyInfo>)

        fun displayError(errorMsg: String)
    }

    interface Presenter: BasePresenter<View> {

        fun calculateCurrencyValuesForBase(baseTicker: String, baseValue: Double?)

        fun pauseUpdates()

        fun resumeUpdates()
    }
}