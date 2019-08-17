package cuk.anze.converter.screens.conversion

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import cuk.anze.converter.R
import cuk.anze.converter.model.CurrencyInfo
import kotlinx.android.synthetic.main.conversion_row.view.*


class ConversionAdapter: RecyclerView.Adapter<ConversionAdapter.ConversionRowHolder>() {

    enum class Payload {
        BASE_VALUE
    }

    private val data = mutableListOf<CurrencyInfo>()
    private var tickerIndexMap = mutableMapOf<String, Int>()
    var manager: RecyclerView.LayoutManager? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversionRowHolder {
        return ConversionRowHolder(LayoutInflater.from(parent.context).inflate(R.layout.conversion_row, parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ConversionRowHolder, position: Int) {
        val rowData = data[position]

        Glide.with(holder.itemView.context)
            .load(rowData.imageUrl)
            .apply(RequestOptions.circleCropTransform())
            .into(holder.ivCurrencyImage)
        holder.tvCurrencyTicker.text = rowData.ticker
        holder.tvCurrencyFullName.text = rowData.fullName
        holder.etCurrencyValue.setText(rowData.baseValue.toString())
    }

    override fun onBindViewHolder(holder: ConversionRowHolder, position: Int, payloads: MutableList<Any>) {
        if (holder.etCurrencyValue.isFocused) {
            return
        }
        if (payloads.isNotEmpty()) {
            when (payloads[0]) {
                Payload.BASE_VALUE -> holder.etCurrencyValue.setText(data[position].baseValue.toString())
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun setData(dataList: List<CurrencyInfo>) {
        data.clear()
        data.addAll(dataList)
        tickerIndexMap = data.mapIndexed { index, currencyInfo -> currencyInfo.ticker to index }
            .toMap()
            .toMutableMap()
    }

    fun updateCurencyValue(currencyInfo: CurrencyInfo) {
        val index = tickerIndexMap[currencyInfo.ticker]
        if (index != null) {
            data[index].baseValue = currencyInfo.baseValue
            notifyItemChanged(index, Payload.BASE_VALUE)
        } else {
            data.add(currencyInfo)
            tickerIndexMap[currencyInfo.ticker] = data.size - 1
        }
    }
    
    fun updateCurencyValues(currencyInfoList: List<CurrencyInfo>) {
        val numPreviousSize = data.size
        currencyInfoList.forEach { currencyInfo ->
            val index = tickerIndexMap[currencyInfo.ticker]
            if (index != null) {
                data[index].baseValue = currencyInfo.baseValue
                notifyItemChanged(index, Payload.BASE_VALUE)
            } else {
                data.add(currencyInfo)
                tickerIndexMap[currencyInfo.ticker] = data.size - 1
            }
        }

        if (numPreviousSize < data.size) {
            notifyItemRangeInserted(numPreviousSize, data.size)
        }
    }

    inner class ConversionRowHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener {
        val ivCurrencyImage: ImageView = view.iv_currencyImage
        val tvCurrencyTicker: TextView = view.tv_currencyTicker
        val tvCurrencyFullName: TextView = view.tv_currencyFullName
        val etCurrencyValue: EditText = view.et_currencyValue

        init {
            etCurrencyValue.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    etCurrencyValue.clearFocus()
                }

                return@setOnEditorActionListener false
            }
            view.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            v?.let {
                val oldPosition = adapterPosition
                if (oldPosition == 0) {
                    return
                }

                val row = data[oldPosition]
                data.removeAt(oldPosition)
                data.add(0, row)
                tickerIndexMap = data.mapIndexed { index, currencyInfo -> currencyInfo.ticker to index }
                    .toMap()
                    .toMutableMap()
                notifyItemMoved(oldPosition, 0).also {
                    manager?.scrollToPosition(0)
                }
            }
        }
    }
}