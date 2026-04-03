package com.financetracker.ui.settings

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.financetracker.R
import com.financetracker.data.model.Category
import com.financetracker.data.model.TransactionType
import com.financetracker.data.repository.MockDataRepository
import com.financetracker.ui.adapter.CategoryManageAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class CategoriesFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var rvCategories: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var adapter: CategoryManageAdapter

    private var currentType: TransactionType = TransactionType.EXPENSE

    private val colorPalette = listOf(
        "#EF6C00", "#1565C0", "#6A1B9A", "#D32F2F", "#2E7D32",
        "#FF6D00", "#0D47A1", "#5F6368", "#1B5E20", "#FF8A80"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_categories, container, false)
        tabLayout = view.findViewById(R.id.tabLayout)
        rvCategories = view.findViewById(R.id.rvCategories)
        fabAdd = view.findViewById(R.id.fabAddCategory)

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener { findNavController().navigateUp() }

        adapter = CategoryManageAdapter(
            onEdit = { cat -> showEditDialog(cat) },
            onDelete = { cat -> showDeleteDialog(cat) }
        )
        rvCategories.layoutManager = LinearLayoutManager(requireContext())
        rvCategories.adapter = adapter

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentType = if (tab.position == 0) TransactionType.EXPENSE else TransactionType.INCOME
                loadCategories()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        fabAdd.setOnClickListener { showAddDialog() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val cats = MockDataRepository.getCategoriesByType(currentType)
            adapter.submitList(cats)
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ДІАЛОГ ДОДАВАННЯ
    // ════════════════════════════════════════════════════════════════

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        val etName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val colorContainer = dialogView.findViewById<LinearLayout>(R.id.colorPalette)
        val chipGroupType = dialogView.findViewById<ChipGroup>(R.id.chipGroupType)
        val chipExpense = dialogView.findViewById<Chip>(R.id.chipExpense)
        val chipIncome = dialogView.findViewById<Chip>(R.id.chipIncome)

        // Встановлюємо тип відповідно до поточної вкладки
        if (currentType == TransactionType.INCOME) chipIncome.isChecked = true
        else chipExpense.isChecked = true

        var selectedColor = colorPalette[0]
        buildColorPalette(colorContainer, selectedColor) { color -> selectedColor = color }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.add_category))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton

                val type = if (chipIncome.isChecked) TransactionType.INCOME else TransactionType.EXPENSE

                lifecycleScope.launch {
                    MockDataRepository.addCategory(
                        Category(0, name, type, R.drawable.ic_category_other, selectedColor)
                    )
                    loadCategories()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ════════════════════════════════════════════════════════════════
    //  ДІАЛОГ РЕДАГУВАННЯ
    // ════════════════════════════════════════════════════════════════

    private fun showEditDialog(category: Category) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        val etName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val colorContainer = dialogView.findViewById<LinearLayout>(R.id.colorPalette)
        val chipGroupType = dialogView.findViewById<ChipGroup>(R.id.chipGroupType)
        val chipExpense = dialogView.findViewById<Chip>(R.id.chipExpense)
        val chipIncome = dialogView.findViewById<Chip>(R.id.chipIncome)

        etName.setText(category.name)
        if (category.type == TransactionType.INCOME) chipIncome.isChecked = true
        else chipExpense.isChecked = true

        var selectedColor = category.colorHex
        buildColorPalette(colorContainer, selectedColor) { color -> selectedColor = color }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.edit_category))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton

                val type = if (chipIncome.isChecked) TransactionType.INCOME else TransactionType.EXPENSE

                lifecycleScope.launch {
                    MockDataRepository.updateCategory(
                        category.copy(name = name, type = type, colorHex = selectedColor)
                    )
                    loadCategories()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ════════════════════════════════════════════════════════════════
    //  ДІАЛОГ ВИДАЛЕННЯ
    // ════════════════════════════════════════════════════════════════

    private fun showDeleteDialog(category: Category) {
        lifecycleScope.launch {
            val count = MockDataRepository.getTransactionCountByCategory(category.id)

            val message = if (count > 0) {
                getString(R.string.category_in_use_warning, count)
            } else {
                getString(R.string.delete_category_confirm, category.name)
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.delete))
                .setMessage(message)
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    lifecycleScope.launch {
                        MockDataRepository.deleteCategory(category.id)
                        loadCategories()
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ПАЛІТРА КОЛЬОРІВ
    // ════════════════════════════════════════════════════════════════

    private fun buildColorPalette(
        container: LinearLayout,
        initialColor: String,
        onSelected: (String) -> Unit
    ) {
        container.removeAllViews()
        val views = mutableListOf<FrameLayout>()

        for (hex in colorPalette) {
            val size = (36 * resources.displayMetrics.density).toInt()
            val margin = (4 * resources.displayMetrics.density).toInt()

            val circle = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, 0, margin, 0)
                }
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(hex))
                    if (hex == initialColor) setStroke(4, Color.parseColor("#1A1C1E"))
                }
                background = bg

                setOnClickListener {
                    // Скидаємо всі обведення
                    views.forEach { v ->
                        val tag = v.tag as String
                        (v.background as? GradientDrawable)?.setStroke(0, Color.TRANSPARENT)
                        (v.background as? GradientDrawable)?.setColor(Color.parseColor(tag))
                    }
                    // Виділяємо поточний
                    (background as? GradientDrawable)?.setStroke(4, Color.parseColor("#1A1C1E"))
                    onSelected(hex)
                }
                tag = hex
            }
            views.add(circle)
            container.addView(circle)
        }
    }
}
