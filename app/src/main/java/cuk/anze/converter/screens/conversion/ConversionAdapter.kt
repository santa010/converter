package cuk.anze.converter.screens.conversion

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import cuk.anze.converter.R
import cuk.anze.converter.extensions.showKeyboard
import cuk.anze.converter.model.CurrencyInfo
import kotlinx.android.synthetic.main.conversion_row.view.*
import java.lang.Double.parseDouble


class ConversionAdapter(
    private val presenter: ConverterContract.Presenter
): RecyclerView.Adapter<ConversionAdapter.ConversionRowHolder>() {

    enum class Payload {
        BASE_VALUE
    }

    private val data = mutableListOf<CurrencyInfo>()
    private var tickerIndexMap = mutableMapOf<String, Int>()
    private lateinit var recyclerView: RecyclerView

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
        bindCurrencyValue(holder, data[position].baseValue)
    }

    override fun onBindViewHolder(holder: ConversionRowHolder, position: Int, payloads: MutableList<Any>) {
        if (holder.etCurrencyValue.isFocused) {
            return
        }
        if (payloads.isNotEmpty()) {
            when (payloads[0]) {
                Payload.BASE_VALUE -> bindCurrencyValue(holder, data[position].baseValue)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }
    
    fun updateCurrencyValues(currencyInfoList: List<CurrencyInfo>) {
        // Check for any removed currencies
        val positionsRemoved = tickerIndexMap.keys.filter { ticker ->
            return@filter !currencyInfoList.any { currencyInfo -> currencyInfo.ticker == ticker }
        }.toCollection(ArrayList())
        positionsRemoved.forEach {
            val position = tickerIndexMap[it]
            notifyItemRemoved(position!!)
            data.removeAt(position)
            tickerIndexMap.remove(it)
        }

        // check for updated and added currencies
        val numPreviousSize = data.size
        currencyInfoList.forEach { currencyInfo ->
            val index = tickerIndexMap[currencyInfo.ticker]
            if (index != null) {
                if (index != 0) {
                    data[index].baseValue = currencyInfo.baseValue
                    notifyItemChanged(index, Payload.BASE_VALUE)
                }
            } else {
                data.add(currencyInfo)
                tickerIndexMap[currencyInfo.ticker] = data.size - 1
            }
        }

        if (numPreviousSize < data.size) {
            notifyItemRangeInserted(numPreviousSize, data.size)
        }
    }

    fun getBaseCurrency(): CurrencyInfo? {
        return if (data.size > 0) data[0] else null
    }

    private fun bindCurrencyValue(holder: ConversionRowHolder, value: Double?) {
        val text = if (value == null) "" else "%.2f".format(value)
        holder.etCurrencyValue.setText(text)
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ConversionRowHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener {
        val ivCurrencyImage: ImageView = view.iv_currencyImage
        val tvCurrencyTicker: TextView = view.tv_currencyTicker
        val tvCurrencyFullName: TextView = view.tv_currencyFullName
        val etCurrencyValue: EditText = view.et_currencyValue

        init {
            etCurrencyValue.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    etCurrencyValue.clearFocus()
                }

                return@setOnEditorActionListener false
            }

            view.setOnClickListener(this)

            etCurrencyValue.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    if (adapterPosition != 0) {
                        return
                    }
                    presenter.resumeUpdates()
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                    if (adapterPosition != 0) {
                        return
                    }
                    presenter.pauseUpdates()
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (adapterPosition != 0) {
                        return
                    }
                    try {
                        val num = if (s.isBlank()) null else parseDouble(s.toString())
                        recyclerView.post {
                            val row = data[adapterPosition]
                            row.baseValue = num
                            presenter.calculateCurrencyValuesForBase(row.ticker, row.baseValue)
                        }
                    } catch (e: NumberFormatException) {

                    }
                }
            })

            etCurrencyValue.setOnTouchListener { _, event ->
                if (MotionEvent.ACTION_UP == event.action) {
                    moveRowToTop()
                }

                return@setOnTouchListener false
            }
        }

        override fun onClick(v: View?) {
            v?.let {
                moveRowToTop()

                if (!etCurrencyValue.hasFocus()) {
                    etCurrencyValue.requestFocus()
                }
                etCurrencyValue.showKeyboard()
            }
        }

        private fun moveRowToTop() {
            if (adapterPosition == 0) {
                return
            }

            recyclerView.post {
                val oldPosition = adapterPosition
                val row = data[oldPosition]
                presenter.calculateCurrencyValuesForBase(row.ticker, row.baseValue)
                data.removeAt(oldPosition)
                data.add(0, row)

                tickerIndexMap = data.mapIndexed { index, currencyInfo -> currencyInfo.ticker to index }
                    .toMap()
                    .toMutableMap()
                notifyItemMoved(oldPosition, 0).also {
                    recyclerView.scrollToPosition(0)
                }
            }
        }
    }
}