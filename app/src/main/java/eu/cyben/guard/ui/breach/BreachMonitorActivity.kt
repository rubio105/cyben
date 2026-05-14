package eu.cyben.guard.ui.breach

import android.content.Intent
import android.os.Bundle
import android.security.keystore.KeyProperties
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.data.api.AddEmailRequest
import com.bumptech.glide.Glide
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.api.HibpCheckRequest
import eu.cyben.guard.databinding.ActivityBreachMonitorBinding
import eu.cyben.guard.data.models.GuardMonitoredEmail
import eu.cyben.guard.data.models.GuardBreachAlert
import eu.cyben.guard.ui.dashboard.DashboardActivity
import eu.cyben.guard.ui.settings.SettingsActivity
import eu.cyben.guard.ui.vpn.VPNActivity
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@AndroidEntryPoint
class BreachMonitorActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    private lateinit var binding: ActivityBreachMonitorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBreachMonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAddEmail.setOnClickListener { showAddEmailDialog() }
        binding.btnHibp.setOnClickListener { showHibpDialog() }
        setupNav()
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val emailsResp = api.getMonitoredEmails()
                val alertsResp = api.getBreachAlerts()
                if (emailsResp.isSuccessful) {
                    val emails = emailsResp.body() ?: emptyList()
                    binding.tvEmailLabel.text = "Email monitorate (${emails.size}/2)"
                    binding.tvBreachSubheader.text = "${emails.size}/2 email monitorate · verifica password disponibile"
                    val breached = emails.sumOf { it.breachCount ?: 0 }
                    if (breached > 0) {
                        binding.tvBreachStatus.text = "$breached violazioni trovate"
                        binding.tvBreachStatus.setTextColor(0xFFFF1744.toInt())
                    } else {
                        binding.tvBreachStatus.text = "Nessuna rilevata"
                        binding.tvBreachStatus.setTextColor(0xFF66BB6A.toInt())
                    }
                    binding.rvEmails.layoutManager = LinearLayoutManager(this@BreachMonitorActivity)
                    binding.rvEmails.adapter = EmailAdapter(emails)
                }
                if (alertsResp.isSuccessful) {
                    val alerts = alertsResp.body() ?: emptyList()
                    if (alerts.isNotEmpty()) {
                        binding.tvBreachAlertsLabel.visibility = View.VISIBLE
                        binding.rvBreachAlerts.visibility = View.VISIBLE
                        binding.rvBreachAlerts.layoutManager = LinearLayoutManager(this@BreachMonitorActivity)
                        binding.rvBreachAlerts.adapter = AlertAdapter(alerts)
                    } else {
                        binding.tvBreachAlertsLabel.visibility = View.GONE
                        binding.rvBreachAlerts.visibility = View.GONE
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun showAddEmailDialog() {
        val input = EditText(this).apply { hint = "email@esempio.com"; inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS; setPadding(48, 32, 48, 32) }
        AlertDialog.Builder(this).setTitle("Aggiungi email").setView(input)
            .setPositiveButton("Aggiungi") { _, _ ->
                val email = input.text.toString().trim()
                if (email.contains("@")) addEmail(email)
                else Toast.makeText(this, "Email non valida", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annulla", null).show()
    }

    private fun addEmail(email: String) {
        binding.progressBreach.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.addMonitoredEmail(AddEmailRequest(email, null))
                if (resp.isSuccessful) {
                    val added = resp.body()
                    Toast.makeText(this@BreachMonitorActivity, "Email aggiunta, verifica in corso...", Toast.LENGTH_SHORT).show()
                    if (added?.id != null) {
                        try { api.checkBreach(added.id) } catch (_: Exception) {}
                    }
                    loadData()
                } else Toast.makeText(this@BreachMonitorActivity, "Errore aggiunta email", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this@BreachMonitorActivity, "Errore di rete", Toast.LENGTH_SHORT).show() }
            finally { binding.progressBreach.visibility = View.GONE }
        }
    }

    private fun showHibpDialog() {
        val input = EditText(this).apply {
            hint = "Inserisci la password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this).setTitle("Verifica password")
            .setMessage("La password non verra' inviata in chiaro. Viene usato SHA-1 k-anonymity.")
            .setView(input)
            .setPositiveButton("Verifica") { _, _ ->
                val pwd = input.text.toString()
                if (pwd.isNotEmpty()) checkPassword(pwd)
            }
            .setNegativeButton("Annulla", null).show()
    }

    private fun checkPassword(password: String) {
        binding.progressBreach.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val sha1 = sha1(password).uppercase()
                val prefix = sha1.take(5)
                val suffix = sha1.drop(5)
                val resp = okhttp3.OkHttpClient().newCall(
                    okhttp3.Request.Builder().url("https://api.pwnedpasswords.com/range/$prefix").build()
                ).execute()
                val body = resp.body?.string() ?: ""
                val count = body.lines().find { it.startsWith(suffix) }?.split(":")?.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                if (count > 0) {
                    AlertDialog.Builder(this@BreachMonitorActivity)
                        .setTitle("Password compromessa")
                        .setMessage("Questa password e' apparsa $count volte in violazioni note. Cambiala subito.")
                        .setPositiveButton("OK", null).show()
                } else {
                    Toast.makeText(this@BreachMonitorActivity, "Password non trovata in violazioni note", Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) { Toast.makeText(this@BreachMonitorActivity, "Errore verifica", Toast.LENGTH_SHORT).show() }
            finally { binding.progressBreach.visibility = View.GONE }
        }
    }

    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun setupNav() {
        binding.tabAnalizza.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        binding.tabViolazioni.setOnClickListener { /* already here */ }
        binding.tabVPN.setOnClickListener {
            startActivity(Intent(this, VPNActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        binding.tabImpostazioni.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }

    inner class EmailAdapter(private val items: List<GuardMonitoredEmail>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<EmailAdapter.VH>() {
        inner class VH(val root: android.widget.LinearLayout) : androidx.recyclerview.widget.RecyclerView.ViewHolder(root)
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val ll = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(40, 28, 40, 28)
                val lp = android.view.ViewGroup.MarginLayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 0, 8)
                layoutParams = lp
                setBackgroundResource(eu.cyben.guard.R.drawable.settings_card_bg)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            return VH(ll)
        }
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val ll = holder.root
            ll.removeAllViews()
            val count = item.breachCount ?: 0
            val icon = android.widget.TextView(ll.context).apply {
                text = if (count > 0) "\u26a0" else "\u2714"
                textSize = 20f
                setTextColor(if (count > 0) 0xFFFF5252.toInt() else 0xFF66BB6A.toInt())
                setPadding(0, 0, 28, 0)
            }
            val info = android.widget.LinearLayout(ll.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvEmail = android.widget.TextView(ll.context).apply {
                text = item.email
                setTextColor(0xFFEEEEFF.toInt())
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val tvSub = android.widget.TextView(ll.context).apply {
                text = if (count > 0) "$count violazioni trovate" else "Nessuna violazione"
                setTextColor(if (count > 0) 0xFFFF5252.toInt() else 0xFF66BB6A.toInt())
                textSize = 12f
            }
            info.addView(tvEmail)
            info.addView(tvSub)
            ll.addView(icon)
            ll.addView(info)
        }
    }

    inner class AlertAdapter(private val items: List<GuardBreachAlert>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<AlertAdapter.VH>() {
        inner class VH(val root: android.widget.LinearLayout, val ll: android.widget.LinearLayout, val logo: android.widget.ImageView) : androidx.recyclerview.widget.RecyclerView.ViewHolder(root)
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val outer = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(40, 28, 40, 28)
                val lp = android.view.ViewGroup.MarginLayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 0, 16)
                layoutParams = lp
                setBackgroundResource(eu.cyben.guard.R.drawable.settings_card_bg)
                gravity = android.view.Gravity.START
            }
            val logo = android.widget.ImageView(parent.context).apply {
                id = android.view.View.generateViewId()
                val lp = android.widget.LinearLayout.LayoutParams(80, 80)
                lp.marginEnd = 24
                layoutParams = lp
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                tag = "logo"
            }
            val ll = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            outer.addView(logo)
            outer.addView(ll)
            return VH(outer, ll, logo)
        }
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val ll = holder.ll
            ll.removeAllViews()
            val domain = item.breachName?.lowercase()?.replace(" ", "") + ".com"
            val logoUrl = "https://logo.clearbit.com/$domain"
            Glide.with(ll.context).load(logoUrl)
                .placeholder(eu.cyben.guard.R.drawable.icon_bg_red)
                .error(eu.cyben.guard.R.drawable.icon_bg_red)
                .circleCrop()
                .into(holder.logo)
            val tvName = android.widget.TextView(ll.context).apply {
                text = "⚠️ " + (item.breachName ?: "Violazione sconosciuta")
                setTextColor(0xFFFF5252.toInt())
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            ll.addView(tvName)
            if (!item.description.isNullOrEmpty()) {
                val tvDesc = android.widget.TextView(ll.context).apply {
                    text = item.description.take(120)
                    setTextColor(0xFFB0B0C0.toInt())
                    textSize = 12f
                    setPadding(0, 6, 0, 0)
                }
                ll.addView(tvDesc)
            }
            if (!item.dataClasses.isNullOrEmpty()) {
                val tvCats = android.widget.TextView(ll.context).apply {
                    text = "Dati esposti: " + item.dataClasses.joinToString(", ")
                    setTextColor(0xFFFF9800.toInt())
                    textSize = 11f
                    setPadding(0, 4, 0, 0)
                }
                ll.addView(tvCats)
            }
        }
    }

}