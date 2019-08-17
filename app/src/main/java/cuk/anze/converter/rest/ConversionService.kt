package cuk.anze.converter.rest

import cuk.anze.converter.model.ConversionRatesResponse
import io.reactivex.Observable
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ConversionService {

    @GET("latest")
    fun getLatestConversionRates(@Query("base") base: String): Observable<ConversionRatesResponse>

    companion object {

        fun create(): ConversionService {
            return Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://revolut.duckdns.org/")
                .build()
                .create(ConversionService::class.java)
        }
    }
}