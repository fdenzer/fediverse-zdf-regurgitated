package com.example.fediversezdfregurgitated.data.remote

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import social.bigbone.MastodonClient
import social.bigbone.api.Pageable
import social.bigbone.api.Range
import social.bigbone.api.entity.MastodonToken
import social.bigbone.api.entity.Status
import social.bigbone.api.exception.BigBoneRequestException
import social.bigbone.api.method.TimelineMethods
import social.bigbone.api.method.OAuthMethods
import social.bigbone.api.method.StatusMethods

class MastodonApiClientTest {

    private lateinit var mockMastodonClient: MastodonClient
    private lateinit var mockTimelineMethods: TimelineMethods
    private lateinit var mockOAuthMethods: OAuthMethods
    private lateinit var mockStatusMethods: StatusMethods

    private lateinit var apiClient: MastodonApiClient

    // Keep track of the original AuthConstants if we modify them for specific tests
    private val originalClientId = AuthConstants.CLIENT_ID_PLACEHOLDER
    private val originalClientSecret = AuthConstants.CLIENT_SECRET_PLACEHOLDER
    private val originalRedirectUri = AuthConstants.REDIRECT_URI_PLACEHOLDER
    private val originalScopes = AuthConstants.SCOPES


    @Before
    fun setUp() {
        mockMastodonClient = mockk(relaxed = true) // relaxed for easier mocking of chained calls
        mockTimelineMethods = mockk()
        mockOAuthMethods = mockk()
        mockStatusMethods = mockk()


        every { mockMastodonClient.timelines } returns mockTimelineMethods
        every { mockMastodonClient.oauth } returns mockOAuthMethods
        every { mockMastodonClient.statuses } returns mockStatusMethods

        // Mock the MastodonClient.Builder chain
        // This is a bit complex due to BigBone's builder pattern.
        // We need to ensure that when MastodonApiClient creates its internal client,
        // it actually uses our mocks. This is tricky without DI for the client itself.
        // For this test, we will assume the internal client gets built, and then test methods.
        // A better approach would be to inject MastodonClient into MastodonApiClient.
        // For now, we'll test the logic within MastodonApiClient assuming 'client' field is correctly set.

        apiClient = MastodonApiClient(null) // Start with no token

        // We need to control the client instance used by MastodonApiClient.
        // This is a workaround since we can't directly inject the mocked client easily
        // into the current MastodonApiClient structure without refactoring it for DI.
        // So, we'll reinitialize apiClient.client with our mock for testing specific methods.
        // This is not ideal but allows testing existing code.
        apiClient.setAccessToken(null) // ensure client is rebuilt
        val slot = slot<String>()
        // Replace the internal client with our mock for testing purposes
        // This is a hacky way; proper DI would be much cleaner.
        // We'd typically inject the MastodonClient.
        // For now, we'll use a helper or reflection if needed, or test as black-box.

        // Let's refine the test structure to work with the current MastodonApiClient
        // by re-assigning its internal client if possible, or by mocking the builder.
        // Since MastodonApiClient internally creates its client, we'll focus on testing the logic
        // given that a client (real or mocked for specific calls) is used.

        // For methods that use a specific client (e.g. oauth.issueAccessToken),
        // we might need to mock the Builder if it's used internally for those.
        // The current implementation of exchangeCodeForToken in MastodonApiClient creates its own client.
        // We will mock the Builder for that specific case.

        mockkConstructor(MastodonClient.Builder::class)
        every { anyConstructed<MastodonClient.Builder>().build() } returns mockMastodonClient
        every { anyConstructed<MastodonClient.Builder>().accessToken(capture(slot())) } answers { self as MastodonClient.Builder }


        apiClient = MastodonApiClient(null) // Re-initialize to use mocked builder
    }
    @After
    fun tearDown() {
        unmockkAll() // Important to clean up mocks, especially constructor mocks
    }


    @Test
    fun `getLocalTimeline success`() = runBlocking {
        val mockStatuses = Pageable(part = listOf(mockk<Status>()), range = Range())
        coEvery { mockTimelineMethods.getPublicTimeline(any(), local = true) } returns mockk {
            coEvery { execute() } returns mockStatuses
        }

        val result = apiClient.getLocalTimeline()

        assertTrue(result.isSuccess)
        assertEquals(mockStatuses, result.getOrNull())
    }

    @Test
    fun `getLocalTimeline failure`() = runBlocking {
        val exception = BigBoneRequestException("Network error")
        coEvery { mockTimelineMethods.getPublicTimeline(any(), local = true) } throws exception

        val result = apiClient.getLocalTimeline()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `exchangeCodeForToken success`() = runBlocking {
        val mockToken = mockk<MastodonToken>()
        val testCode = "test_code"
        every { mockToken.accessToken } returns "test_access_token"

        coEvery {
            mockOAuthMethods.issueAccessToken(
                AuthConstants.CLIENT_ID_PLACEHOLDER,
                AuthConstants.CLIENT_SECRET_PLACEHOLDER,
                AuthConstants.REDIRECT_URI_PLACEHOLDER,
                "authorization_code",
                testCode,
                AuthConstants.SCOPES
            )
        } returns mockk {
            coEvery { execute() } returns mockToken
        }

        val result = apiClient.exchangeCodeForToken(testCode)

        assertTrue(result.isSuccess)
        assertEquals(mockToken, result.getOrNull())
        assertEquals("test_access_token", apiClient.accessToken) // Verify internal token is set
        assertTrue(apiClient.hasAccessToken())
    }
    @Test
    fun `exchangeCodeForToken failure`() = runBlocking {
        val testCode = "test_code"
        val exception = BigBoneRequestException("OAuth error")

        coEvery {
            mockOAuthMethods.issueAccessToken(any(), any(), any(), any(), eq(testCode), any())
        } throws exception


        val result = apiClient.exchangeCodeForToken(testCode)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        assertFalse(apiClient.hasAccessToken())
    }


    @Test
    fun `getHomeTimeline success when authenticated`() = runBlocking {
        apiClient.setAccessToken("fake_token") // Authenticate the client
        val mockStatuses = Pageable(part = listOf(mockk<Status>()), range = Range())

        coEvery { mockTimelineMethods.getHomeTimeline(any()) } returns mockk {
            coEvery { execute() } returns mockStatuses
        }

        val result = apiClient.getHomeTimeline()
        assertTrue(result.isSuccess)
        assertEquals(mockStatuses, result.getOrNull())
    }

    @Test
    fun `getHomeTimeline failure when not authenticated`() = runBlocking {
        apiClient.setAccessToken(null) // Ensure not authenticated
        val result = apiClient.getHomeTimeline()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }


    @Test
    fun `postStatus success when authenticated`() = runBlocking {
        apiClient.setAccessToken("fake_token")
        val mockStatus = mockk<Status>()
        val statusText = "Hello World"

        coEvery { mockStatusMethods.postStatus(status = statusText, visibility = Status.Visibility.PUBLIC, mediaIds = null, pollOptions = null, pollExpiresIn = null, pollMultiple = null, inReplyToId = null, spoilerText = null) } returns mockk {
            coEvery { execute() } returns mockStatus
        }

        val result = apiClient.postStatus(statusText)
        assertTrue(result.isSuccess)
        assertEquals(mockStatus, result.getOrNull())
    }

    @Test
    fun `postStatus failure when not authenticated`() = runBlocking {
        apiClient.setAccessToken(null)
        val result = apiClient.postStatus("Test")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `favouriteStatus success when authenticated`() = runBlocking {
        apiClient.setAccessToken("fake_token")
        val mockStatus = mockk<Status>()
        val statusId = "123"

        coEvery { mockStatusMethods.favouriteStatus(statusId) } returns mockk {
            coEvery { execute() } returns mockStatus
        }
        val result = apiClient.favouriteStatus(statusId)
        assertTrue(result.isSuccess)
    }
}
