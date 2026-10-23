package stanissay.shopping.shared

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuth(private val activity: Activity) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(activity)

    suspend fun signIn(): Result<GoogleUser> = runCatching {
        auth.currentUser?.let { user ->
            return@runCatching GoogleUser(
                uid = user.uid,
                name = user.displayName,
                email = user.email,
                photoUrl = user.photoUrl?.toString()
            )
        }

        val googleOption = GetGoogleIdOption.Builder()
            .setServerClientId(activity.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val result = credentialManager.getCredential(
            context = activity,
            request = request
        )

        val credential = result.credential

        check(
            credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Не вдалося отримати Google ID token" }

        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)

        val firebaseCredential = GoogleAuthProvider.getCredential(
            googleCredential.idToken,
            null
        )

        val authResult = auth.signInWithCredential(firebaseCredential).await()
        val user = authResult.user ?: error("Firebase не повернув користувача")

        GoogleUser(
            uid = user.uid,
            name = user.displayName ?: googleCredential.displayName,
            email = user.email ?: googleCredential.id,
            photoUrl = user.photoUrl?.toString()
                ?: googleCredential.profilePictureUri?.toString()
        )
    }

    fun signOut() {
        auth.signOut()
    }
}