package eu.cyben.guard.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import eu.cyben.guard.R
import eu.cyben.guard.data.api.ApiService
import eu.cyben.guard.data.models.GuardAnalysis
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AnalysisHistoryActivity : AppCompatActivity() {
    @Inject lateinit var api: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@AnalysisHistoryActivity)
            setPadding(0, 56, 0, 0)
            setBackgroundColor(0xFF121212.toInt())
        }
        setContentView(rv)
        lifecycleScope.launch {
            try {
                val resp = api.getAnalyses()
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    rv.adapter = AnalysisAdapter(list)
                } else Toast.makeText(this@AnalysisHistoryActivity, "Errore caricamento", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@AnalysisHistoryActivity, "Errore di rete", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class AnalysisAdapter(private val items: List<GuardAnalysis>) : RecyclerView.Adapter<AnalysisAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvText: TextView = v.findViewById(android.R.id.text1)
        val tvRisk: TextView = v.findViewById(android.R.id.text2)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        v.setBackgroundColor(0xFF1E1E1E.toInt())
        v.setPadding(32, 24, 32, 24)
        v.findViewById<TextView>(android.R.id.text1).setTextColor(0xFFFFFFFF.toInt())
        v.findViewById<TextView>(android.R.id.text2).setTextColor(0xFF9E9E9E.toInt())
        return VH(v)
    }
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvText.text = item.inputText?.take(80) ?: "Analisi"
        val riskColor = when (item.riskLevel) {
            "dangerous" -> 0xFFFF5252.toInt()
            "suspicious" -> 0xFFFF9800.toInt()
            else -> 0xFF4CAF50.toInt()
        }
        holder.tvRisk.text = "${item.riskLabel ?: item.riskLevel ?: "Sicuro"} - ${item.createdAt?.take(10) ?: ""}"
        holder.tvRisk.setTextColor(riskColor)
    }
}
