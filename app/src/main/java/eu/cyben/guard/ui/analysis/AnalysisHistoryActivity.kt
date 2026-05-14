package eu.cyben.guard.ui.analysis

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import android.content.Intent
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.ui.breach.BreachMonitorActivity
import eu.cyben.guard.ui.dashboard.DashboardActivity
import eu.cyben.guard.ui.settings.SettingsActivity
import eu.cyben.guard.ui.vpn.VPNActivity
import eu.cyben.guard.data.models.GuardAnalysis
import eu.cyben.guard.databinding.ActivityAnalysisHistoryBinding
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AnalysisHistoryActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService
    private lateinit var binding: ActivityAnalysisHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        setupNav()
        loadHistory()
    }

    private fun loadHistory() {
        binding.progressHistory.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = api.getAnalyses()
                if (resp.isSuccessful) {
                    val analyses = resp.body() ?: emptyList()
                    if (analyses.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvHistory.visibility = View.INVISIBLE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvHistory.visibility = View.VISIBLE
                        binding.rvHistory.layoutManager = LinearLayoutManager(this@AnalysisHistoryActivity)
                        binding.rvHistory.adapter = AnalysisAdapter(analyses)
                    }
                }
            } catch (_: Exception) {}
            finally { binding.progressHistory.visibility = View.GONE }
        }
    }

    private fun setupNav() {
        binding.tabAnalizza.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        binding.tabViolazioni.setOnClickListener {
            startActivity(Intent(this, BreachMonitorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        binding.tabVPN.setOnClickListener {
            startActivity(Intent(this, VPNActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        binding.tabStorico.setOnClickListener { /* already here */ }
        binding.tabImpostazioni.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }

    inner class AnalysisAdapter(private val items: List<GuardAnalysis>) : RecyclerView.Adapter<AnalysisAdapter.VH>() {
        inner class VH(val card: LinearLayout) : RecyclerView.ViewHolder(card)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val card = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 32, 40, 32)
                val lp = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.setMargins(32, 12, 32, 0)
                layoutParams = lp
                setBackgroundResource(eu.cyben.guard.R.drawable.settings_card_bg)
            }
            return VH(card)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.card.removeAllViews()

            val riskColor = when (item.riskLevel) {
                "dangerous" -> 0xFFD32F2F.toInt()
                "suspicious" -> 0xFFFF6F00.toInt()
                else -> 0xFF388E3C.toInt()
            }
            val riskEmoji = when (item.riskLevel) { "dangerous" -> "🔴"; "suspicious" -> "🟠"; else -> "🟢" }
            val typeLabel = when (item.inputType) { "call" -> "Chiamata"; "sms" -> "SMS"; "image" -> "Immagine"; else -> "Testo" }

            // Row: emoji + label + type + date
            val rowTop = LinearLayout(holder.card.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            rowTop.addView(TextView(holder.card.context).apply {
                text = riskEmoji; textSize = 18f
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = 10
                layoutParams = lp
            })
            rowTop.addView(TextView(holder.card.context).apply {
                text = item.riskLabel; setTextColor(riskColor); textSize = 14f; setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            rowTop.addView(TextView(holder.card.context).apply {
                text = typeLabel; setTextColor(0xFF707090.toInt()); textSize = 11f
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = 10
                layoutParams = lp
            })
            rowTop.addView(TextView(holder.card.context).apply {
                text = item.createdAt?.take(10) ?: ""; setTextColor(0xFF505070.toInt()); textSize = 11f
            })
            holder.card.addView(rowTop)

            // Input text preview
            if (!item.inputText.isNullOrBlank()) {
                holder.card.addView(TextView(holder.card.context).apply {
                    text = item.inputText.take(90) + if ((item.inputText.length) > 90) "…" else ""
                    setTextColor(0xFFB0B8D0.toInt()); textSize = 12f
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.topMargin = 8
                    layoutParams = lp
                })
            }

            // Explanation
            if (!item.explanation.isNullOrBlank()) {
                holder.card.addView(TextView(holder.card.context).apply {
                    text = item.explanation.take(120) + if ((item.explanation.length) > 120) "…" else ""
                    setTextColor(0xFF8090A8.toInt()); textSize = 11f
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.topMargin = 6
                    layoutParams = lp
                })
            }
        }

        override fun getItemCount() = items.size
    }
}