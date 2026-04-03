package com.financetracker.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.financetracker.FinanceTrackerApp
import com.financetracker.databinding.ActivityRegisterBinding
import com.financetracker.ui.MainActivity
import com.google.android.material.snackbar.Snackbar

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    // ════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    // ════════════════════════════════════════════════════════════════
    //  SETUP
    // ════════════════════════════════════════════════════════════════

    private fun setupListeners() {
        // Кнопка реєстрації
        binding.btnRegister.setOnClickListener {
            clearErrors()

            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etPasswordConfirm.text.toString()

            // Локальна валідація з показом помилок на полях
            val validation = viewModel.validateRegisterFields(
                name, email, password, confirmPassword
            )
            if (!validation.isValid) {
                showFieldErrors(validation)
                return@setOnClickListener
            }

            viewModel.register(name, email, password, confirmPassword)
        }

        // Перехід на вхід
        binding.btnGoToLogin.setOnClickListener {
            finish()
        }

        // «Done» на останньому полі запускає реєстрацію
        binding.etPasswordConfirm.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btnRegister.performClick()
                true
            } else false
        }

        // Очищення помилок при фокусі
        binding.etName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilName.error = null
        }
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilEmail.error = null
        }
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilPassword.error = null
        }
        binding.etPasswordConfirm.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilPasswordConfirm.error = null
        }
    }

    private fun observeState() {
        viewModel.registerState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Idle -> {
                    setLoading(false)
                }
                is AuthViewModel.AuthState.Loading -> {
                    setLoading(true)
                }
                is AuthViewModel.AuthState.Success -> {
                    setLoading(false)
                    // Автоматичний вхід після реєстрації
                    (application as FinanceTrackerApp).saveSession(state.user.id)
                    navigateToMain()
                }
                is AuthViewModel.AuthState.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.resetRegisterState()
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ДОПОМІЖНІ МЕТОДИ
    // ════════════════════════════════════════════════════════════════

    private fun showFieldErrors(validation: AuthViewModel.ValidationResult) {
        binding.tilName.error = validation.nameError
        binding.tilEmail.error = validation.emailError
        binding.tilPassword.error = validation.passwordError
        binding.tilPasswordConfirm.error = validation.confirmPasswordError
    }

    private fun clearErrors() {
        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilPasswordConfirm.error = null
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
        binding.etName.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etPasswordConfirm.isEnabled = !isLoading
        binding.btnGoToLogin.isEnabled = !isLoading
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
