package id.andriawan.cofinance.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoogleAuthResultTest {

    @Test
    fun testSuccessResult() {
        val idToken = "test_id_token_12345"
        val email = "user@example.com"
        val result = GoogleAuthResult.Success(idToken, email)

        assertTrue(result is GoogleAuthResult.Success)
        assertEquals(idToken, result.idToken)
        assertEquals(email, result.email)
    }

    @Test
    fun testSuccessResultWithNullEmail() {
        val idToken = "test_id_token_12345"
        val result = GoogleAuthResult.Success(idToken, null)

        assertTrue(result is GoogleAuthResult.Success)
        assertEquals(idToken, result.idToken)
        assertNull(result.email)
    }

    @Test
    fun testErrorResult() {
        val message = "Authentication failed"
        val exception = Exception("Network error")
        val result = GoogleAuthResult.Error(message, exception)

        assertTrue(result is GoogleAuthResult.Error)
        assertEquals(message, result.message)
        assertNotNull(result.exception)
        assertEquals("Network error", result.exception?.message)
    }

    @Test
    fun testErrorResultWithoutException() {
        val message = "Authentication failed"
        val result = GoogleAuthResult.Error(message)

        assertTrue(result is GoogleAuthResult.Error)
        assertEquals(message, result.message)
        assertNull(result.exception)
    }

    @Test
    fun testCancelledResult() {
        val result = GoogleAuthResult.Cancelled

        assertTrue(result is GoogleAuthResult.Cancelled)
        assertEquals(GoogleAuthResult.Cancelled, result)
    }

    @Test
    fun testMultipleSuccessResultsAreIndependent() {
        val result1 = GoogleAuthResult.Success("token1", "user1@example.com")
        val result2 = GoogleAuthResult.Success("token2", "user2@example.com")

        assertTrue(result1 is GoogleAuthResult.Success)
        assertTrue(result2 is GoogleAuthResult.Success)
        assertEquals("token1", result1.idToken)
        assertEquals("token2", result2.idToken)
        assertEquals("user1@example.com", result1.email)
        assertEquals("user2@example.com", result2.email)
    }

    @Test
    fun testErrorMessageVariations() {
        val emptyMessage = GoogleAuthResult.Error("")
        val longMessage = GoogleAuthResult.Error("This is a very long error message that describes what went wrong in detail")
        val specialCharsMessage = GoogleAuthResult.Error("Error: 401 - Unauthorized! @#$%")

        assertTrue(emptyMessage is GoogleAuthResult.Error)
        assertTrue(longMessage is GoogleAuthResult.Error)
        assertTrue(specialCharsMessage is GoogleAuthResult.Error)
        assertEquals("", emptyMessage.message)
        assertTrue(longMessage.message.length > 50)
        assertTrue(specialCharsMessage.message.contains("401"))
    }

    @Test
    fun testSealedClassExhaustiveCheck() {
        val results = listOf(
            GoogleAuthResult.Success("token", "email@example.com"),
            GoogleAuthResult.Error("error"),
            GoogleAuthResult.Cancelled
        )

        results.forEach { result ->
            when (result) {
                is GoogleAuthResult.Success -> assertTrue(result.idToken.isNotEmpty())
                is GoogleAuthResult.Error -> assertTrue(result.message.isNotEmpty())
                is GoogleAuthResult.Cancelled -> assertTrue(true) // Just verify it matches
            }
        }
    }
}