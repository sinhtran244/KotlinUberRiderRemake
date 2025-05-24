package com.example.kotlinuberriderremake.Remote

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface IGoogleAPI {
    //IMPORTANT : PLEASE ENABLE BILLING FOR YOUR GOOGLE PROJECT TO USE THIS API
    @GET(value = "maps/api/directions/json")
    fun getDirections(
        @Query(value = "mode") mode: String?,
        @Query(value = "transit_routing_preference") transit_routing: String?,
        @Query(value = "origin") from: String?,
        @Query(value = "destination") to: String?,
        @Query(value = "key") key: String
    ): Observable<String>?
}