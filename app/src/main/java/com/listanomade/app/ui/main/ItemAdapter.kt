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
import com.listanomade.app.util.DateFormatter
import com.listanomade.app.util.MoneyFormatter

class ItemAdapter(
    private val onResolvedChanged: (ShoppingItem, Boolean) -> Unit,
    private val onMore: (View, ShoppingItem) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var items: List<ShoppingItem> = emptyList()

    fun submitItems(value: List<ShoppingItem>) {
        items = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder =
        ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_shopping, parent, false))

    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) = holder.bind(items[position])

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val container: LinearLayout = view.findViewById(R.id.itemContainer)
        private val check: CheckBox = view.findViewById(R.id.checkPurchased)
        private val name: TextView = view.findViewById(R.id.textItemName)
        private val unit: TextView = view.findViewById(R.id.textItemUnit)
        private val state: TextView = view.findViewById(R.id.textItemState)
        private val extra: TextView = view.findViewById(R.id.textItemExtra)
        private val total: TextView = view.findViewById(R.id.textItemTotal)
        private val more: ImageButton = view.findViewById(R.id.buttonItemMore)

        fun bind(item: ShoppingItem) {
            val context = itemView.context
            check.setOnCheckedChangeListener(null)
            check.isChecked = item.resolved
            name.text = item.name
            unit.text = context.getString(R.string.item_unit_line, item.quantity, MoneyFormatter.format(item.unitPriceCents))
            bindState(item)
            bindExtra(item)
            total.text = MoneyFormatter.format(if (item.owned) item.totalCents else item.effectiveCostCents)

            val strike = if (item.resolved) Paint.STRIKE_THRU_TEXT_FLAG else 0
            name.paintFlags = (name.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()) or strike
            container.alpha = if (item.resolved) 0.78f else 1.0f
            container.isSelected = item.resolved

            check.setOnCheckedChangeListener { _, checked -> onResolvedChanged(item, checked) }
            more.setOnClickListener { onMore(it, item) }
        }

        private fun bindState(item: ShoppingItem) {
            val context = itemView.context
            when {
                item.owned -> {
                    state.setText(R.string.already_have)
                    state.setTextColor(context.getColor(R.color.status_owned))
                }
                item.fullyPurchased -> {
                    state.setText(R.string.purchased)
                    state.setTextColor(context.getColor(R.color.status_purchased))
                }
                item.partial -> {
                    state.text = context.getString(R.string.partial_state, item.boughtQuantity, item.quantity)
                    state.setTextColor(context.getColor(R.color.status_pending))
                }
                else -> {
                    state.setText(R.string.not_purchased)
                    state.setTextColor(context.getColor(R.color.status_pending))
                }
            }
        }

        private fun bindExtra(item: ShoppingItem) {
            val context = itemView.context
            val parts = mutableListOf(priorityLabel(item))
            if (item.store.isNotBlank()) parts += item.store
            if (item.targetDateMillis > 0L) parts += context.getString(R.string.target_date_line, DateFormatter.format(item.targetDateMillis))
            if (item.boughtQuantity > 0 && item.actualUnitPriceCents > 0L) {
                parts += context.getString(R.string.price_paid_line, MoneyFormatter.format(item.actualUnitPriceCents))
                when {
                    item.savingsCents > 0L -> parts += context.getString(R.string.price_saved_line, MoneyFormatter.format(item.savingsCents))
                    item.savingsCents < 0L -> parts += context.getString(R.string.price_over_line, MoneyFormatter.format(-item.savingsCents))
                }
            }
            extra.text = parts.joinToString(" • ")
        }

        private fun priorityLabel(item: ShoppingItem): String = itemView.context.getString(
            when (item.priority) {
                ShoppingItem.PRIORITY_ESSENTIAL -> R.string.priority_essential
                ShoppingItem.PRIORITY_OPTIONAL -> R.string.priority_optional
                else -> R.string.priority_important
            }
        )
    }
}
