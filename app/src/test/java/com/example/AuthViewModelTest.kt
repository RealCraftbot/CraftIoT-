package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AuthTokenManager
import com.example.ui.auth.AuthState
import com.example.ui.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthViewModelTest {

    private lateinit var application: Application
    private lateinit var tokenManager: AuthTokenManager
    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        tokenManager = AuthTokenManager(application)
        tokenManager.clearSession()
        viewModel = AuthViewModel(application, testDispatcher)
    }

    @After
    fun tearDown() {
        tokenManager.clearSession()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateIsIdle() {
        assertEquals(AuthState.Idle, viewModel.authState.value)
    }

    @Test
    fun testLoginValidCredentials() = runTest(testDispatcher) {
        viewModel.login("admin.operator@craftinnovations.com.ng", "12345")
        
        // Assert state is loading immediately
        runCurrent()
        assertEquals(AuthState.Loading, viewModel.authState.value)
        
        // Advance time to bypass delay(1500)
        advanceTimeBy(1600)
        
        // Assert authenticated successfully
        val currentState = viewModel.authState.value
        assertTrue(currentState is AuthState.Authenticated)
        
        val user = (currentState as AuthState.Authenticated).user
        assertEquals("admin.operator@craftinnovations.com.ng", user.email)
        assertEquals("System Administrator", user.role)
        assertTrue(tokenManager.hasActiveSession())
    }

    @Test
    fun testLoginInvalidEmailFormat() = runTest(testDispatcher) {
        viewModel.login("invalid-email", "12345")
        advanceTimeBy(1600)
        
        val currentState = viewModel.authState.value
        assertTrue(currentState is AuthState.Error)
        assertEquals("Please enter a valid Corporate Identity email.", (currentState as AuthState.Error).message)
    }

    @Test
    fun testGoogleSignIn() = runTest(testDispatcher) {
        viewModel.signInWithGoogle("test.google@gmail.com", "Google Admin")
        runCurrent()
        assertEquals(AuthState.Loading, viewModel.authState.value)
        
        advanceTimeBy(1300)
        
        val currentState = viewModel.authState.value
        assertTrue(currentState is AuthState.Authenticated)
        
        val user = (currentState as AuthState.Authenticated).user
        assertEquals("test.google@gmail.com", user.email)
        assertEquals("Google Admin", user.fullName)
        assertEquals("External Cloud Administrator", user.role)
    }

    @Test
    fun testLogoutClearsSession() = runTest(testDispatcher) {
        // First log in
        viewModel.signInWithGoogle()
        advanceTimeBy(1300)
        assertTrue(tokenManager.hasActiveSession())
        
        // Then log out
        viewModel.logout()
        assertEquals(AuthState.Idle, viewModel.authState.value)
        assertFalse(tokenManager.hasActiveSession())
    }

    @Test
    fun testPasswordResetRequest() = runTest(testDispatcher) {
        viewModel.resetPassword("operator@craftinnovations.com.ng")
        advanceTimeBy(1300)
        
        assertNotNull(viewModel.passwordResetState.value)
        assertTrue(viewModel.passwordResetState.value!!.contains("operator@craftinnovations.com.ng"))
    }
}
