package domain.user.usecase

import domain.enum.ConsentType
import domain.user.DataRightResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ExerciseDataRightTest {

    @Test
    fun `GIVEN access is requested WHEN access is called THEN the repository's summary passes through`() = runTest {
        val repository = FakeUserRepository(dataRightResult = DataRightResult.AccessGranted("resumo dos dados"))
        val useCase = ExerciseDataRight(repository)

        val result = useCase.access()

        assertEquals(DataRightResult.AccessGranted("resumo dos dados"), result)
    }

    @Test
    fun `GIVEN export is requested WHEN export is called THEN the repository's payload passes through`() = runTest {
        val repository = FakeUserRepository(dataRightResult = DataRightResult.ExportReady("payload.json"))
        val useCase = ExerciseDataRight(repository)

        val result = useCase.export()

        assertEquals(DataRightResult.ExportReady("payload.json"), result)
    }

    @Test
    fun `GIVEN deletion is requested WHEN delete is called THEN the repository's confirmation passes through`() = runTest {
        val repository = FakeUserRepository(dataRightResult = DataRightResult.DeletionConfirmed)
        val useCase = ExerciseDataRight(repository)

        val result = useCase.delete()

        assertEquals(DataRightResult.DeletionConfirmed, result)
    }

    @Test
    fun `GIVEN a consent type WHEN revokeConsent is called THEN it passes through unchanged to the repository`() = runTest {
        val repository = FakeUserRepository(dataRightResult = DataRightResult.ConsentRevoked)
        val useCase = ExerciseDataRight(repository)

        val result = useCase.revokeConsent(ConsentType.Location)

        assertEquals(ConsentType.Location, repository.revokedConsentType)
        assertEquals(DataRightResult.ConsentRevoked, result)
    }
}
