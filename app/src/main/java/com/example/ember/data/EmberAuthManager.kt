package com.example.ember.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.ember.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class EmberAuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ember_auth_prefs", Context.MODE_PRIVATE)
    private val fbAuth = FirebaseAuth.getInstance()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "EmberAuthManager"
        private const val SUPABASE_URL = "https://ombptejuakpgaapeclyj.supabase.co"
        private const val SUPABASE_ANON_KEY = "sb_publishable_RJOx9D6cYvi_75XCoWY8Dw_VDUHR8eF"

        @Volatile
        private var INSTANCE: EmberAuthManager? = null

        fun getInstance(context: Context): EmberAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmberAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    var currentUser: User? = null
        private set

    init {
        loadPersistedUser()
        refreshFirebaseTokenIfPossible()
    }

    private fun loadPersistedUser() {
        val fbUser = fbAuth.currentUser
        if (fbUser != null) {
            val id = fbUser.uid
            val email = fbUser.email ?: ""
            val displayName = fbUser.displayName ?: prefs.getString("display_name", "") ?: (if (email.isNotEmpty()) email.substringBefore("@") else "Listener")
            val avatarUrl = fbUser.photoUrl?.toString() ?: prefs.getString("avatar_url", null)
            val token = prefs.getString("token", null)
            currentUser = User(id, email, displayName, avatarUrl, token, isGuest = false)
            return
        }

        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (isLoggedIn) {
            val id = prefs.getString("user_id", "") ?: ""
            val email = prefs.getString("email", "") ?: ""
            val displayName = prefs.getString("display_name", "") ?: (if (email.isNotEmpty()) email.substringBefore("@") else "Listener")
            val avatarUrl = prefs.getString("avatar_url", null)
            val token = prefs.getString("token", null)
            val isGuest = prefs.getBoolean("is_guest", false)

            if (id.isNotEmpty()) {
                currentUser = User(id, email, displayName, avatarUrl, token, isGuest)
                return
            }
        }

        // New install / logged-out user: require login
        currentUser = null
    }

    private fun refreshFirebaseTokenIfPossible() {
        try {
            fbAuth.currentUser?.getIdToken(false)?.addOnSuccessListener { res ->
                res.token?.let { freshToken ->
                    currentUser?.let { u ->
                        currentUser = u.copy(token = freshToken)
                        prefs.edit().putString("token", freshToken).apply()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "refreshFirebaseToken error: ${e.message}")
        }
    }

    suspend fun getFreshToken(): String? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            val fbUser = fbAuth.currentUser
            if (fbUser == null) {
                cont.resume(currentUser?.token)
                return@suspendCancellableCoroutine
            }
            fbUser.getIdToken(false)
                .addOnSuccessListener { result ->
                    val token = result.token
                    if (token != null) {
                        currentUser = currentUser?.copy(token = token)
                        prefs.edit().putString("token", token).apply()
                    }
                    cont.resume(token ?: currentUser?.token)
                }
                .addOnFailureListener {
                    cont.resume(currentUser?.token)
                }
        }
    }

    fun persistUser(user: User) {
        currentUser = user
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_id", user.id)
            putString("email", user.email)
            putString("display_name", user.displayName)
            putString("avatar_url", user.avatarUrl)
            putString("token", user.token)
            putBoolean("is_guest", user.isGuest)
            apply()
        }
    }

    /**
     * Resolves canonical user profile from Supabase profiles table using email.
     * Supabase profiles table is public read-only and maps email to canonical UID.
     */
    suspend fun resolveSupabaseProfileByEmail(email: String): User? = withContext(Dispatchers.IO) {
        if (email.isBlank()) return@withContext null
        try {
            val url = "$SUPABASE_URL/rest/v1/profiles?email=eq.${email.trim()}&select=id,email,display_name,avatar_url"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            if (response.isSuccessful) {
                val arr = JSONArray(body)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    val uid = obj.getString("id")
                    val name = obj.optString("display_name", email.substringBefore("@"))
                    val avatar = obj.optString("avatar_url", null)
                    Log.i(TAG, "Found verified Supabase profile for $email -> $uid ($name)")
                    return@withContext User(
                        id = uid,
                        email = email.trim(),
                        displayName = name,
                        avatarUrl = if (avatar.isNullOrEmpty() || avatar == "null") null as String? else avatar,
                        token = null,
                        isGuest = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveSupabaseProfileByEmail error: ${e.message}")
        }
        return@withContext null
    }

    suspend fun signIn(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        if (email.isBlank()) {
            return@withContext Result.failure(Exception("Email cannot be empty."))
        }

        val trimmedEmail = email.trim()

        // 1. Try Firebase Auth sign in
        val fbResult = suspendCancellableCoroutine<Result<User>?> { cont ->
            if (password.isBlank()) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            fbAuth.signInWithEmailAndPassword(trimmedEmail, password.trim())
                .addOnSuccessListener { authResult ->
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        fbUser.getIdToken(true).addOnSuccessListener { tokenRes ->
                            val user = User(
                                id = fbUser.uid,
                                email = fbUser.email ?: trimmedEmail,
                                displayName = fbUser.displayName ?: trimmedEmail.substringBefore("@"),
                                avatarUrl = fbUser.photoUrl?.toString(),
                                token = tokenRes.token,
                                isGuest = false
                            )
                            cont.resume(Result.success(user))
                        }.addOnFailureListener {
                            val user = User(
                                id = fbUser.uid,
                                email = fbUser.email ?: trimmedEmail,
                                displayName = fbUser.displayName ?: trimmedEmail.substringBefore("@"),
                                avatarUrl = fbUser.photoUrl?.toString(),
                                token = null,
                                isGuest = false
                            )
                            cont.resume(Result.success(user))
                        }
                    } else {
                        cont.resume(null)
                    }
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }

        if (fbResult != null && fbResult.isSuccess) {
            val user = fbResult.getOrThrow()
            val sbUser = resolveSupabaseProfileByEmail(trimmedEmail)
            val finalUser = if (sbUser != null) {
                user.copy(id = sbUser.id, displayName = sbUser.displayName, avatarUrl = sbUser.avatarUrl ?: user.avatarUrl)
            } else {
                user
            }
            persistUser(finalUser)
            return@withContext Result.success(finalUser)
        }

        // 2. Direct Supabase account link by email (e.g., user signed in with Google on web)
        val sbUser = resolveSupabaseProfileByEmail(trimmedEmail)
        if (sbUser != null) {
            persistUser(sbUser)
            return@withContext Result.success(sbUser)
        }

        return@withContext Result.failure(Exception("Could not sign in with this email. Please check your credentials or register."))
    }

    suspend fun signUp(email: String, password: String, displayName: String): Result<User> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val name = if (displayName.isNotBlank()) displayName.trim() else trimmedEmail.substringBefore("@")

        // Check if account already exists in Supabase
        val existing = resolveSupabaseProfileByEmail(trimmedEmail)
        if (existing != null) {
            persistUser(existing)
            return@withContext Result.success(existing)
        }

        suspendCancellableCoroutine { cont ->
            fbAuth.createUserWithEmailAndPassword(trimmedEmail, password.trim())
                .addOnSuccessListener { authResult ->
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        fbUser.updateProfile(profileUpdates)

                        fbUser.getIdToken(true).addOnSuccessListener { tokenRes ->
                            val user = User(
                                id = fbUser.uid,
                                email = fbUser.email ?: trimmedEmail,
                                displayName = name,
                                avatarUrl = null,
                                token = tokenRes.token,
                                isGuest = false
                            )
                            persistUser(user)
                            cont.resume(Result.success(user))
                        }.addOnFailureListener {
                            val user = User(
                                id = fbUser.uid,
                                email = fbUser.email ?: trimmedEmail,
                                displayName = name,
                                avatarUrl = null,
                                token = null,
                                isGuest = false
                            )
                            persistUser(user)
                            cont.resume(Result.success(user))
                        }
                    } else {
                        cont.resume(Result.failure(Exception("Could not create user account.")))
                    }
                }
                .addOnFailureListener { ex ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val sb = resolveSupabaseProfileByEmail(trimmedEmail)
                        if (sb != null) {
                            persistUser(sb)
                            cont.resume(Result.success(sb))
                        } else {
                            cont.resume(Result.failure(ex))
                        }
                    }
                }
        }
    }

    fun saveGoogleUser(user: User) {
        persistUser(user)
        CoroutineScope(Dispatchers.IO).launch {
            val sb = resolveSupabaseProfileByEmail(user.email)
            if (sb != null) {
                val finalUser = user.copy(
                    id = sb.id,
                    displayName = sb.displayName,
                    avatarUrl = sb.avatarUrl ?: user.avatarUrl
                )
                withContext(Dispatchers.Main) {
                    persistUser(finalUser)
                }
            }
        }
    }

    fun continueAsGuest(): User {
        val guest = User(
            id = "guest_" + System.currentTimeMillis(),
            email = "guest@ember.local",
            displayName = "Guest Listener",
            avatarUrl = null,
            token = null,
            isGuest = true
        )
        persistUser(guest)
        return guest
    }

    fun signOut() {
        try {
            fbAuth.signOut()
        } catch (_: Exception) {}
        currentUser = null
        prefs.edit().apply {
            putBoolean("is_logged_in", false)
            remove("user_id")
            remove("email")
            remove("display_name")
            remove("avatar_url")
            remove("token")
            putBoolean("is_guest", false)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = currentUser != null && !currentUser!!.isGuest
}
