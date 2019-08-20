package cuk.anze.converter.model

data class CurrencyInfo(
    var ticker: String,
    var imageUrl: String,
    var fullName: String,
    var baseValue: Double?


) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CurrencyInfo

        if (ticker != other.ticker) return false

        return true
    }

    override fun hashCode(): Int {
        return ticker.hashCode()
    }
}