package com.listanomade.app.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
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
    private val onPurchasedChanged: (ShoppingItem, Boolean) -> Unit,
    private val onEditItem: (ShoppingItem) -> Unit,
    private val onDeleteItem: (ShoppingItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var categories: List<CategoryList> = emptyList()

    fun submitCategories(value: List<CategoryList>) {
        categories = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun getItemCount(): Int = categories.size

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.textCategoryName)
        private val status: TextView = view.findViewById(R.id.textCategoryStatus)
        private val total: TextView = view.findViewById(R.id.textCategoryTotal)
        private val empty: TextView = view.findViewById(R.id.textEmpty)
        private val addItem: Button = view.findViewById(R.id.buttonCategoryAddItem)
        private val more: ImageButton = view.findViewById(R.id.buttonCategoryMore)
        private val recycler: RecyclerView = view.findViewById(R.id.recyclerItems)
        private val itemAdapter = ItemAdapter(onPurchasedChanged, onEditItem, onDeleteItem)

        init {
            recycler.layoutManager = LinearLayoutManager(itemView.context)
            recycler.adapter = itemAdapter
            recycler.isNestedScrollingEnabled = false
        }

        fun bind(categoryList: CategoryList) {
            name.text = categoryList.category.name
            status.text = itemView.context.getString(
                R.string.pending_items,
                categoryList.pendingCount,
                categoryList.purchasedCount
            )
            total.text = MoneyFormatter.format(categoryList.totalCents)
            empty.visibility = if (categoryList.items.isEmpty()) View.VISIBLE else View.GONE
            recycler.visibility = if (categoryList.items.isEmpty()) View.GONE else View.VISIBLE
            itemAdapter.submitItems(categoryList.items)
            addItem.setOnClickListener { onAddItem(categoryList.category.id) }
            more.setOnClickListener { onMore(it, categoryList) }
        }
    }
}
