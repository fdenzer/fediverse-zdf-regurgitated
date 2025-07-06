package com.example.fediversezdfregurgitated.data.remote

import social.bigbone.MastodonClient
import social.bigbone.api.Pageable
import social.bigbone.api.Range
import social.bigbone.api.entity.Status
import social.bigbone.api.exception.BigBoneRequestException

import social.bigbone.MastodonClient
import social.bigbone.api.Pageable
import social.bigbone.api.Range
import social.bigbone.api.entity.MastodonToken
import social.bigbone.api.entity.Status
import social.bigbone.api.exception.BigBoneRequestException

class MastodonApiClient(private var accessToken: String? = null) {

    // Configure the client to point to zdf.social
    private val clientBuilder = MastodonClient.Builder(AuthConstants.ZDF_SOCIAL_INSTANCE_URL)
    private var client: MastodonClient

    init {
        if (accessToken != null) {
            clientBuilder.accessToken(accessToken!!)
        }
        client = clientBuilder.build()
    }

    fun setAccessToken(token: String?) {
        this.accessToken = token
        if (token != null) {
            client = MastodonClient.Builder(AuthConstants.ZDF_SOCIAL_INSTANCE_URL)
                .accessToken(token)
                .build()
        } else {
            // Build client without access token if token is null (for public endpoints)
            client = MastodonClient.Builder(AuthConstants.ZDF_SOCIAL_INSTANCE_URL)
                .build()
        }
    }

    fun hasAccessToken(): Boolean = accessToken != null

    /**
     * Exchanges an authorization code for an access token.
     */
    suspend fun exchangeCodeForToken(code: String): Result<MastodonToken> {
        return try {
            val token = client.oauth.issueAccessToken(
                clientId = AuthConstants.CLIENT_ID_PLACEHOLDER,
                clientSecret = AuthConstants.CLIENT_SECRET_PLACEHOLDER,
                redirectUri = AuthConstants.REDIRECT_URI_PLACEHOLDER,
                grantType = "authorization_code",
                code = code,
                scopes = AuthConstants.SCOPES
            ).execute()
            // Store this token securely and update the client instance
            this.setAccessToken(token.accessToken)
            Result.success(token)
        } catch (e: BigBoneRequestException) {
            Result.failure(e)
        }
    }


    /**
     * Fetches the home timeline for the authenticated user.
     * Requires authentication.
     */
    suspend fun getHomeTimeline(range: Range = Range()): Result<Pageable<Status>> {
        // if (accessToken == null) return Result.failure(IllegalStateException("Access token not set."))
        return try {
            // This call requires an access token, which we don't have yet in this step.
            // For now, this will fail if called directly without auth.
            // We will integrate OAuth in a later step.
            // val statuses = client.timelines.getHomeTimeline(range).execute()
            // Result.success(statuses)
            Result.failure(NotImplementedError("OAuth not implemented yet. Cannot fetch home timeline."))
        } catch (e: BigBoneRequestException) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the local timeline for zdf.social.
     * Does not require authentication.
     */
    suspend fun getLocalTimeline(range: Range = Range()): Result<Pageable<Status>> {
        return try {
            val statuses = client.timelines.getPublicTimeline(range, local = true).execute()
            Result.success(statuses)
        } catch (e: BigBoneRequestException) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the federated timeline.
     * Does not require authentication.
     */
    suspend fun getFederatedTimeline(range: Range = Range()): Result<Pageable<Status>> {
        return try {
            val statuses = client.timelines.getPublicTimeline(range, local = false).execute()
            Result.success(statuses)
        } catch (e: BigBoneRequestException) {
            Result.failure(e)
        }
    }

    // --- Posting and Interactions ---

    suspend fun postStatus(
        statusText: String,
        visibility: Status.Visibility = Status.Visibility.PUBLIC,
        mediaIds: List<String>? = null,
        pollOptions: List<String>? = null,
        pollExpiresInSeconds: Int? = null,
        pollMultipleChoice: Boolean? = null,
        inReplyToId: String? = null,
        spoilerText: String? = null
        // TODO: Add other parameters like language, scheduledAt, etc. if needed
    ): Result<Status> {
        if (!hasAccessToken()) return Result.failure(IllegalStateException("Access token not set. Please login."))
        return try {
            val status = client.statuses.postStatus(
                status = statusText,
                visibility = visibility,
                mediaIds = mediaIds,
                pollOptions = pollOptions,
                pollExpiresIn = pollExpiresInSeconds,
                pollMultiple = pollMultipleChoice,
                inReplyToId = inReplyToId,
                spoilerText = spoilerText
            ).execute()
            Result.success(status)
        } catch (e: BigBoneRequestException) {
            Result.failure(e)
        }
    }

    suspend fun favouriteStatus(statusId: String): Result<Status> {
        if (!hasAccessToken()) return Result.failure(IllegalStateException("Access token not set. Please login."))
        return try {
            val status = client.statuses.favouriteStatus(statusId).execute()
            Result.success(status)
        } catch (e: BigBoneRequestException) {
            Result.failure(e)
        }
    }

    suspend fun unfavouriteStatus(statusId: String): Result<Status> {
        if (!hasAccessToken()) return Result.failure(IllegalStateException("Access token not set. Please login."))
        return try {
            val status = client.statuses.unfavouriteStatus(statusId).execute()
            Result.success(status)
        } catch (e: BigBoneRequestException) {
            Result.failure(e)
        }
    }

    suspend fun boostStatus(statusId: String): Result<Status> {
        if (!hasAccessToken()) return Result.failure(IllegalStateException("Access token not set. Please login."))
        return try {
            val status = client.statuses.boostStatus(statusId).execute()
            Result.success(status)
        } catch (e: BigBoneRequestException) {
            Result.failure(e)
        }
    }

    suspend fun unboostStatus(statusId: String): Result<Status> {
        if (!hasAccessToken()) return Result.failure(IllegalStateException("Access token not set. Please login."))
        return try {
            val status = client.statuses.unboostStatus(statusId).execute()
            Result.success(status)
        } catch (e: BigBoneRequestException) {
            Result.failure(e)
        }
    }

    suspend fun bookmarkStatus(statusId: String): Result<Status> {
        if (!hasAccessToken()) return Result.failure(IllegalStateException("Access token not set. Please login."))
        return try {
            val status = client.statuses.bookmarkStatus(statusId).execute()
            Result.success(status)
        } catch (e: BigBoneRequestException) {
            Result.failure(e)
        }
    }

    suspend fun unbookmarkStatus(statusId: String): Result<Status> {
        if (!hasAccessToken()) return Result.failure(IllegalStateException("Access token not set. Please login."))
        return try {
            val status = client.statuses.unbookmarkStatus(statusId).execute()
            Result.success(status)
        } catch (e: BigBoneRequestException) {
            Result.failure(e)
        }
    }

    // TODO: Implement media upload (client.media.uploadMedia(...))
    // TODO: Implement notifications (client.notifications.getAllNotifications(...))
    // TODO: Implement streaming (client.streaming.user(...)) for real-time updates
}
