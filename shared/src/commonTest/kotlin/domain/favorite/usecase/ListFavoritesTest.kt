package domain.favorite.usecase

import domain.enum.City
import domain.event.Event
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun sampleEvent(id: String) = Event(
    id = id,
    title = "Show",
    description = "desc",
    coverImageUrl = null,
    startsAt = "2026-10-01T22:00:00Z",
    city = City.Vitoria,
    genre = "Rock",
    address = "Rua X",
    isFree = true,
    ticketUrl = null,
)

class ListFavoritesTest {

    @Test
    fun `GIVEN favorited events WHEN execute is called THEN they are returned as a success`() = runTest {
        val repository = FakeFavoriteRepository(listResult = listOf(sampleEvent("e1"), sampleEvent("e2")))
        val useCase = ListFavorites(repository)

        val result = useCase.execute()

        assertEquals(Result.success(listOf(sampleEvent("e1"), sampleEvent("e2"))), result)
    }

    @Test
    fun `GIVEN no favorited events WHEN execute is called THEN an empty success list is returned not an error`() = runTest {
        val repository = FakeFavoriteRepository(listResult = emptyList())
        val useCase = ListFavorites(repository)

        val result = useCase.execute()

        assertEquals(Result.success(emptyList()), result)
    }

    @Test
    fun `GIVEN the repository throws WHEN execute is called THEN the error surfaces as a Result failure not an exception`() = runTest {
        val repository = FakeFavoriteRepository(listError = IllegalStateException("network error"))
        val useCase = ListFavorites(repository)

        val result = useCase.execute()

        assertTrue(result.isFailure)
    }
}
