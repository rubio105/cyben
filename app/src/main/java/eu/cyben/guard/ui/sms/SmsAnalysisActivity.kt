package eu.cyben.guard.ui.sms

import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import eu.cyben.guard.R
import eu.cyben.guard.receiver.SmsReceiver

class SmsAnalysisActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val alertType   = intent.getStringExtra("alert_type") ?: "suspicious"
        val sender      = intent.getStringExtra("sender") ?: "Sconosciuto"
        val body        = intent.getStringExtra("body") ?: ""
        val score       = intent.getFloatExtra("score", 0f)
        val urls        = intent.getStringArrayListExtra("urls") ?: arrayListOf()
        val notifId     = intent.getIntExtra("notification_id", SmsReceiver.NOTIF_ID_PHISHING)

        // cancel persistent notification
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notifId)

        // save suspect sender
        getSharedPreferences("suspect_senders", Context.MODE_PRIVATE)
            .edit().putString(sender, alertType).apply()

        val isPhishing = alertType == "phishing"
        val headerColor = if (isPhishing) Color.parseColor("#B71C1C") else Color.parseColor("#E65100")
        val scorePercent = (score * 100).toInt()

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }
        scroll.addView(root)

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(headerColor)
            setPadding(48, 80, 48, 40)
        }
        val icon = TextView(this).apply {
            text = if (isPhishing) "⚠️" else "🔍"
            textSize = 40f
            gravity = Gravity.CENTER
        }
        val headerTitle = TextView(this).apply {
            text = if (isPhishing) "SMS di Phishing Rilevato" else "SMS Sospetto"
            textSize = 22f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 8)
        }
        val senderText = TextView(this).apply {
            text = "Da: $sender"
            textSize = 14f
            setTextColor(Color.parseColor("#FFCDD2"))
            gravity = Gravity.CENTER
        }
        header.addView(icon)
        header.addView(headerTitle)
        header.addView(senderText)
        root.addView(header)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Risk bar
        val riskLabel = TextView(this).apply {
            text = "Livello di rischio: $scorePercent%"
            textSize = 14f
            setTextColor(Color.parseColor("#BBBBBB"))
            setPadding(0, 0, 0, 8)
        }
        val barBg = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#333333"))
            minimumHeight = 20
        }
        val barFill = View(this).apply {
            setBackgroundColor(headerColor)
            layoutParams = FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        barBg.addView(barFill)
        barBg.viewTreeObserver.addOnGlobalLayoutListener {
            val totalWidth = barBg.width
            barFill.layoutParams = FrameLayout.LayoutParams(
                (totalWidth * score).toInt(), FrameLayout.LayoutParams.MATCH_PARENT
            )
            barFill.requestLayout()
        }
        content.addView(riskLabel)
        content.addView(barBg)

        divider(content)

        // SMS body
        sectionTitle(content, "Contenuto SMS")
        val bodyText = TextView(this).apply {
            text = body.ifBlank { "(vuoto)" }
            textSize = 14f
            setTextColor(Color.parseColor("#EEEEEE"))
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(24, 20, 24, 20)
        }
        content.addView(bodyText)

        // URLs
        if (urls.isNotEmpty()) {
            divider(content)
            sectionTitle(content, "Link sospetti (${urls.size})")
            urls.forEach { url ->
                val urlView = TextView(this).apply {
                    text = "🔗 $url"
                    textSize = 13f
                    setTextColor(Color.parseColor("#FF6B6B"))
                    setPadding(0, 8, 0, 8)
                }
                content.addView(urlView)
            }
        }

        divider(content)

        // Buttons
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 24, 0, 0)
        }
        val btnIgnore = Button(this).apply {
            text = "Ignora"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#444444"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 16
            }
            setOnClickListener { finish() }
        }
        val btnBlock = Button(this).apply {
            text = "Blocca Mittente"
            setTextColor(Color.WHITE)
            setBackgroundColor(headerColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                getSharedPreferences("suspect_senders", Context.MODE_PRIVATE)
                    .edit().putString(sender, "blocked").apply()
                Toast.makeText(this@SmsAnalysisActivity, "Mittente bloccato", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        btnRow.addView(btnIgnore)
        btnRow.addView(btnBlock)
        content.addView(btnRow)

        root.addView(content)
        setContentView(scroll)
        supportActionBar?.apply {
            title = "Analisi SMS"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun divider(parent: LinearLayout) {
        parent.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#333333"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { topMargin = 24; bottomMargin = 24 }
        })
    }

    private fun sectionTitle(parent: LinearLayout, text: String) {
        parent.addView(TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.parseColor("#888888"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 12)
            isAllCaps = true
        })
    }
}
