package com.listanomade.app.ui.main

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.listanomade.app.R
import com.listanomade.app.model.ShoppingItem
import com.listanomade.app.util.MoneyFormatter

class ItemAdapter(
    private val onPurchasedChanged: (ShoppingItem, Boolean) -> Unit,
    private val onMore: (View, ShoppingItem) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var items: List<ShoppingItem> = emptyList()

    fun submitItems(value: List<ShoppingItem>) {
        items = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shopping, parent, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val container: LinearLayout = view.findViewById(R.id.itemContainer)
        private val check: CheckBox = view.findViewById(R.id.checkPurchased)
        private val name: TextView = view.findViewById(R.id.textItemName)
        private val unit: TextView = view.findViewById(R.id.textItemUnit)
        private val state: TextView = view.findViewById(R.id.textItemState)
        private val total: TextView = view.findViewById(R.id.textItemTotal)
        private val more: ImageButton = view.findViewById(R.id.buttonItemMore)

        fun bind(item: ShoppingItem) {
            check.setOnCheckedChangeListener(null)
            check.isChecked = item.purchased
            name.text = item.name
            unit.text = itemView.context.getString(
                R.string.item_unit_line,
                item.quantity,
                MoneyFormatter.format(item.unitPriceCents)
            )
            state.setText(if (item.purchased) R.string.purchased else R.string.not_purchased)
            state.setTextColor(
                itemView.context.getColor(
                    if (item.purchased) R.color.status_purchased else R.color.status_pending
                )
            )
            total.text = MoneyFormatter.format(item.totalCents)

            val strike = if (item.purchased) Paint.STRIKE_THRU_TEXT_FLAG else 0
            name.paintFlags = (name.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()) or strike
            container.alpha = if (item.purchased) 0.78f else 1.0f
            container.isSelected = item.purchased

            check.setOnCheckedChangeListener { _, checked -> onPurchasedChanged(item, checked) }
            more.setOnClickListener { onMore(it, item) }
        }
    }
}
