package eu.cyben.guard.ui.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import eu.cyben.guard.R
import eu.cyben.guard.data.models.ChatMessage

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_AI_TEXT = 1
        private const val TYPE_AI_ANALYSIS = 2
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return when {
            msg.isUser -> TYPE_USER
            msg.analysis != null -> TYPE_AI_ANALYSIS
            else -> TYPE_AI_TEXT
        }
    }

    inner class TextVH(val tv: TextView, val container: LinearLayout) : RecyclerView.ViewHolder(container)
    inner class AnalysisVH(val container: LinearLayout) : RecyclerView.ViewHolder(container)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_AI_ANALYSIS -> {
                val container = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(16, 8, 16, 8)
                    }
                }
                AnalysisVH(container)
            }
            else -> {
                val container = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(16, 8, 16, 8)
                    }
                }
                val tv = TextView(parent.context).apply {
                    setPadding(32, 24, 32, 24)
                    textSize = 15f
                    maxWidth = (parent.width * 0.80).toInt()
                }
                container.addView(tv)
                TextVH(tv, container)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is TextVH -> bindText(holder, msg)
            is AnalysisVH -> bindAnalysis(holder, msg)
        }
    }

    private fun bindText(holder: TextVH, msg: ChatMessage) {
        holder.tv.text = msg.text
        if (msg.isUser) {
            holder.tv.setBackgroundResource(R.drawable.bg_bubble_user)
            holder.tv.setTextColor(Color.WHITE)
            holder.container.gravity = Gravity.END
        } else {
            holder.tv.setBackgroundResource(R.drawable.bg_bubble_ai)
            holder.tv.setTextColor(ContextCompat.getColor(holder.tv.context, R.color.text_primary))
            holder.container.gravity = Gravity.START
        }
    }

    private fun bindAnalysis(holder: AnalysisVH, msg: ChatMessage) {
        val analysis = msg.analysis ?: return
        val ctx = holder.container.context
        val isItalian = java.util.Locale.getDefault().language == "it"
        holder.container.removeAllViews()
        holder.container.gravity = Gravity.START

        val outerCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // Text bubble above card (like iOS AssistantMessageBubble)
        if (msg.text.isNotBlank()) {
            val textBubble = TextView(ctx).apply {
                text = msg.text
                textSize = 15f
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.bg_bubble_ai)
                setPadding(32, 20, 32, 20)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(ctx, 8)
                }
            }
            outerCol.addView(textBubble)
        }

        val riskColor = when (analysis.resolvedRiskLevel) {
            "dangerous" -> Color.parseColor("#EF4444")
            "suspicious" -> Color.parseColor("#F59E0B")
            "safe" -> Color.parseColor("#22C55E")
            else -> Color.parseColor("#F59E0B")
        }
        val score = (analysis.resolvedRiskScore ?: analysis.riskScore) ?: 0

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 12), dp(ctx, 12), dp(ctx, 12))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0D1827"))
                cornerRadius = dp(ctx, 14).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // Header: icon + label + circular score
        val headerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val riskIcon = TextView(ctx).apply {
            text = when (analysis.resolvedRiskLevel) { "safe" -> "✅"; "dangerous" -> "🛡️"; else -> "⚠️" }
            textSize = 18f
            setPadding(0, 0, dp(ctx, 8), 0)
        }
        val riskTitle = TextView(ctx).apply {
            text = analysis.riskLabel
            textSize = 16f
            setTextColor(riskColor)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val scoreCircle = CircularScoreView(ctx).apply {
            progress = score / 100f
            scoreColor = riskColor
            val size = dp(ctx, 52)
            layoutParams = LinearLayout.LayoutParams(size, size)
        }
        headerRow.addView(riskIcon)
        headerRow.addView(riskTitle)
        headerRow.addView(scoreCircle)
        card.addView(headerRow)

        // Indicators
        val indicators = analysis.indicators?.take(3) ?: emptyList()
        if (indicators.isNotEmpty()) {
            card.addView(spacer(ctx, 8))
            indicators.forEach { indicator ->
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = dp(ctx, 4)
                    }
                }
                val dot = TextView(ctx).apply {
                    text = "ℹ️"
                    textSize = 13f
                    setPadding(0, 0, dp(ctx, 6), 0)
                }
                val indText = TextView(ctx).apply {
                    text = indicator
                    textSize = 13f
                    setTextColor(Color.parseColor("#B8C2CC"))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(dot)
                row.addView(indText)
                card.addView(row)
            }
        }

        // Detail box
        if (!analysis.explanation.isNullOrBlank()) {
            card.addView(spacer(ctx, 8))
            val detailBox = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(ctx, 10), dp(ctx, 10), dp(ctx, 10), dp(ctx, 10))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#0D263F"))
                    cornerRadius = dp(ctx, 10).toFloat()
                }
            }
            val detailLabel = TextView(ctx).apply {
                text = if (isItalian) "Dettaglio analisi" else "Analysis detail"
                textSize = 11f
                setTextColor(Color.parseColor("#9CA3AF"))
                typeface = Typeface.DEFAULT_BOLD
                isAllCaps = true
                setPadding(0, 0, 0, dp(ctx, 4))
            }
            val detailText = TextView(ctx).apply {
                text = analysis.explanation
                textSize = 13f
                setTextColor(Color.WHITE)
            }
            detailBox.addView(detailLabel)
            detailBox.addView(detailText)
            card.addView(detailBox)
        }

        // Recommendation
        if (!analysis.recommendation.isNullOrBlank()) {
            card.addView(spacer(ctx, 8))
            val tipRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FFD54F").let { Color.argb(18, Color.red(it), Color.green(it), Color.blue(it)) })
                    cornerRadius = dp(ctx, 8).toFloat()
                }
                setPadding(dp(ctx, 8), dp(ctx, 8), dp(ctx, 8), dp(ctx, 8))
            }
            val tipIcon = TextView(ctx).apply {
                text = "💡"
                textSize = 14f
                setPadding(0, 0, dp(ctx, 6), 0)
            }
            val tipText = TextView(ctx).apply {
                text = analysis.recommendation
                textSize = 13f
                setTextColor(Color.parseColor("#B8C2CC"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            tipRow.addView(tipIcon)
            tipRow.addView(tipText)
            card.addView(tipRow)
        }

        outerCol.addView(card)
        holder.container.addView(outerCol)
    }

    private fun spacer(ctx: Context, heightDp: Int): View = View(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, heightDp))
    }

    private fun dp(ctx: Context, dp: Int) = (dp * ctx.resources.displayMetrics.density).toInt()

    override fun getItemCount() = messages.size

    private class CircularScoreView(context: Context) : View(context) {
        var progress: Float = 0f
        var scoreColor: Int = Color.RED

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#2A3A5A")
        }
        private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        private val oval = RectF()

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            val stroke = w * 0.13f
            bgPaint.strokeWidth = stroke
            fgPaint.strokeWidth = stroke
            val pad = stroke / 2f + 2f
            oval.set(pad, pad, w - pad, h - pad)
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawArc(oval, 0f, 360f, false, bgPaint)
            fgPaint.color = scoreColor
            canvas.drawArc(oval, -90f, 360f * progress, false, fgPaint)
            textPaint.color = scoreColor
            textPaint.textSize = width * 0.24f
            val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText("${(progress * 100).toInt()}%", width / 2f, textY, textPaint)
        }
    }
}
