package cuk.anze.converter.screens.conversion

import cuk.anze.converter.model.CurrencyInfo
import cuk.anze.converter.mvp.BasePresenter
import cuk.anze.converter.mvp.BaseView

interface ConverterContract {

    interface View: BaseView {

        fun displayConversionRates(currencyInfoList: List<CurrencyInfo>)

        fun displayError(errorMsg: String)
    }

    interface Presenter: BasePresenter<View> {

        fun setBaseCurrency(ticker: String)

        fun calculateCurrencyValuesForBase(baseTicker: String, baseValue: Double)

        fun pauseUpdates()

        fun resumeUpdates()
    }
}