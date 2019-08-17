package cuk.anze.converter.model

import java.util.*

data class ConversionRatesResponse(
    val base: String,
    val date: Date,
    val rates: Map<String, Double>
)