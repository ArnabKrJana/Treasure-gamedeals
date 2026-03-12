package com.example.treasure.data.remote.networkDto.steam

import com.google.gson.annotations.SerializedName

// Wrapper for the specific app ID object
data class SteamAppResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: SteamGameDataDto
)

data class SteamGameDataDto(
    @SerializedName("name") val name: String,
    @SerializedName("detailed_description") val descriptionHtml: String,
    @SerializedName("short_description") val shortDescription: String,

    @SerializedName("required_age") val requiredAge: Int?,
    @SerializedName("developers") val developers: List<String>?,
    @SerializedName("publishers") val publishers: List<String>?,

    // --- MEDIA ---
    @SerializedName("header_image") val headerImage: String,
    @SerializedName("screenshots") val screenshots: List<SteamScreenshotDto>?,
    @SerializedName("movies") val movies: List<SteamMovieDto>?,

    // --- REQUIREMENTS ---
    @SerializedName("pc_requirements") val pcRequirements: SteamRequirementsDto?,
    @SerializedName("mac_requirements") val macRequirements: SteamRequirementsDto?,
    @SerializedName("linux_requirements") val linuxRequirements: SteamRequirementsDto?,

    // --- METADATA ---
    @SerializedName("release_date") val releaseDate: SteamReleaseDateDto?,
    @SerializedName("platforms") val platforms: SteamPlatformsDto?,
    @SerializedName("genres") val genres: List<SteamGenreDto>?
)

data class SteamRequirementsDto(
    @SerializedName("minimum") val minimum: String?,
    @SerializedName("recommended") val recommended: String?
)

data class SteamScreenshotDto(
    @SerializedName("path_thumbnail") val thumbnail: String,
    @SerializedName("path_full") val full: String
)

data class SteamMovieDto(
    @SerializedName("name") val name: String,
    @SerializedName("highlight") val highlight: Boolean,

    // Video Formats
    @SerializedName("mp4") val mp4: SteamMovieSourceDto?,
    @SerializedName("webm") val webm: SteamMovieSourceDto?,

    // Streaming Formats (Often used by newer games/Steam Deck)
    @SerializedName("hls_h264") val hlsUrl: String?,  // .m3u8 file
    @SerializedName("dash_h264") val dashUrl: String? // .mpd file
)

data class SteamMovieSourceDto(
    @SerializedName("480") val sd: String?,
    @SerializedName("max") val hd: String?
)

data class SteamReleaseDateDto(
    @SerializedName("date") val date: String
)

data class SteamPlatformsDto(
    @SerializedName("windows") val windows: Boolean,
    @SerializedName("mac") val mac: Boolean,
    @SerializedName("linux") val linux: Boolean
)

data class SteamGenreDto(
    @SerializedName("description") val description: String
)