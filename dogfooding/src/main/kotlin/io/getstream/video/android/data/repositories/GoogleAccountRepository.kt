package io.getstream.video.android.data.repositories

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import io.getstream.video.android.data.dto.GetGoogleAccountsResponseDto
import io.getstream.video.android.data.dto.asDomainModel
import io.getstream.video.android.models.GoogleAccount
import io.getstream.video.android.util.GoogleSignInHelper
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

    suspend fun getAllAccounts(): List<GoogleAccount>? {
        val readMask = "readMask=emailAddresses,names,photos"
        val sources = "sources=DIRECTORY_SOURCE_TYPE_DOMAIN_PROFILE"
        val pageSize = "pageSize=1000"

        return if (silentSignIn()) {
            GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
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
        } else {
            null
        }
    }

    private fun silentSignIn(): Boolean { // Used to refresh token
        val gsc = GoogleSignInHelper.getGoogleSignInClient(context)
        val task = gsc.silentSignIn()

        // Below code needed for debugging silent sign-in failures
        if (task.isSuccessful) {
            Log.d("Google Silent Sign In", "Successful")
            return true
        } else {
            task.addOnCompleteListener {
                try {
                    val signInAccount = task.getResult(ApiException::class.java)
                    Log.d("Google Silent Sign In", signInAccount.email.toString())
                } catch (apiException: ApiException) {
                    // You can get from apiException.getStatusCode() the detailed error code
                    // e.g. GoogleSignInStatusCodes.SIGN_IN_REQUIRED means user needs to take
                    // explicit action to finish sign-in;
                    // Please refer to GoogleSignInStatusCodes Javadoc for details
                    Log.d("Google Silent Sign In", apiException.statusCode.toString())
                }
            }
            return false
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
    private fun parseUserListJson(jsonString: String): List<GoogleAccount>? {
        val moshi: Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val jsonAdapter: JsonAdapter<GetGoogleAccountsResponseDto> = moshi.adapter()

        val response = jsonAdapter.fromJson(jsonString)
        return response?.people?.map { it.asDomainModel() }
    }

    fun getCurrentUser(): GoogleAccount {
        val currentUser = GoogleSignIn.getLastSignedInAccount(context)
        return GoogleAccount(
            email = currentUser?.email ?: "",
            id = currentUser?.id ?: "",
            name = currentUser?.givenName ?: "",
            photoUrl = currentUser?.photoUrl?.toString(),
            isFavorite = false
        )
    }
}