package com.followme.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CredentialsRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class TokenPairResponse(val accessToken: String, val refreshToken: String)

@Serializable
data class AccessTokenResponse(val accessToken: String)

@Serializable
data class OkResponse(val ok: Boolean)

@Serializable
data class ErrorResponse(val error: String)
