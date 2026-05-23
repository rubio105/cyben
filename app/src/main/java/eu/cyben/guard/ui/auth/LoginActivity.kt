package eu.cyben.guard.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.onesignal.OneSignal
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.R
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.api.EmailRequest
import eu.cyben.guard.data.api.LoginRequest
import eu.cyben.guard.data.models.GoogleAuthRequest
import eu.cyben.guard.databinding.ActivityLoginBinding
import eu.cyben.guard.ui.dashboard.DashboardActivity
import eu.cyben.guard.utils.AppLockManager
import eu.cyben.guard.utils.TokenManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    @Inject lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private var passwordVisible = false

    companion object { private const val RC_SIGN_IN = 9001 }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (tokenManager.isLoggedIn()) { goToDashboard(); return }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("743094303265-k7i1je5nglbjf1vgdt0shj83dq9brlsu.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnLogin.setOnClickListener { doLogin() }
        binding.btnRegister.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
        binding.btnTogglePwd.setOnClickListener { togglePassword() }
        binding.btnGoogleSignIn.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) { binding.etEmail.error = "Inserisci la tua email"; return@setOnClickListener }
            lifecycleScope.launch {
                try {
                    api.forgotPassword(EmailRequest(email))
                    Toast.makeText(this@LoginActivity, "Email di recupero inviata", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken ?: return
                doGoogleLogin(idToken)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In fallito: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun doGoogleLogin(idToken: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = api.googleLogin(GoogleAuthRequest(idToken))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body?.token != null) { tokenManager.saveToken(body.token); goToDashboard() }
                    else Toast.makeText(this@LoginActivity, body?.error ?: "Errore Google login", Toast.LENGTH_LONG).show()
                } else Toast.makeText(this@LoginActivity, "Errore Google Sign-In", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Errore di rete", Toast.LENGTH_SHORT).show()
            } finally { setLoading(false) }
        }
    }

    private fun togglePassword() {
        passwordVisible = !passwordVisible
        val cursor = binding.etPassword.selectionEnd
        binding.etPassword.transformationMethod = if (passwordVisible)
            HideReturnsTransformationMethod.getInstance()
        else PasswordTransformationMethod.getInstance()
        binding.etPassword.setSelection(cursor)
        binding.btnTogglePwd.setImageResource(if (passwordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye)
    }

    private fun doLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        if (email.isEmpty()) { binding.etEmail.error = "Campo obbligatorio"; return }
        if (password.isEmpty()) { binding.etPassword.error = "Campo obbligatorio"; return }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = api.login(LoginRequest(email, password))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    when {
                        body?.needsVerification == true -> startActivity(Intent(this@LoginActivity, EmailVerificationActivity::class.java).apply { putExtra("email", email) })
                        body?.token != null -> { tokenManager.saveToken(body.token); goToDashboard() }
                        else -> Toast.makeText(this@LoginActivity, body?.error ?: "Login fallito", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val msg = when (resp.code()) {
                        401 -> "Email o password non corretti"
                        403 -> "Account bloccato"
                        429 -> "Troppi tentativi, riprova tra poco"
                        else -> "Servizio temporaneamente non disponibile"
                    }
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Nessuna connessione internet", Toast.LENGTH_LONG).show()
            } finally { setLoading(false) }
        }
    }

    private fun goToDashboard() {
        lifecycleScope.launch {
            OneSignal.Notifications.requestPermission(true)
        }
        val appLock = AppLockManager(this)
        if (!appLock.isEnabled) { startActivity(Intent(this, DashboardActivity::class.java)); finish(); return }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = androidx.biometric.BiometricPrompt(this, executor, object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(r: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java)); finish()
            }
            override fun onAuthenticationError(code: Int, msg: CharSequence) {
                if (code != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                    code != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON)
                    Toast.makeText(this@LoginActivity, "Errore biometria: $msg", Toast.LENGTH_SHORT).show()
            }
            override fun onAuthenticationFailed() {}
        })
        prompt.authenticate(androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Sblocca Cyben Defender")
            .setSubtitle("Usa la biometria per accedere")
            .setNegativeButtonText("Annulla").build())
    }

    private fun setLoading(b: Boolean) {
        binding.progressBar.visibility = if (b) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !b
    }
}
