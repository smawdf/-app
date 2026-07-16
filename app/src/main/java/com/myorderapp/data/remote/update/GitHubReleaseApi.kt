package com.myorderapp.data.remote.update

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Headers

@JsonClass(generateAdapter = true)
data class GitHubReleaseResponse(
    @Json(name = "tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubReleaseAsset> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseAsset(
    val name: String = "",
    @Json(name = "browser_download_url") val downloadUrl: String = ""
)

interface GitHubReleaseApi {
    @Headers(
        "Accept: application/vnd.github+json",
        "X-GitHub-Api-Version: 2022-11-28",
        "User-Agent: GaoTangXiaoShi-Android"
    )
    @GET("repos/smawdf/-app/releases/latest")
    suspend fun latestRelease(): GitHubReleaseResponse
}
