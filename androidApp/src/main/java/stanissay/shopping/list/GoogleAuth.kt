package stanissay.shopping.list

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuth(private val activity: Activity) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(activity)

    suspend fun signIn(): Result<String> = runCatching {
        auth.currentUser?.let { return Result.success(it.uid) }

        val googleOption = GetSignInWithGoogleOption.Builder(
            activity.getString(R.string.default_web_client_id)
        ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val result = credentialManager.getCredential(
            context = activity,
            request = request
        )

        Log.d("LOGIN", result.toString())

        val credential = result.credential

        Log.d("LOGIN", credential.toString())

        check(credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Не вдалося отримати Google ID token" }

        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)

        Log.d("LOGIN", googleCredential.toString())

        val firebaseCredential = GoogleAuthProvider.getCredential(
            googleCredential.idToken,
            null
        )

        Log.d("LOGIN", firebaseCredential.toString())

        auth.signInWithCredential(firebaseCredential).await()
        auth.currentUser?.uid ?: error("Firebase не повернув користувача")
    }
}