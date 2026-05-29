package eu.cyben.mobile.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import eu.cyben.mobile.R
import eu.cyben.mobile.databinding.ActivityLoginBinding
import eu.cyben.mobile.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                showError(getString(R.string.google_signin_failed, e.statusCode))
                showLoading(false)
            }
        } else {
            showLoading(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        setupGoogleSignIn()
        setupViews()

        if (auth.currentUser != null) {
            navigateToDashboard()
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupViews() {
        binding.btnAccedi.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isEmpty()) { binding.etEmail.error = getString(R.string.error_email_required); return@setOnClickListener }
            if (password.isEmpty()) { binding.etPassword.error = getString(R.string.error_password_required); return@setOnClickListener }
            signInWithEmail(email, password)
        }
        binding.btnGoogleSignIn.setOnClickListener {
            showLoading(true)
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }
        binding.tvPasswordDimenticata.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) { binding.etEmail.error = getString(R.string.error_email_required); return@setOnClickListener }
            resetPassword(email)
        }
        binding.tvRegistrati.setOnClickListener {
            Toast.makeText(this, getString(R.string.register_redirect), Toast.LENGTH_LONG).show()
        }
    }

    private fun signInWithEmail(email: String, password: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                navigateToDashboard()
            } catch (e: Exception) {
                showError(getString(R.string.error_invalid_credentials))
                showLoading(false)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                navigateToDashboard()
            } catch (e: Exception) {
                showError(getString(R.string.google_signin_failed, e.message ?: "unknown"))
                showLoading(false)
            }
        }
    }

    private fun resetPassword(email: String) {
        lifecycleScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                Toast.makeText(this@LoginActivity, getString(R.string.password_reset_sent, email), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                showError(getString(R.string.error_reset_password, e.message ?: ""))
            }
        }
    }

    private fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun showError(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnAccedi.isEnabled = !loading
        binding.btnGoogleSignIn.isEnabled = !loading
        binding.etEmail.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
    }
}
