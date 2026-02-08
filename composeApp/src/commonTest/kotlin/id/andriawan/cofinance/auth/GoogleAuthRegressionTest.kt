package id.andriawan.cofinance.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression tests for Google Authentication
 * Tests to prevent regression of fixed bugs and ensure backward compatibility
 */
class GoogleAuthRegressionTest {

    @Test
    fun testSuccessResultPreservesIdTokenIntegrity() {
        // Regression: Ensure idToken is never modified or corrupted
        val originalToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature"
        val result = GoogleAuthResult.Success(originalToken, "user@example.com")

        assertEquals(originalToken, result.idToken)
        assertTrue(result.idToken.contains("."))
        assertFalse(result.idToken.isEmpty())
    }

    @Test
    fun testSuccessResultHandlesNullEmailProperly() {
        // Regression: Ensure null email doesn't cause NPE or unexpected behavior
        val result = GoogleAuthResult.Success("token", null)

        assertNotNull(result.idToken)
        assertNull(result.email)
        assertTrue(result is GoogleAuthResult.Success)
    }

    @Test
    fun testErrorResultPreservesOriginalException() {
        // Regression: Ensure exception chain is preserved
        val originalException = IllegalStateException("Original error")
        val result = GoogleAuthResult.Error("Wrapped error", originalException)

        assertNotNull(result.exception)
        assertEquals(originalException, result.exception)
        assertEquals("Original error", result.exception?.message)
    }

    @Test
    fun testErrorResultWithNullExceptionDoesNotCrash() {
        // Regression: Null exception should be handled gracefully
        val result = GoogleAuthResult.Error("Error message", null)

        assertNotNull(result.message)
        assertNull(result.exception)
        assertTrue(result is GoogleAuthResult.Error)
    }

    @Test
    fun testCancelledResultIsConsistentSingleton() {
        // Regression: Cancelled should always be the same object
        val cancelled1 = GoogleAuthResult.Cancelled
        val cancelled2 = GoogleAuthResult.Cancelled

        assertTrue(cancelled1 === cancelled2)
        assertEquals(cancelled1, cancelled2)
    }

    @Test
    fun testWhenExpressionDoesNotMissAnyCase() {
        // Regression: Ensure all result types are handled in when expressions
        val results = listOf(
            GoogleAuthResult.Success("token", "email"),
            GoogleAuthResult.Error("error"),
            GoogleAuthResult.Cancelled
        )

        results.forEach { result ->
            // This should not throw when expression not exhaustive error
            val handled = when (result) {
                is GoogleAuthResult.Success -> "success"
                is GoogleAuthResult.Error -> "error"
                is GoogleAuthResult.Cancelled -> "cancelled"
            }
            assertNotNull(handled)
        }
    }

    @Test
    fun testSuccessResultWithEmptyEmailString() {
        // Regression: Empty string email should be different from null
        val resultWithEmpty = GoogleAuthResult.Success("token", "")
        val resultWithNull = GoogleAuthResult.Success("token", null)

        assertNotNull(resultWithEmpty.email)
        assertNull(resultWithNull.email)
        assertEquals("", resultWithEmpty.email)
    }

    @Test
    fun testErrorResultWithEmptyMessage() {
        // Regression: Empty error message should be allowed
        val result = GoogleAuthResult.Error("")

        assertEquals("", result.message)
        assertTrue(result.message.isEmpty())
        assertFalse(result.message.isBlank())  // Empty string, not whitespace
    }

    @Test
    fun testMultipleSuccessResultsAreIndependent() {
        // Regression: Multiple instances should not share state
        val result1 = GoogleAuthResult.Success("token1", "email1@example.com")
        val result2 = GoogleAuthResult.Success("token2", "email2@example.com")

        assertNotEquals(result1.idToken, result2.idToken)
        assertNotEquals(result1.email, result2.email)
    }

    @Test
    fun testErrorResultWithLongStackTrace() {
        // Regression: Long exception chains should be preserved
        var currentException: Exception = Exception("Root cause")
        repeat(10) { i ->
            currentException = Exception("Level $i", currentException)
        }

        val result = GoogleAuthResult.Error("Deep exception", currentException)

        assertNotNull(result.exception)
        var depth = 0
        var ex: Throwable? = result.exception
        while (ex != null) {
            depth++
            ex = ex.cause
        }
        assertEquals(11, depth) // 10 levels + 1 root
    }

    @Test
    fun testSuccessResultWithVeryLongEmail() {
        // Regression: Long email addresses should be handled
        val longEmail = "very.long.email.address.that.might.cause.issues@" + "subdomain.".repeat(10) + "example.com"
        val result = GoogleAuthResult.Success("token", longEmail)

        assertEquals(longEmail, result.email)
        assertTrue(result.email!!.length > 100)
    }

    @Test
    fun testErrorMessageWithUnicodeCharacters() {
        // Regression: Unicode should be preserved in error messages
        val unicodeMessage = "エラー: 認証に失敗しました 🔒"
        val result = GoogleAuthResult.Error(unicodeMessage)

        assertEquals(unicodeMessage, result.message)
        assertTrue(result.message.contains("🔒"))
        assertTrue(result.message.contains("エラー"))
    }

    @Test
    fun testSuccessResultWithSpecialCharactersInToken() {
        // Regression: JWT tokens can contain special characters
        val tokenWithSpecialChars = "eyJhbGc++==/+iOiJSUzI1NiIsInR5cCI6IkpXVCJ9"
        val result = GoogleAuthResult.Success(tokenWithSpecialChars, "user@example.com")

        assertEquals(tokenWithSpecialChars, result.idToken)
        assertTrue(result.idToken.contains("+"))
        assertTrue(result.idToken.contains("="))
        assertTrue(result.idToken.contains("/"))
    }

    @Test
    fun testErrorResultDifferentExceptionTypesArePreserved() {
        // Regression: Different exception types should maintain their identity
        val exceptions = mapOf(
            IllegalArgumentException("Arg error") to "IllegalArgumentException",
            IllegalStateException("State error") to "IllegalStateException",
            RuntimeException("Runtime error") to "RuntimeException",
            Exception("Generic error") to "Exception"
        )

        exceptions.forEach { (exception, expectedType) ->
            val result = GoogleAuthResult.Error("Error", exception)
            assertNotNull(result.exception)
            assertTrue(result.exception!!::class.simpleName == expectedType)
        }
    }

    @Test
    fun testResultsInCollectionMaintainType() {
        // Regression: Results in collections should maintain their sealed class type
        val results: List<GoogleAuthResult> = listOf(
            GoogleAuthResult.Success("token", "email"),
            GoogleAuthResult.Error("error"),
            GoogleAuthResult.Cancelled
        )

        assertEquals(3, results.size)
        results.forEach { result ->
            assertTrue(result is GoogleAuthResult)
        }
    }

    @Test
    fun testSuccessResultEmailCasePreservation() {
        // Regression: Email case should be preserved as provided
        val mixedCaseEmail = "User.Name@Example.COM"
        val result = GoogleAuthResult.Success("token", mixedCaseEmail)

        assertEquals(mixedCaseEmail, result.email)
        assertTrue(result.email!!.contains("User"))
        assertTrue(result.email!!.contains("Example"))
        assertTrue(result.email!!.contains("COM"))
    }

    @Test
    fun testErrorResultMessageNewlinesPreserved() {
        // Regression: Multiline error messages should preserve formatting
        val multilineMessage = "Error occurred:\n- Reason 1\n- Reason 2\n- Reason 3"
        val result = GoogleAuthResult.Error(multilineMessage)

        assertEquals(multilineMessage, result.message)
        assertEquals(3, result.message.count { it == '\n' })
    }

    @Test
    fun testGoogleAuthManagerMultipleInstantiations() {
        // Regression: Creating multiple instances should not fail
        val managers = List(10) { GoogleAuthManager() }

        assertEquals(10, managers.size)
        managers.forEach { manager ->
            assertNotNull(manager)
        }
    }

    @Test
    fun testGoogleAuthManagerSignOutIdempotency() {
        // Regression: Multiple signOut calls should be safe
        val manager = GoogleAuthManager()

        repeat(100) {
            manager.signOut()
        }

        // Should complete without exception
        assertTrue(true)
    }

    @Test
    fun testResultTypeDiscrimination() {
        // Regression: Type checking should work correctly
        val success: GoogleAuthResult = GoogleAuthResult.Success("token", "email")
        val error: GoogleAuthResult = GoogleAuthResult.Error("error")
        val cancelled: GoogleAuthResult = GoogleAuthResult.Cancelled

        assertTrue(success is GoogleAuthResult.Success)
        assertFalse(success is GoogleAuthResult.Error)
        assertFalse(success is GoogleAuthResult.Cancelled)

        assertTrue(error is GoogleAuthResult.Error)
        assertFalse(error is GoogleAuthResult.Success)

        assertTrue(cancelled is GoogleAuthResult.Cancelled)
        assertFalse(cancelled is GoogleAuthResult.Success)
    }

    @Test
    fun testSuccessResultWithWhitespaceInEmail() {
        // Regression: Whitespace in email should be preserved (validation is elsewhere)
        val emailWithSpaces = " user@example.com "
        val result = GoogleAuthResult.Success("token", emailWithSpaces)

        assertEquals(emailWithSpaces, result.email)
        assertTrue(result.email!!.startsWith(" "))
        assertTrue(result.email!!.endsWith(" "))
    }

    @Test
    fun testErrorResultWithVeryLongMessage() {
        // Regression: Very long error messages should not be truncated
        val longMessage = "Error: " + "This is a very long error message. ".repeat(100)
        val result = GoogleAuthResult.Error(longMessage)

        assertEquals(longMessage, result.message)
        assertTrue(result.message.length > 3000)
    }
}