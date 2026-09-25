package com.listanomade.app.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.listanomade.app.R
import com.listanomade.app.model.CategoryList
import com.listanomade.app.model.ShoppingItem
import com.listanomade.app.util.MoneyFormatter

class CategoryAdapter(
    private val onAddItem: (Long) -> Unit,
    private val onMore: (View, CategoryList) -> Unit,
    private val onToggleCollapsed: (Long, Boolean) -> Unit,
    private val onResolvedChanged: (ShoppingItem, Boolean) -> Unit,
    private val onItemMore: (View, ShoppingItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var categories: List<CategoryList> = emptyList()

    fun submitCategories(value: List<CategoryList>) {
        categories = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        return CategoryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false))
    }

    override fun getItemCount(): Int = categories.size
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) = holder.bind(categories[position])

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val header: View = view.findViewById(R.id.categoryHeader)
        private val name: TextView = view.findViewById(R.id.textCategoryName)
        private val status: TextView = view.findViewById(R.id.textCategoryStatus)
        private val budget: TextView = view.findViewById(R.id.textCategoryBudget)
        private val total: TextView = view.findViewById(R.id.textCategoryTotal)
        private val empty: TextView = view.findViewById(R.id.textEmpty)
        private val addItem: Button = view.findViewById(R.id.buttonCategoryAddItem)
        private val more: ImageButton = view.findViewById(R.id.buttonCategoryMore)
        private val indicator: ImageView = view.findViewById(R.id.imageCollapse)
        private val recycler: RecyclerView = view.findViewById(R.id.recyclerItems)
        private val divider: View = view.findViewById(R.id.categoryDivider)
        private val footer: View = view.findViewById(R.id.categoryFooter)
        private val itemAdapter = ItemAdapter(onResolvedChanged, onItemMore)

        init {
            recycler.layoutManager = LinearLayoutManager(itemView.context)
            recycler.adapter = itemAdapter
            recycler.isNestedScrollingEnabled = false
        }

        fun bind(list: CategoryList) {
            val context = itemView.context
            name.text = list.category.name
            status.text = context.getString(
                R.string.category_summary,
                list.items.size,
                list.resolvedCount,
                list.partialCount,
                MoneyFormatter.format(list.pendingCents)
            )
            bindBudget(list)
            total.text = MoneyFormatter.format(list.totalCents)
            itemAdapter.submitItems(list.visibleItems)

            val collapsed = list.category.collapsed
            val hasVisibleItems = list.visibleItems.isNotEmpty()
            recycler.visibility = if (!collapsed && hasVisibleItems) View.VISIBLE else View.GONE
            empty.visibility = if (!collapsed && !hasVisibleItems) View.VISIBLE else View.GONE
            empty.setText(if (list.items.isEmpty()) R.string.empty_category else R.string.empty_filter)
            divider.visibility = if (collapsed) View.GONE else View.VISIBLE
            footer.visibility = if (collapsed) View.GONE else View.VISIBLE
            indicator.rotation = if (collapsed) -90f else 0f

            header.setOnClickListener { onToggleCollapsed(list.category.id, !collapsed) }
            addItem.setOnClickListener { onAddItem(list.category.id) }
            more.setOnClickListener { onMore(it, list) }
        }

        private fun bindBudget(list: CategoryList) {
            val value = list.category.budgetCents
            if (value <= 0L) {
                budget.visibility = View.GONE
                return
            }
            budget.visibility = View.VISIBLE
            val delta = value - list.totalCents
            budget.text = if (delta >= 0L) {
                itemView.context.getString(R.string.budget_remaining, MoneyFormatter.format(value), MoneyFormatter.format(delta))
            } else {
                itemView.context.getString(R.string.budget_over, MoneyFormatter.format(value), MoneyFormatter.format(-delta))
            }
            budget.setTextColor(itemView.context.getColor(if (delta >= 0L) R.color.status_purchased else R.color.danger))
        }
    }
}
