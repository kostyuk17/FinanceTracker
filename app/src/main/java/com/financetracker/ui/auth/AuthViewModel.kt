package com.financetracker.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.data.model.User
import com.financetracker.data.repository.MockDataRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    // ════════════════════════════════════════════════════════════════
    //  СТАНИ
    // ════════════════════════════════════════════════════════════════

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val user: User) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _loginState = MutableLiveData<AuthState>(AuthState.Idle)
    val loginState: LiveData<AuthState> = _loginState

    private val _registerState = MutableLiveData<AuthState>(AuthState.Idle)
    val registerState: LiveData<AuthState> = _registerState

    // ════════════════════════════════════════════════════════════════
    //  ВАЛІДАЦІЯ
    // ════════════════════════════════════════════════════════════════

    data class ValidationResult(
        val isValid: Boolean,
        val emailError: String? = null,
        val passwordError: String? = null,
        val nameError: String? = null,
        val confirmPasswordError: String? = null
    )

    fun validateLoginFields(email: String, password: String): ValidationResult {
        var emailError: String? = null
        var passwordError: String? = null

        if (email.isBlank()) {
            emailError = "Введіть email"
        } else if (!isValidEmail(email)) {
            emailError = "Невірний формат email"
        }

        if (password.isBlank()) {
            passwordError = "Введіть пароль"
        } else if (password.length < 6) {
            passwordError = "Пароль має містити мінімум 6 символів"
        }

        return ValidationResult(
            isValid = emailError == null && passwordError == null,
            emailError = emailError,
            passwordError = passwordError
        )
    }

    fun validateRegisterFields(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): ValidationResult {
        var nameError: String? = null
        var emailError: String? = null
        var passwordError: String? = null
        var confirmPasswordError: String? = null

        if (name.isBlank()) {
            nameError = "Введіть ім'я"
        } else if (name.trim().length < 2) {
            nameError = "Ім'я має містити мінімум 2 символи"
        }

        if (email.isBlank()) {
            emailError = "Введіть email"
        } else if (!isValidEmail(email)) {
            emailError = "Невірний формат email"
        }

        if (password.isBlank()) {
            passwordError = "Введіть пароль"
        } else if (password.length < 6) {
            passwordError = "Пароль має містити мінімум 6 символів"
        }

        if (confirmPassword.isBlank()) {
            confirmPasswordError = "Підтвердіть пароль"
        } else if (password != confirmPassword) {
            confirmPasswordError = "Паролі не співпадають"
        }

        return ValidationResult(
            isValid = nameError == null && emailError == null
                    && passwordError == null && confirmPasswordError == null,
            nameError = nameError,
            emailError = emailError,
            passwordError = passwordError,
            confirmPasswordError = confirmPasswordError
        )
    }

    // ════════════════════════════════════════════════════════════════
    //  АВТОРИЗАЦІЯ
    // ════════════════════════════════════════════════════════════════

    fun login(email: String, password: String) {
        val validation = validateLoginFields(email, password)
        if (!validation.isValid) {
            _loginState.value = AuthState.Error("Перевірте правильність введених даних")
            return
        }

        _loginState.value = AuthState.Loading
        viewModelScope.launch {
            val user = MockDataRepository.login(email.trim(), password)
            if (user != null) {
                _loginState.value = AuthState.Success(user)
            } else {
                _loginState.value = AuthState.Error("Невірний email або пароль")
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  РЕЄСТРАЦІЯ
    // ════════════════════════════════════════════════════════════════

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        val validation = validateRegisterFields(name, email, password, confirmPassword)
        if (!validation.isValid) {
            _registerState.value = AuthState.Error("Перевірте правильність введених даних")
            return
        }

        _registerState.value = AuthState.Loading
        viewModelScope.launch {
            val result = MockDataRepository.register(
                username = name.trim(),
                email = email.trim(),
                password = password
            )
            result.fold(
                onSuccess = { user ->
                    _registerState.value = AuthState.Success(user)
                },
                onFailure = { exception ->
                    _registerState.value = AuthState.Error(
                        exception.message ?: "Помилка реєстрації"
                    )
                }
            )
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  СКИДАННЯ СТАНІВ
    // ════════════════════════════════════════════════════════════════

    fun resetLoginState() {
        _loginState.value = AuthState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = AuthState.Idle
    }

    // ════════════════════════════════════════════════════════════════
    //  УТИЛІТИ
    // ════════════════════════════════════════════════════════════════

    private fun isValidEmail(email: String): Boolean {
        val pattern = android.util.Patterns.EMAIL_ADDRESS
        return pattern.matcher(email.trim()).matches()
    }
}
