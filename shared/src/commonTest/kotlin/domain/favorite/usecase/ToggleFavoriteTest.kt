package domain.favorite.usecase

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToggleFavoriteTest {

    @Test
    fun `GIVEN a successful toggle WHEN execute is called THEN the new favorited state is returned as a success`() = runTest {
        val repository = FakeFavoriteRepository(toggleResult = true)
        val useCase = ToggleFavorite(repository)

        val result = useCase.execute("42")

        assertEquals(Result.success(true), result)
        assertEquals("42", repository.lastToggledEventId)
    }

    @Test
    fun `GIVEN the repository throws WHEN execute is called THEN the error surfaces as a Result failure not an exception`() = runTest {
        val repository = FakeFavoriteRepository(toggleError = IllegalStateException("network error"))
        val useCase = ToggleFavorite(repository)

        val result = useCase.execute("42")

        assertTrue(result.isFailure)
    }
}
