package id.andriawan.cofinance.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Edge case and boundary tests for Google Authentication
 * Tests unusual inputs, boundary conditions, and potential failure scenarios
 */
class GoogleAuthEdgeCaseTest {

    @Test
    fun testSuccessWithEmptyIdToken() {
        val result = GoogleAuthResult.Success("", "user@example.com")

        assertTrue(result is GoogleAuthResult.Success)
        assertEquals("", result.idToken)
        assertEquals("user@example.com", result.email)
    }

    @Test
    fun testSuccessWithVeryLongToken() {
        val longToken = "a".repeat(10000)
        val result = GoogleAuthResult.Success(longToken, "user@example.com")

        assertTrue(result is GoogleAuthResult.Success)
        assertEquals(10000, result.idToken.length)
    }

    @Test
    fun testSuccessWithSpecialCharactersInEmail() {
        val specialEmails = listOf(
            "user+tag@example.com",
            "user.name@example.com",
            "user_name@example.com",
            "user-name@example.com",
            "123@example.com",
            "a@b.co"
        )

        specialEmails.forEach { email ->
            val result = GoogleAuthResult.Success("token", email)
            assertTrue(result is GoogleAuthResult.Success)
            assertEquals(email, result.email)
        }
    }

    @Test
    fun testSuccessWithNullEmailHandling() {
        val result = GoogleAuthResult.Success("valid_token", null)

        assertTrue(result is GoogleAuthResult.Success)
        assertNotNull(result.idToken)
        assertNull(result.email)
    }

    @Test
    fun testErrorWithEmptyMessage() {
        val result = GoogleAuthResult.Error("")

        assertTrue(result is GoogleAuthResult.Error)
        assertEquals("", result.message)
    }

    @Test
    fun testErrorWithVeryLongMessage() {
        val longMessage = "Error: " + "x".repeat(10000)
        val result = GoogleAuthResult.Error(longMessage)

        assertTrue(result is GoogleAuthResult.Error)
        assertTrue(result.message.length > 10000)
    }

    @Test
    fun testErrorWithMultilineMessage() {
        val multilineMessage = """
            Error occurred during authentication:
            - Network connection failed
            - Server returned 500
            - Please try again later
        """.trimIndent()

        val result = GoogleAuthResult.Error(multilineMessage)

        assertTrue(result is GoogleAuthResult.Error)
        assertTrue(result.message.contains('\n'))
    }

    @Test
    fun testErrorWithUnicodeCharacters() {
        val unicodeMessage = "認証エラー: ユーザー名が無効です 🔒"
        val result = GoogleAuthResult.Error(unicodeMessage)

        assertTrue(result is GoogleAuthResult.Error)
        assertTrue(result.message.contains("🔒"))
    }

    @Test
    fun testErrorWithNullException() {
        val result = GoogleAuthResult.Error("Failed", null)

        assertTrue(result is GoogleAuthResult.Error)
        assertNull(result.exception)
    }

    @Test
    fun testErrorWithNestedExceptions() {
        val rootCause = Exception("Root cause")
        val middleException = Exception("Middle exception", rootCause)
        val topException = Exception("Top exception", middleException)

        val result = GoogleAuthResult.Error("Nested error", topException)

        assertTrue(result is GoogleAuthResult.Error)
        assertNotNull(result.exception)
        assertNotNull(result.exception?.cause)
        assertNotNull(result.exception?.cause?.cause)
    }

    @Test
    fun testCancelledIsConsistentObject() {
        val cancelled1 = GoogleAuthResult.Cancelled
        val cancelled2 = GoogleAuthResult.Cancelled
        val cancelled3 = GoogleAuthResult.Cancelled

        assertTrue(cancelled1 === cancelled2)
        assertTrue(cancelled2 === cancelled3)
        assertTrue(cancelled1 === cancelled3)
    }

    @Test
    fun testSuccessTokenWithJWTFormat() {
        // Simulate actual JWT token format
        val jwtToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2NTBmZjI5ZmJjZTI4MmY5NDMwM2M1ZGU5MTg3MGM3ZmRhMTk5MDQiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiJ0ZXN0LWNsaWVudC1pZC5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSIsImF1ZCI6InRlc3QtY2xpZW50LWlkLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwic3ViIjoiMTEyMzQ1Njc4OTAxMjM0NTY3ODkwIiwiZW1haWwiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImF0X2hhc2giOiJ0ZXN0LWhhc2giLCJpYXQiOjE2MzY1Nzg5MDAsImV4cCI6MTYzNjU4MjUwMH0.test-signature"
        val result = GoogleAuthResult.Success(jwtToken, "test@example.com")

        assertTrue(result is GoogleAuthResult.Success)
        assertTrue(result.idToken.contains("."))
        assertTrue(result.idToken.startsWith("eyJ"))
    }

    @Test
    fun testComparisonBetweenDifferentResults() {
        val success = GoogleAuthResult.Success("token", "email")
        val error = GoogleAuthResult.Error("error")
        val cancelled = GoogleAuthResult.Cancelled

        // Different types should not be equal
        assertNotEquals(success, error)
        assertNotEquals(error, cancelled)
        assertNotEquals(success, cancelled)
    }

    @Test
    fun testSuccessResultsWithSameValuesAreEqual() {
        val result1 = GoogleAuthResult.Success("token123", "user@example.com")
        val result2 = GoogleAuthResult.Success("token123", "user@example.com")

        assertEquals(result1.idToken, result2.idToken)
        assertEquals(result1.email, result2.email)
    }

    @Test
    fun testErrorResultsWithSameMessageAreEqual() {
        val result1 = GoogleAuthResult.Error("Failed to authenticate")
        val result2 = GoogleAuthResult.Error("Failed to authenticate")

        assertEquals(result1.message, result2.message)
    }

    @Test
    fun testErrorWithDifferentExceptionTypes() {
        val exceptions = listOf(
            RuntimeException("Runtime error"),
            IllegalArgumentException("Invalid argument"),
            IllegalStateException("Invalid state"),
            Exception("Generic exception")
        )

        exceptions.forEach { exception ->
            val result = GoogleAuthResult.Error("Error occurred", exception)
            assertTrue(result is GoogleAuthResult.Error)
            assertNotNull(result.exception)
            assertEquals(exception, result.exception)
        }
    }

    @Test
    fun testWhenExpressionWithAllResultTypes() {
        val testCases = mapOf(
            GoogleAuthResult.Success("t", "e") to "success",
            GoogleAuthResult.Error("err") to "error",
            GoogleAuthResult.Cancelled to "cancelled"
        )

        testCases.forEach { (result, expected) ->
            val actual = when (result) {
                is GoogleAuthResult.Success -> "success"
                is GoogleAuthResult.Error -> "error"
                is GoogleAuthResult.Cancelled -> "cancelled"
            }
            assertEquals(expected, actual)
        }
    }

    @Test
    fun testSuccessWithWhitespaceInEmail() {
        // Note: Real validation should happen elsewhere, but test edge case handling
        val result = GoogleAuthResult.Success("token", " user@example.com ")
        assertTrue(result is GoogleAuthResult.Success)
        assertEquals(" user@example.com ", result.email)
    }

    @Test
    fun testErrorMessageWithSpecialCharacters() {
        val specialMessages = listOf(
            "Error: <script>alert('xss')</script>",
            "Error: \"quoted\" message",
            "Error: \\ backslash",
            "Error: \t tab \n newline",
            "Error: null"
        )

        specialMessages.forEach { message ->
            val result = GoogleAuthResult.Error(message)
            assertTrue(result is GoogleAuthResult.Error)
            assertEquals(message, result.message)
        }
    }

    @Test
    fun testResultTypesInCollection() {
        val results = listOf<GoogleAuthResult>(
            GoogleAuthResult.Success("token1", "email1"),
            GoogleAuthResult.Error("error1"),
            GoogleAuthResult.Cancelled,
            GoogleAuthResult.Success("token2", null),
            GoogleAuthResult.Error("error2", Exception())
        )

        assertEquals(5, results.size)
        assertTrue(results[0] is GoogleAuthResult.Success)
        assertTrue(results[1] is GoogleAuthResult.Error)
        assertTrue(results[2] is GoogleAuthResult.Cancelled)
        assertTrue(results[3] is GoogleAuthResult.Success)
        assertTrue(results[4] is GoogleAuthResult.Error)
    }
}