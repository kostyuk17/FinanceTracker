package com.financetracker.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.financetracker.FinanceTrackerApp
import com.financetracker.databinding.ActivityLoginBinding
import com.financetracker.ui.MainActivity
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    // ════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Автоматичний вхід, якщо сесія збережена
        if ((application as FinanceTrackerApp).isLoggedIn()) {
            navigateToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    // ════════════════════════════════════════════════════════════════
    //  SETUP
    // ════════════════════════════════════════════════════════════════

    private fun setupListeners() {
        // Кнопка входу
        binding.btnLogin.setOnClickListener {
            clearErrors()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            // Локальна валідація з показом помилок на полях
            val validation = viewModel.validateLoginFields(email, password)
            if (!validation.isValid) {
                showFieldErrors(validation)
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        // Перехід на реєстрацію
        binding.btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // «Done» на клавіатурі також запускає вхід
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btnLogin.performClick()
                true
            } else false
        }

        // Очищення помилок при наборі тексту
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilEmail.error = null
        }
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilPassword.error = null
        }
    }

    private fun observeState() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Idle -> {
                    setLoading(false)
                }
                is AuthViewModel.AuthState.Loading -> {
                    setLoading(true)
                }
                is AuthViewModel.AuthState.Success -> {
                    setLoading(false)
                    // Зберігаємо сесію
                    (application as FinanceTrackerApp).saveSession(state.user.id)
                    navigateToMain()
                }
                is AuthViewModel.AuthState.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.resetLoginState()
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ДОПОМІЖНІ МЕТОДИ
    // ════════════════════════════════════════════════════════════════

    private fun showFieldErrors(validation: AuthViewModel.ValidationResult) {
        binding.tilEmail.error = validation.emailError
        binding.tilPassword.error = validation.passwordError
    }

    private fun clearErrors() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.btnGoToRegister.isEnabled = !isLoading
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
