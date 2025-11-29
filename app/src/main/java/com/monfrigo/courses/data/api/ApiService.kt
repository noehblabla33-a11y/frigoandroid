package com.monfrigo.courses.data.api

import com.monfrigo.courses.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface Retrofit pour les appels API
 */
interface ApiService {

    @GET("health")
    suspend fun checkHealth(): Response<Map<String, String>>

    @GET("courses")
    suspend fun getCourses(): Response<CoursesResponse>

    @POST("courses/sync")
    suspend fun syncCourses(@Body request: SyncRequest): Response<SyncResponse>

    @DELETE("courses/{id}")
    suspend fun deleteItem(@Path("id") itemId: Int): Response<Map<String, Any>>
}