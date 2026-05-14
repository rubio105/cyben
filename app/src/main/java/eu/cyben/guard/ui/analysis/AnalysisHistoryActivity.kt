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
                        binding.rvHistory.visibility = View.GONE
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
        inner class VH(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val layout = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL; setPadding(40, 28, 40, 28)
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            return VH(layout)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.layout.removeAllViews()
            val riskColor = when (item.riskLevel) { "dangerous" -> 0xFFFF1744.toInt(); "suspicious" -> 0xFFFF9800.toInt(); else -> 0xFF4CAF50.toInt() }
            holder.layout.addView(TextView(holder.layout.context).apply {
                text = "${item.riskLabel} - ${item.inputType ?: "testo"}"; setTextColor(riskColor); textSize = 14f; setPadding(0, 0, 0, 4)
            })
            holder.layout.addView(TextView(holder.layout.context).apply {
                text = item.inputText?.take(80) ?: ""; setTextColor(0xFFB0B0C0.toInt()); textSize = 12f; setPadding(0, 0, 0, 4)
            })
            holder.layout.addView(TextView(holder.layout.context).apply {
                text = item.createdAt?.take(10) ?: ""; setTextColor(0xFF707080.toInt()); textSize = 11f
            })
            if (position < items.size - 1) {
                holder.layout.addView(View(holder.layout.context).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).also { it.topMargin = 16 }
                    setBackgroundColor(0xFF2A2A4A.toInt())
                })
            }
        }
        override fun getItemCount() = items.size
    }
}