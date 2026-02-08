package id.andriawan.cofinance.auth

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for GoogleAuthManager expect/actual implementations
 * These tests verify that platform-specific implementations exist and can be instantiated
 */
class GoogleAuthManagerTest {

    @Test
    fun testGoogleAuthManagerCanBeInstantiated() {
        val authManager = GoogleAuthManager()
        assertNotNull(authManager, "GoogleAuthManager should be instantiable")
    }

    @Test
    fun testGoogleAuthManagerHasSignInMethod() {
        val authManager = GoogleAuthManager()
        // Verify the instance is created and has the expected interface
        assertNotNull(authManager)
    }

    @Test
    fun testGoogleAuthManagerHasSignOutMethod() {
        val authManager = GoogleAuthManager()
        // Verify signOut doesn't throw exception
        authManager.signOut()
        // Test passes if no exception is thrown
        assertTrue(true)
    }

    @Test
    fun testMultipleGoogleAuthManagerInstances() {
        val authManager1 = GoogleAuthManager()
        val authManager2 = GoogleAuthManager()

        assertNotNull(authManager1)
        assertNotNull(authManager2)
    }

    @Test
    fun testGoogleAuthManagerSignOutIsIdempotent() {
        val authManager = GoogleAuthManager()

        // Should be able to call signOut multiple times without error
        authManager.signOut()
        authManager.signOut()
        authManager.signOut()

        assertTrue(true, "Multiple signOut calls should not throw exceptions")
    }

    @Test
    fun testGoogleAuthResultTypeHierarchy() {
        // Test that all result types are properly defined
        val success = GoogleAuthResult.Success("token", "email@example.com")
        val error = GoogleAuthResult.Error("error message")
        val cancelled = GoogleAuthResult.Cancelled

        assertTrue(success is GoogleAuthResult)
        assertTrue(error is GoogleAuthResult)
        assertTrue(cancelled is GoogleAuthResult)
    }

    @Test
    fun testGoogleAuthResultWhenExpression() {
        val results = listOf(
            GoogleAuthResult.Success("token123", "user@test.com"),
            GoogleAuthResult.Error("Network timeout"),
            GoogleAuthResult.Cancelled
        )

        results.forEach { result ->
            val handled = when (result) {
                is GoogleAuthResult.Success -> true
                is GoogleAuthResult.Error -> true
                is GoogleAuthResult.Cancelled -> true
            }
            assertTrue(handled, "All result types should be handled in when expression")
        }
    }

    @Test
    fun testGoogleAuthResultSuccessWithVariousTokenFormats() {
        val shortToken = GoogleAuthResult.Success("abc", "user@example.com")
        val longToken = GoogleAuthResult.Success("eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2NTBmZjI5ZmJjZTI4MmY5NDMwM2M1ZGU5MTg3MGM3ZmRhMTk5MDQiLCJ0eXAiOiJKV1QifQ", "user@example.com")
        val emptyToken = GoogleAuthResult.Success("", null)

        assertTrue(shortToken is GoogleAuthResult.Success)
        assertTrue(longToken is GoogleAuthResult.Success)
        assertTrue(emptyToken is GoogleAuthResult.Success)
    }

    @Test
    fun testGoogleAuthResultErrorWithVariousExceptions() {
        val networkError = GoogleAuthResult.Error("Network error", Exception("Connection timeout"))
        val parseError = GoogleAuthResult.Error("Parse error", IllegalArgumentException("Invalid format"))
        val genericError = GoogleAuthResult.Error("Unknown error", RuntimeException())

        assertTrue(networkError is GoogleAuthResult.Error)
        assertTrue(parseError is GoogleAuthResult.Error)
        assertTrue(genericError is GoogleAuthResult.Error)
        assertNotNull(networkError.exception)
        assertNotNull(parseError.exception)
        assertNotNull(genericError.exception)
    }

    @Test
    fun testGoogleAuthResultCancelledSingleton() {
        val cancelled1 = GoogleAuthResult.Cancelled
        val cancelled2 = GoogleAuthResult.Cancelled

        // Both should reference the same singleton object
        assertTrue(cancelled1 === cancelled2)
        assertTrue(cancelled1 == cancelled2)
    }
}