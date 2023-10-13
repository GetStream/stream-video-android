package io.getstream.video.android.data.repositories

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import io.getstream.video.android.data.dto.GetGoogleAccountsResponseDto
import io.getstream.video.android.data.dto.asDomainModel
import io.getstream.video.android.models.StreamUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class GoogleAccountRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val baseUrl = "https://people.googleapis.com/v1/people:listDirectoryPeople"

    suspend fun getAllAccounts(): List<StreamUser>? {
        val readMask = "readMask=emailAddresses,names,photos"
        val sources = "sources=DIRECTORY_SOURCE_TYPE_DOMAIN_PROFILE"
        val pageSize = "pageSize=1000"

        return GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
            withContext(Dispatchers.IO) {
                getAccessToken(account)?.let { accessToken ->
                    val urlString = "$baseUrl?access_token=$accessToken&$readMask&$sources&$pageSize"
                    val request = buildRequest(urlString)
                    val okHttpClient = buildOkHttpClient()
                    var responseBody: String?

                    okHttpClient.newCall(request).execute().let { response ->
                        if (response.isSuccessful) {
                            responseBody = response.body?.string()
                            responseBody?.let { parseUserListJson(it) }
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }

    private fun getAccessToken(account: GoogleSignInAccount) =
         try {
            GoogleAuthUtil.getToken(
                context,
                account.account,
                "oauth2:profile email"
            )
        } catch (e: Exception) {
            null
        }

    private fun buildRequest(urlString: String) = Request.Builder()
        .url(urlString.toHttpUrl())
        .build()

    private fun buildOkHttpClient() = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .build()

    @OptIn(ExperimentalStdlibApi::class)
    private fun parseUserListJson(jsonString: String): List<StreamUser>? {
        val moshi: Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val jsonAdapter: JsonAdapter<GetGoogleAccountsResponseDto> = moshi.adapter()

        val response = jsonAdapter.fromJson(jsonString)
        return response?.people?.map { it.asDomainModel() }
    }
}