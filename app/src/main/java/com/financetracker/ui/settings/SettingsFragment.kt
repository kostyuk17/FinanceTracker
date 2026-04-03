package com.financetracker.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.financetracker.FinanceTrackerApp
import com.financetracker.R
import com.financetracker.data.model.TransactionType
import com.financetracker.data.repository.MockDataRepository
import com.financetracker.ui.auth.LoginActivity
import com.financetracker.utils.ExportUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private lateinit var tvInitials: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvCurrentName: TextView
    private lateinit var tvCategoriesCount: TextView
    private lateinit var tvVersion: TextView

    private var currentUserId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        tvInitials = view.findViewById(R.id.tvInitials)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        tvCurrentName = view.findViewById(R.id.tvCurrentName)
        tvCategoriesCount = view.findViewById(R.id.tvCategoriesCount)
        tvVersion = view.findViewById(R.id.tvVersion)

        currentUserId = (requireActivity().application as FinanceTrackerApp).getCurrentUserId()

        // Кліки
        view.findViewById<LinearLayout>(R.id.btnChangeName).setOnClickListener { showChangeNameDialog() }
        view.findViewById<LinearLayout>(R.id.btnChangePassword).setOnClickListener { showChangePasswordDialog() }
        view.findViewById<LinearLayout>(R.id.btnCategories).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_categories)
        }
        view.findViewById<LinearLayout>(R.id.btnExport).setOnClickListener { exportAllData() }
        view.findViewById<LinearLayout>(R.id.btnClearData).setOnClickListener { showClearDataDialog() }
        view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener { showLogoutDialog() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserInfo()
        loadCategoriesCount()
        loadVersion()
    }

    override fun onResume() {
        super.onResume()
        loadUserInfo()
        loadCategoriesCount()
    }

    // ════════════════════════════════════════════════════════════════
    //  ЗАВАНТАЖЕННЯ ДАНИХ
    // ════════════════════════════════════════════════════════════════

    private fun loadUserInfo() {
        lifecycleScope.launch {
            val user = MockDataRepository.getUserById(currentUserId) ?: return@launch
            tvUserName.text = user.username
            tvUserEmail.text = user.email
            tvCurrentName.text = user.username

            // Ініціали
            val initials = user.username
                .split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
            tvInitials.text = if (initials.isNotEmpty()) initials else user.username.take(2).uppercase()
        }
    }

    private fun loadCategoriesCount() {
        lifecycleScope.launch {
            val count = MockDataRepository.getAllCategories().size
            tvCategoriesCount.text = getString(R.string.categories_count, count)
        }
    }

    private fun loadVersion() {
        try {
            val packageInfo = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            tvVersion.text = getString(R.string.app_version, packageInfo.versionName)
        } catch (_: Exception) {
            tvVersion.text = getString(R.string.app_version, "1.0")
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ЗМІНА ІМЕНІ
    // ════════════════════════════════════════════════════════════════

    private fun showChangeNameDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_change_name, null)
        val etName = dialogView.findViewById<EditText>(R.id.etNewName)
        etName.setText(tvCurrentName.text)
        etName.selectAll()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.change_name))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newName = etName.text.toString().trim()
                if (newName.length < 2) {
                    Snackbar.make(requireView(), getString(R.string.error_name_short), Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val user = MockDataRepository.getUserById(currentUserId) ?: return@launch
                    MockDataRepository.updateUser(user.copy(username = newName))
                    loadUserInfo()
                    Snackbar.make(requireView(), getString(R.string.name_changed), Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ════════════════════════════════════════════════════════════════
    //  ЗМІНА ПАРОЛЯ
    // ════════════════════════════════════════════════════════════════

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_change_password, null)
        val etOldPassword = dialogView.findViewById<TextInputEditText>(R.id.etOldPassword)
        val etNewPassword = dialogView.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val tilOld = dialogView.findViewById<TextInputLayout>(R.id.tilOldPassword)
        val tilNew = dialogView.findViewById<TextInputLayout>(R.id.tilNewPassword)
        val tilConfirm = dialogView.findViewById<TextInputLayout>(R.id.tilConfirmPassword)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.change_password))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val oldPwd = etOldPassword.text.toString()
                val newPwd = etNewPassword.text.toString()
                val confirmPwd = etConfirmPassword.text.toString()

                lifecycleScope.launch {
                    val user = MockDataRepository.getUserById(currentUserId) ?: return@launch
                    val oldHash = MockDataRepository.hashPassword(oldPwd)

                    when {
                        oldHash != user.passwordHash -> {
                            Snackbar.make(requireView(), getString(R.string.wrong_old_password), Snackbar.LENGTH_SHORT).show()
                        }
                        newPwd.length < 6 -> {
                            Snackbar.make(requireView(), getString(R.string.error_password_short), Snackbar.LENGTH_SHORT).show()
                        }
                        newPwd != confirmPwd -> {
                            Snackbar.make(requireView(), getString(R.string.error_password_mismatch), Snackbar.LENGTH_SHORT).show()
                        }
                        else -> {
                            val newHash = MockDataRepository.hashPassword(newPwd)
                            MockDataRepository.updateUser(user.copy(passwordHash = newHash))
                            Snackbar.make(requireView(), getString(R.string.password_changed), Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ════════════════════════════════════════════════════════════════
    //  ЕКСПОРТ ДАНИХ
    // ════════════════════════════════════════════════════════════════

    private fun exportAllData() {
        lifecycleScope.launch {
            val allTransactions = MockDataRepository.getAllTransactions(currentUserId)
            val catMap = MockDataRepository.getAllCategories().associateBy { it.id }
            val success = ExportUtils.exportToExcel(requireContext(), allTransactions, catMap, "all")
            Snackbar.make(
                requireView(),
                if (success) "Файл збережено у Downloads" else "Помилка експорту",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ОЧИСТКА ДАНИХ
    // ════════════════════════════════════════════════════════════════

    private fun showClearDataDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.clear_all_data))
            .setMessage(getString(R.string.clear_data_confirm))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lifecycleScope.launch {
                    MockDataRepository.clearAllTransactions(currentUserId)
                    Snackbar.make(requireView(), getString(R.string.data_cleared), Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ════════════════════════════════════════════════════════════════
    //  ВИХІД
    // ════════════════════════════════════════════════════════════════

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.logout))
            .setMessage(getString(R.string.logout_confirm))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                (requireActivity().application as FinanceTrackerApp).clearSession()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
}
