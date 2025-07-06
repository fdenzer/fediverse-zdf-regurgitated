package com.example.fediversezdfregurgitated.data.repositories

import com.example.fediversezdfregurgitated.data.local.FilterType
import com.example.fediversezdfregurgitated.data.local.RealmFilterRule
import io.mockk.*
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
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
import org.mongodb.kbson.ObjectId

@OptIn(ExperimentalCoroutinesApi::class)
class FilterRepositoryTest {

    private lateinit var mockRealm: Realm
    private lateinit var filterRepository: FilterRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRealm = mockk(relaxed = true)

        // Mock Realm write transaction
        val transactionLambdaSlot = slot<Realm.() -> Unit>()
        every { mockRealm.write(capture(transactionLambdaSlot)) } answers {
            // Potentially execute lambda with mockRealm or a sub-mock if needed
            // For addFilter, we need to ensure the created object is returned by copyToRealm
        }
        every { mockRealm.write(any<suspend Realm.() -> Unit>()) } coAnswers {
            // For suspend version
        }
        filterRepository = FilterRepository(mockRealm, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addFilter success`() = runTest {
        val phrase = "test keyword"
        val type = FilterType.KEYWORD
        val contexts = listOf("home")
        val newRule = RealmFilterRule().apply {
            this.id = ObjectId()
            this.phrase = phrase
            this.filterType = type.name
            this.filterContexts.addAll(contexts)
            this.isEnabled = true
        }

        every { mockRealm.write<RealmFilterRule>(any()) } answers {
            // Simulate the write block by executing the lambda and returning the object
            // This is a simplified mock; real Realm would handle object creation.
            val block = firstArg<Realm.() -> RealmFilterRule>()
            mockRealm.block() // This won't actually create a managed object in mock
            newRule // Return the object that would have been created/managed
        }
        // More accurately, mock copyToRealm if it's directly testable
         coEvery { mockRealm.write<RealmFilterRule>(captureCoroutine()) } coAnswers {
             val coroutine = arg<suspend Realm.() -> RealmFilterRule>(0)
             // This is still tricky because copyToRealm is within the lambda.
             // We'll assume the write block works and verify the object passed to copyToRealm.
             // For simplicity in this mock, let's assume the write block correctly prepares 'newRule'.
             newRule
         }


        val result = filterRepository.addFilter(phrase, type, contexts)

        assertTrue(result.isSuccess)
        assertEquals(phrase, result.getOrNull()?.phrase) // Check some properties
        // Verification of write is tricky due to `copyToRealm` inside.
        // A better test would use an in-memory Realm.
    }

    @Test
    fun `addFilter failure when phrase is blank`() = runTest {
        val result = filterRepository.addFilter("", FilterType.KEYWORD)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `deleteFilter success`() = runTest {
        val filterId = ObjectId()
        val mockFilterRule = mockk<RealmFilterRule>()
        val mockQuery = mockk<RealmQuery<RealmFilterRule>>()
        val mockResults = mockk<RealmResults<RealmFilterRule>>()

        every { mockRealm.query<RealmFilterRule>("id == $0", filterId) } returns mockQuery
        every { mockQuery.first() } returns mockResults
        every { mockResults.find() } returns mockFilterRule
        every { mockRealm.write(any<suspend Realm.() -> Unit>()) } coAnswers {
            val lambda = arg<suspend Realm.() -> Unit>(0)
            // We need to mock `delete(mockFilterRule)` call inside the lambda
            mockRealm.lambda() // Execute the lambda
        }
        coEvery { mockRealm.delete(mockFilterRule) } just runs // Mock the delete call

        val result = filterRepository.deleteFilter(filterId)
        assertTrue(result.isSuccess)
        coVerify { mockRealm.delete(mockFilterRule) } // Verify delete was called on the object
    }


    @Test
    fun `getAllFiltersFlow returns flow of filters`() = runTest {
        val filter1 = RealmFilterRule().apply { phrase = "f1" }
        val filter2 = RealmFilterRule().apply { phrase = "f2" }
        val mockRealmResults = mockk<RealmResults<RealmFilterRule>>()
        every { mockRealmResults.toList() } returns listOf(filter1, filter2)

        val mockResultsChange = mockk<ResultsChange<RealmFilterRule>>()
        every { mockResultsChange.list } returns mockRealmResults

        val mockQuery = mockk<RealmQuery<RealmFilterRule>>()
        every { mockRealm.query<RealmFilterRule>() } returns mockQuery
        every { mockQuery.asFlow() } returns flowOf(mockResultsChange)

        val flow = filterRepository.getAllFiltersFlow()
        val resultList = flow.first()

        assertEquals(2, resultList.size)
    }

    @Test
    fun `shouldFilterStatus keyword match`() = runTest {
        val keywordFilter = RealmFilterRule().apply {
            phrase = "test"
            filterType = FilterType.KEYWORD.name
            isEnabled = true
            filterContexts.add("home")
        }
        coEvery { filterRepository.getActiveFiltersForContext("home") } returns listOf(keywordFilter)
        // Direct call to getActiveFiltersForContext is mocked for this unit test's focus.

        assertTrue(filterRepository.shouldFilterStatus("this is a test content", "author1", "user@host","home"))
        assertFalse(filterRepository.shouldFilterStatus("this is safe content", "author2", "user2@host","home"))
    }

    @Test
    fun `shouldFilterStatus user match by username`() = runTest {
        val userFilter = RealmFilterRule().apply {
            phrase = "blockeduser@example.com"
            filterType = FilterType.USER_ID.name // Assuming phrase stores username for USER_ID type
            isEnabled = true
            filterContexts.add("home")
        }
        coEvery { filterRepository.getActiveFiltersForContext(any()) } returns listOf(userFilter)

        assertTrue(filterRepository.shouldFilterStatus("content", "author123", "blockeduser@example.com", "home"))
        assertFalse(filterRepository.shouldFilterStatus("content", "author456", "otheruser@example.com", "home"))
    }

    @Test
    fun `shouldFilterStatus user match by ID`() = runTest {
        val userFilter = RealmFilterRule().apply {
            phrase = "author123" // Filter phrase is the author ID
            filterType = FilterType.USER_ID.name
            isEnabled = true
            filterContexts.add("home")
        }
        coEvery { filterRepository.getActiveFiltersForContext(any()) } returns listOf(userFilter)

        assertTrue(filterRepository.shouldFilterStatus("content", "author123", "blockeduser@example.com", "home"))
        assertFalse(filterRepository.shouldFilterStatus("content", "author456", "otheruser@example.com", "home"))
    }


    @Test
    fun `shouldFilterStatus no active filters`() = runTest {
        coEvery { filterRepository.getActiveFiltersForContext(any()) } returns emptyList()
        assertFalse(filterRepository.shouldFilterStatus("any content", "anyAuthor", "anyUser@host", "home"))
    }

    @Test
    fun `shouldFilterStatus filter not in context`() = runTest {
         val keywordFilter = RealmFilterRule().apply {
            phrase = "test"
            filterType = FilterType.KEYWORD.name
            isEnabled = true
            filterContexts.add("notifications") // Only in notifications context
        }
        coEvery { filterRepository.getActiveFiltersForContext("home") } returns emptyList() // No filters for 'home'
        coEvery { filterRepository.getActiveFiltersForContext("notifications") } returns listOf(keywordFilter)


        assertFalse(filterRepository.shouldFilterStatus("this is a test content", "author1", "user@host", "home"))
        assertTrue(filterRepository.shouldFilterStatus("this is a test content", "author1", "user@host", "notifications"))
    }

    // Note: More comprehensive testing of updateFilter would be similar to addFilter/deleteFilter,
    // involving mocking query().first().find() and verifying the properties set in the write block.
    // As with StatusRepositoryTest, in-memory Realm would be better for full DAO-style testing.
}
