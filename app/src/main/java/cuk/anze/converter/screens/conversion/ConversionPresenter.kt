package cuk.anze.converter.screens.conversion

import cuk.anze.converter.model.CurrencyInfo
import cuk.anze.converter.rest.ConversionService
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class ConversionPresenter(
    private val conversionService: ConversionService
): ConverterContract.Presenter {

    companion object {
        val currencyNameMap = mapOf(
            "AED" to "United Arab Emirates Dirham",
            "AFN" to "Afghan Afghani",
            "ALL" to "Albanian Lek",
            "AMD" to "Armenian Dram",
            "ANG" to "Netherlands Antillean Guilder",
            "AOA" to "Angolan Kwanza",
            "ARS" to "Argentine Peso",
            "AUD" to "Australian Dollar",
            "AWG" to "Aruban Florin",
            "AZN" to "Azerbaijani Manat",
            "BAM" to "Bosnia-Herzegovina Convertible Mark",
            "BBD" to "Barbadian Dollar",
            "BDT" to "Bangladeshi Taka",
            "BGN" to "Bulgarian Lev",
            "BHD" to "Bahraini Dinar",
            "BIF" to "Burundian Franc",
            "BMD" to "Bermudan Dollar",
            "BND" to "Brunei Dollar",
            "BOB" to "Bolivian Boliviano",
            "BRL" to "Brazilian Real",
            "BSD" to "Bahamian Dollar",
            "BTC" to "Bitcoin",
            "BTN" to "Bhutanese Ngultrum",
            "BWP" to "Botswanan Pula",
            "BYN" to "Belarusian Ruble",
            "BZD" to "Belize Dollar",
            "CAD" to "Canadian Dollar",
            "CDF" to "Congolese Franc",
            "CHF" to "Swiss Franc",
            "CLF" to "Chilean Unit of Account (UF)",
            "CLP" to "Chilean Peso",
            "CNH" to "Chinese Yuan (Offshore)",
            "CNY" to "Chinese Yuan",
            "COP" to "Colombian Peso",
            "CRC" to "Costa Rican Colón",
            "CUC" to "Cuban Convertible Peso",
            "CUP" to "Cuban Peso",
            "CVE" to "Cape Verdean Escudo",
            "CZK" to "Czech Republic Koruna",
            "DJF" to "Djiboutian Franc",
            "DKK" to "Danish Krone",
            "DOP" to "Dominican Peso",
            "DZD" to "Algerian Dinar",
            "EGP" to "Egyptian Pound",
            "ERN" to "Eritrean Nakfa",
            "ETB" to "Ethiopian Birr",
            "EUR" to "Euro",
            "FJD" to "Fijian Dollar",
            "FKP" to "Falkland Islands Pound",
            "GBP" to "British Pound Sterling",
            "GEL" to "Georgian Lari",
            "GGP" to "Guernsey Pound",
            "GHS" to "Ghanaian Cedi",
            "GIP" to "Gibraltar Pound",
            "GMD" to "Gambian Dalasi",
            "GNF" to "Guinean Franc",
            "GTQ" to "Guatemalan Quetzal",
            "GYD" to "Guyanaese Dollar",
            "HKD" to "Hong Kong Dollar",
            "HNL" to "Honduran Lempira",
            "HRK" to "Croatian Kuna",
            "HTG" to "Haitian Gourde",
            "HUF" to "Hungarian Forint",
            "IDR" to "Indonesian Rupiah",
            "ILS" to "Israeli New Sheqel",
            "IMP" to "Manx pound",
            "INR" to "Indian Rupee",
            "IQD" to "Iraqi Dinar",
            "IRR" to "Iranian Rial",
            "ISK" to "Icelandic Króna",
            "JEP" to "Jersey Pound",
            "JMD" to "Jamaican Dollar",
            "JOD" to "Jordanian Dinar",
            "JPY" to "Japanese Yen",
            "KES" to "Kenyan Shilling",
            "KGS" to "Kyrgystani Som",
            "KHR" to "Cambodian Riel",
            "KMF" to "Comorian Franc",
            "KPW" to "North Korean Won",
            "KRW" to "South Korean Won",
            "KWD" to "Kuwaiti Dinar",
            "KYD" to "Cayman Islands Dollar",
            "KZT" to "Kazakhstani Tenge",
            "LAK" to "Laotian Kip",
            "LBP" to "Lebanese Pound",
            "LKR" to "Sri Lankan Rupee",
            "LRD" to "Liberian Dollar",
            "LSL" to "Lesotho Loti",
            "LYD" to "Libyan Dinar",
            "MAD" to "Moroccan Dirham",
            "MDL" to "Moldovan Leu",
            "MGA" to "Malagasy Ariary",
            "MKD" to "Macedonian Denar",
            "MMK" to "Myanma Kyat",
            "MNT" to "Mongolian Tugrik",
            "MOP" to "Macanese Pataca",
            "MRO" to "Mauritanian Ouguiya (pre-2018)",
            "MRU" to "Mauritanian Ouguiya",
            "MUR" to "Mauritian Rupee",
            "MVR" to "Maldivian Rufiyaa",
            "MWK" to "Malawian Kwacha",
            "MXN" to "Mexican Peso",
            "MYR" to "Malaysian Ringgit",
            "MZN" to "Mozambican Metical",
            "NAD" to "Namibian Dollar",
            "NGN" to "Nigerian Naira",
            "NIO" to "Nicaraguan Córdoba",
            "NOK" to "Norwegian Krone",
            "NPR" to "Nepalese Rupee",
            "NZD" to "New Zealand Dollar",
            "OMR" to "Omani Rial",
            "PAB" to "Panamanian Balboa",
            "PEN" to "Peruvian Nuevo Sol",
            "PGK" to "Papua New Guinean Kina",
            "PHP" to "Philippine Peso",
            "PKR" to "Pakistani Rupee",
            "PLN" to "Polish Zloty",
            "PYG" to "Paraguayan Guarani",
            "QAR" to "Qatari Rial",
            "RON" to "Romanian Leu",
            "RSD" to "Serbian Dinar",
            "RUB" to "Russian Ruble",
            "RWF" to "Rwandan Franc",
            "SAR" to "Saudi Riyal",
            "SBD" to "Solomon Islands Dollar",
            "SCR" to "Seychellois Rupee",
            "SDG" to "Sudanese Pound",
            "SEK" to "Swedish Krona",
            "SGD" to "Singapore Dollar",
            "SHP" to "Saint Helena Pound",
            "SLL" to "Sierra Leonean Leone",
            "SOS" to "Somali Shilling",
            "SRD" to "Surinamese Dollar",
            "SSP" to "South Sudanese Pound",
            "STD" to "São Tomé and Príncipe Dobra (pre-2018)",
            "STN" to "São Tomé and Príncipe Dobra",
            "SVC" to "Salvadoran Colón",
            "SYP" to "Syrian Pound",
            "SZL" to "Swazi Lilangeni",
            "THB" to "Thai Baht",
            "TJS" to "Tajikistani Somoni",
            "TMT" to "Turkmenistani Manat",
            "TND" to "Tunisian Dinar",
            "TOP" to "Tongan Pa'anga",
            "TRY" to "Turkish Lira",
            "TTD" to "Trinidad and Tobago Dollar",
            "TWD" to "New Taiwan Dollar",
            "TZS" to "Tanzanian Shilling",
            "UAH" to "Ukrainian Hryvnia",
            "UGX" to "Ugandan Shilling",
            "USD" to "United States Dollar",
            "UYU" to "Uruguayan Peso",
            "UZS" to "Uzbekistan Som",
            "VEF" to "Venezuelan Bolívar Fuerte (Old)",
            "VES" to "Venezuelan Bolívar Soberano",
            "VND" to "Vietnamese Dong",
            "VUV" to "Vanuatu Vatu",
            "WST" to "Samoan Tala",
            "XAF" to "CFA Franc BEAC",
            "XAG" to "Silver Ounce",
            "XAU" to "Gold Ounce",
            "XCD" to "East Caribbean Dollar",
            "XDR" to "Special Drawing Rights",
            "XOF" to "CFA Franc BCEAO",
            "XPD" to "Palladium Ounce",
            "XPF" to "CFP Franc",
            "XPT" to "Platinum Ounce",
            "YER" to "Yemeni Rial",
            "ZAR" to "South African Rand",
            "ZMW" to "Zambian Kwacha",
            "ZWL" to "Zimbabwean Dollar"
        )
    }

    private var view: ConverterContract.View? = null
    private lateinit var conversionBaseTicker: String
    private var actualBaseValue: Double = 1.0
    private var conversionRatesForBase: Map<String, Double>? = null
    private var disposable: Disposable? = null

    private var givenBaseValue = 1.0
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

    override fun setBaseCurrency(ticker: String) {
        conversionBaseTicker = ticker
    }

    override fun calculateCurrencyValuesForBase(baseTicker: String, baseValue: Double) {
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
        disposable = Observable.interval(0, 1, TimeUnit.SECONDS)
            .flatMap { conversionService.getLatestConversionRates(conversionBaseTicker.toUpperCase()) }
            .filter { it.base.equals(conversionBaseTicker, true) }
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

    private fun sendDataToView(givenBaseTicker: String, givenBaseValue: Double) {
        conversionRatesForBase?.let { conversionRatesForBase ->
            if (givenBaseTicker.equals(conversionBaseTicker, true)) {
                val currencyInfoList = conversionRatesForBase.map {
                    CurrencyInfo(
                        it.key,
                        getImageUrlForTicker(it.key),
                        currencyNameMap[it.key] ?: "not provided",
                        it.value * givenBaseValue
                    )
                }.toCollection(ArrayList())
                currencyInfoList.add(0, CurrencyInfo(
                    givenBaseTicker,
                    getImageUrlForTicker(givenBaseTicker),
                    "",
                    givenBaseValue
                ))

                view?.displayConversionRates(currencyInfoList)
            } else {
                conversionRatesForBase[givenBaseTicker]?.let { conversionRateForProvidedBase ->
                    val actualBaseValue = givenBaseValue / conversionRateForProvidedBase
                    val currencyInfoList = conversionRatesForBase.filter {
                        !it.key.equals(givenBaseTicker, true) && !it.key.equals(conversionBaseTicker, true)
                    }.map {
                        CurrencyInfo(
                            it.key,
                            getImageUrlForTicker(it.key),
                            currencyNameMap[it.key] ?: "not provided",
                            it.value * actualBaseValue
                        )
                    }.toCollection(ArrayList())
                    currencyInfoList.add(
                        CurrencyInfo(
                            conversionBaseTicker,
                            getImageUrlForTicker(conversionBaseTicker),
                            currencyNameMap[conversionBaseTicker] ?: "not provided",
                            actualBaseValue
                        )
                    )
                    currencyInfoList.add(
                        0,
                        CurrencyInfo(
                            givenBaseTicker,
                            getImageUrlForTicker(givenBaseTicker),
                            currencyNameMap[givenBaseTicker] ?: "not provided",
                            givenBaseValue
                        )
                    )
                    view?.displayConversionRates(currencyInfoList)

                } ?: view?.displayError("Something went wrong")
            }
        } ?: view?.displayError("Something went wrong")
    }


    private fun getImageUrlForTicker(ticker: String): String {
//        return "https://www.xe.com/themes/xe/images/flags/svg/${ticker.toLowerCase()}.svg"
        return "https://www.xe.com/themes/xe/images/flags/big/${ticker.toLowerCase()}.png"
    }
}