package eu.cyben.guard.data.api

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eu.cyben.guard.BuildConfig
import eu.cyben.guard.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides @Singleton
    fun provideOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val auth = Interceptor { chain ->
            val token = tokenManager.getToken()
            val req = chain.request().newBuilder()
                .addHeader("User-Agent", "CybenGuard-Android/1.0")
                .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
                .build()
            chain.proceed(req)
        }
        return OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            }).build()
    }

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL).client(client)
        .addConverterFactory(GsonConverterFactory.create()).build()

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)
}
