package com.example.fediversezdfregurgitated.data.repositories

import com.example.fediversezdfregurgitated.data.local.RealmStatus
import com.example.fediversezdfregurgitated.data.remote.MastodonApiClient
import io.mockk.*
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.query.RealmResults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import social.bigbone.api.Pageable
import social.bigbone.api.Range
import social.bigbone.api.entity.Account
import social.bigbone.api.entity.Status
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class StatusRepositoryTest {

    private lateinit var mockApiClient: MastodonApiClient
    private lateinit var mockRealm: Realm
    private lateinit var mockFilterRepository: FilterRepository
    private lateinit var statusRepository: StatusRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher) // For runTest

        mockApiClient = mockk()
        mockRealm = mockk(relaxed = true) // relaxed for write and query blocks
        mockFilterRepository = mockk()

        // Mock Realm write transaction
        val transactionLambdaSlot = slot<Realm.() -> Unit>()
        every { mockRealm.write(capture(transactionLambdaSlot)) } answers {
            // Execute the lambda with the mockRealm instance if needed, or just capture
            // For simplicity, we might not need to actually execute it if we verify calls within it
        }
        every { mockRealm.write(any<suspend Realm.() -> Unit>()) } coAnswers {
            // For suspend version
        }


        statusRepository = StatusRepository(mockApiClient, mockRealm, mockFilterRepository, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createMockApiStatus(
        id: String,
        content: String = "Test content",
        accountId: String = "acc1",
        accountAcct: String = "user@instance.com",
        accountUrl: String = "https://instance.com/@user",
        createdAt: String = "2023-01-01T12:00:00.000Z", // ISO_DATE_TIME
        isFavourited: Boolean = false,
        isReblogged: Boolean = false,
        isBookmarked: Boolean = false,
        repliesCount: Int = 0,
        reblogsCount: Int = 0,
        favouritesCount: Int = 0,
        sensitive: Boolean = false,
        spoilerText: String = ""
    ): Status {
        val mockAccount = mockk<Account>()
        every { mockAccount.id } returns accountId
        every { mockAccount.acct } returns accountAcct
        every { mockAccount.url } returns accountUrl
        every { mockAccount.avatarStatic } returns "http://example.com/avatar.png"
        every { mockAccount.displayName } returns "Test User"


        return Status(
            id = id,
            uri = "uri",
            url = "url",
            account = mockAccount,
            inReplyToId = null,
            inReplyToAccountId = null,
            reblog = null,
            content = content,
            createdAt = createdAt,
            emojis = emptyList(),
            repliesCount = repliesCount,
            reblogsCount = reblogsCount,
            favouritesCount = favouritesCount,
            isReblogged = isReblogged,
            isFavourited = isFavourited,
            isMuted = false,
            isSensitive = sensitive,
            spoilerText = spoilerText,
            visibility = Status.Visibility.PUBLIC,
            mediaAttachments = emptyList(),
            mentions = emptyList(),
            tags = emptyList(),
            application = null,
            language = "en",
            isPinned = false,
            isEdited = false,
            isBookmarked = isBookmarked,
            card = null,
            poll = null,
            text = null // BigBone sets this if content has HTML
        )
    }


    @Test
    fun `apiStatusToRealmStatus converts correctly`() {
        val isoDateTime = "2023-01-15T10:30:00.000Z"
        val apiStatus = createMockApiStatus(
            id = "123",
            content = "<p>Hello</p>",
            createdAt = isoDateTime,
            isFavourited = true,
            reblogsCount = 5
        )
        val expectedTimestamp = ZonedDateTime.parse(isoDateTime, DateTimeFormatter.ISO_DATE_TIME)
            .toInstant().toEpochMilli()

        val realmStatus = statusRepository.apiStatusToRealmStatus(apiStatus, "home")

        assertEquals("123", realmStatus.id)
        assertEquals("<p>Hello</p>", realmStatus.content)
        assertEquals(expectedTimestamp, realmStatus.createdAt)
        assertEquals("home", realmStatus.timelineType)
        assertTrue(realmStatus.favourited)
        assertEquals(5, realmStatus.boostCount)
        assertEquals("instance.com", realmStatus.instanceUrl) // from account.url
    }


    @Test
    fun `fetchAndCacheTimeline success - fetches, converts, and writes to Realm`() = runTest {
        val apiStatus1 = createMockApiStatus(id = "s1")
        val apiStatus2 = createMockApiStatus(id = "s2")
        val pageableStatuses = Pageable(part = listOf(apiStatus1, apiStatus2), range = Range())

        coEvery { mockApiClient.getLocalTimeline(any()) } returns Result.success(pageableStatuses)

        val result = statusRepository.fetchAndCacheLocalTimeline()

        assertTrue(result.isSuccess)
        coVerify { mockApiClient.getLocalTimeline(any()) } // Check API was called

        // Verify that realm.write was called and it tried to copy RealmStatus objects
        // We need to capture the lambda passed to realm.write
        val transactionLambda = slot<suspend Realm.() -> Unit>()
        coVerify { mockRealm.write(capture(transactionLambda)) }

        // To verify the objects inside, we'd need more sophisticated mocking of copyToRealm
        // or use a real in-memory Realm for testing this part.
        // For now, verifying the call to write is a good step.
        // We can also check the number of times copyToRealm was called if we mock it.
        // e.g. verify(exactly = 2) { mockRealm.copyToRealm(any<RealmStatus>(), UpdatePolicy.ALL) }
        // This requires mockRealm to not be relaxed or to specifically mock copyToRealm.
    }

    @Test
    fun `fetchAndCacheTimeline failure - API error`() = runTest {
        val exception = BigBoneRequestException("API error")
        coEvery { mockApiClient.getLocalTimeline(any()) } returns Result.failure(exception)

        val result = statusRepository.fetchAndCacheLocalTimeline()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 0) { mockRealm.write(any<suspend Realm.() -> Unit>()) } // Ensure no write on API failure
    }

    @Test
    fun `getTimelineFlow returns filtered data from Realm`() = runTest {
        val status1 = RealmStatus().apply { id = "1"; content = "Status 1"; timelineType = "home" }
        val status2 = RealmStatus().apply { id = "2"; content = "Status 2 with keyword"; timelineType = "home" }
        val status3 = RealmStatus().apply { id = "3"; content = "Status 3"; timelineType = "local" } // Different type

        val mockRealmResults = mockk<RealmResults<RealmStatus>>()
        every { mockRealmResults.toList() } returns listOf(status1, status2) // Simulating already filtered by type by query

        val mockResultsChange = mockk<ResultsChange<RealmStatus>>()
        every { mockResultsChange.list } returns mockRealmResults

        val mockQuery = mockk<RealmQuery<RealmStatus>>()
        every { mockRealm.query<RealmStatus>(any<String>(), "home") } returns mockQuery
        every { mockQuery.asFlow() } returns flowOf(mockResultsChange)

        // Mock filter repository to return no active filters initially
        coEvery { mockFilterRepository.getAllFiltersFlow() } returns flowOf(emptyList())


        val flow = statusRepository.getTimelineFlow("home")
        val resultList = flow.first() // Collect first emission

        assertEquals(2, resultList.size)
        assertTrue(resultList.any { it.id == "1" })
        assertTrue(resultList.any { it.id == "2" })

        // TODO: Add tests for when filters are active and should remove items
    }


    // Note: Testing Realm flows with filtering logic can become complex with mocks.
    // Using an in-memory Realm instance via a test rule or specific test configuration
    // for Android instrumented tests or Robolectric tests is often more robust for these.
    // For pure unit tests on JVM, mocking Realm interactions like above is one approach,
    // but has limitations in fully testing query and flow behaviors.
}
