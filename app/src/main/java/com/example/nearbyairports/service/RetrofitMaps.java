package com.example.nearbyairports.service;

import com.example.nearbyairports.model.findplace.PlacesResponse;
import com.example.nearbyairports.model.textsearch.TextSearchResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RetrofitMaps {
    @GET("api/place/findplacefromtext/json?inputtype=textquery&fields=photos,formatted_address,name,opening_hours,rating,geometry&locationbias=circle:{radius}@{lat},{long}")
    Call<PlacesResponse> findPlaceFromText(@Query("input") String input, @Path("radius") int radius, @Path("lat") Double lat, @Path("long") Double longitude, @Query("key") String key);

    @GET("api/place/textsearch/json")
    Call<TextSearchResponse> textSearch(@Query("type") String type, @Query("query") String query, @Query("radius") int radius, @Query("location") String location, @Query("key") String key);

    @GET("api/place/nearbysearch/json")
    Call<TextSearchResponse> nearbySearch(@Query("type") String type, @Query("keyword") String query, @Query("radius") int radius, @Query("location") String location, @Query("key") String key);
}