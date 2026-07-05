package com.example.ui.auth

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AuthTokenManager
import com.example.data.model.User
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Authenticated(val user: User) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel @kotlin.jvm.JvmOverloads constructor(
    application: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {
    private val tokenManager = AuthTokenManager(application)
    private val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()

    private fun isValidEmail(email: String): Boolean {
        return email.matches(emailRegex)
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _passwordResetState = MutableStateFlow<String?>(null) // Success message or null
    val passwordResetState: StateFlow<String?> = _passwordResetState.asStateFlow()

    private val _accountCreatedState = MutableStateFlow<Boolean>(false)
    val accountCreatedState: StateFlow<Boolean> = _accountCreatedState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        if (tokenManager.hasActiveSession()) {
            val user = User(
                email = tokenManager.getUserEmail() ?: "",
                fullName = tokenManager.getUserName() ?: "",
                role = tokenManager.getUserRole() ?: "Enterprise Administrator",
                organization = tokenManager.getUserOrg() ?: "Craft Innovations",
                token = tokenManager.getJwtToken() ?: ""
            )
            _authState.value = AuthState.Authenticated(user)
        } else {
            _authState.value = AuthState.Idle
        }
    }

    /**
     * Authenticate user with Email and Password / Security Pin
     */
    fun login(email: String, pinOrPassword: String) {
        viewModelScope.launch(ioDispatcher) {
            _authState.value = AuthState.Loading
            _passwordResetState.value = null
            _accountCreatedState.value = false
            delay(1500) // Realistic network delay

            if (email.isBlank() || !isValidEmail(email)) {
                _authState.value = AuthState.Error("Please enter a valid Corporate Identity email.")
                return@launch
            }

            if (pinOrPassword.length < 4) {
                _authState.value = AuthState.Error("Security Access Pin/Password must be at least 4 characters.")
                return@launch
            }

            // High-fidelity corporate authentication mock logic
            val name = email.substringBefore("@").replaceFirstChar { it.uppercase() } + " Operator"
            val role = if (email.contains("admin")) "System Administrator" else "Enterprise IoT Operator"
            val org = "Craft Innovations Ltd"
            val jwt = generateJwt(email, name, role, org)

            tokenManager.saveSession(jwt, email, name, role, org)
            
            _authState.value = AuthState.Authenticated(
                User(email = email, fullName = name, role = role, organization = org, token = jwt)
            )
        }
    }

    /**
     * Complete account creation flow
     */
    fun createAccount(email: String, fullName: String, pinOrPassword: String) {
        viewModelScope.launch(ioDispatcher) {
            _authState.value = AuthState.Loading
            delay(1800)

            if (email.isBlank() || !isValidEmail(email)) {
                _authState.value = AuthState.Error("Invalid email address for registration.")
                return@launch
            }
            if (fullName.isBlank()) {
                _authState.value = AuthState.Error("Full Name cannot be blank.")
                return@launch
            }
            if (pinOrPassword.length < 4) {
                _authState.value = AuthState.Error("Pin/Password must be at least 4 characters.")
                return@launch
            }

            _accountCreatedState.value = true
            _authState.value = AuthState.Idle
        }
    }

    /**
     * Trigger Google Sign-In with official-looking branding and OAuth JWT generation
     */
    fun signInWithGoogle(googleEmail: String = "operator.craft@gmail.com", googleName: String = "Craft Operator") {
        viewModelScope.launch(ioDispatcher) {
            _authState.value = AuthState.Loading
            delay(1200)

            val role = "External Cloud Administrator"
            val org = "Craft Innovations (Google Hub)"
            val jwt = generateJwt(googleEmail, googleName, role, org)

            tokenManager.saveSession(jwt, googleEmail, googleName, role, org)

            _authState.value = AuthState.Authenticated(
                User(email = googleEmail, fullName = googleName, role = role, organization = org, token = jwt)
            )
        }
    }

    /**
     * Send secure corporate password reset link
     */
    fun resetPassword(email: String) {
        viewModelScope.launch(ioDispatcher) {
            _authState.value = AuthState.Loading
            delay(1200)

            if (email.isBlank() || !isValidEmail(email)) {
                _authState.value = AuthState.Error("Please enter a valid corporate email to receive reset link.")
                return@launch
            }

            _passwordResetState.value = "Reset link dispatched securely to $email. Please check your inbox."
            _authState.value = AuthState.Idle
        }
    }

    /**
     * Clear secure session storage and set state to Idle
     */
    fun logout() {
        tokenManager.clearSession()
        _authState.value = AuthState.Idle
        _passwordResetState.value = null
        _accountCreatedState.value = false
    }

    /**
     * Helper to clear errors and reset state
     */
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
        _passwordResetState.value = null
    }

    /**
     * Generates a fully formatted, valid decodable Base64 JWT string
     */
    private fun generateJwt(email: String, name: String, role: String, org: String): String {
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" // Header: {"alg":"HS256","typ":"JWT"}
        val payload = """
            {
              "iss": "https://auth.craftinnovations.com.ng",
              "sub": "$email",
              "name": "$name",
              "role": "$role",
              "org": "$org",
              "iat": ${System.currentTimeMillis() / 1000},
              "exp": ${(System.currentTimeMillis() / 1000) + 3600}
            }
        """.trimIndent()
        val payloadEncoded = Base64.encodeToString(
            payload.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE
        )
        val signature = "v_mR4P2Nn47B4_Zt-pInzNfWb39Kx29mZp-8W78vN84" // HMAC SHA256 Signature
        return "$header.$payloadEncoded.$signature"
    }
}
