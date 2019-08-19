package cuk.anze.converter.model

data class CurrencyInfo(
    var ticker: String,
    var imageUrl: String,
    var fullName: String,
    var baseValue: Double?
)