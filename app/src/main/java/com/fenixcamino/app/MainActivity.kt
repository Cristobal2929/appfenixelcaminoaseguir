package com.fenixcamino.app

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fenixcamino.app.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val client = OkHttpClient()
    private val quotes = mutableListOf<String>()
    private lateinit var adapter: QuoteAdapter
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = QuoteAdapter(quotes)
        binding.rvQuotes.layoutManager = LinearLayoutManager(this)
        binding.rvQuotes.adapter = adapter

        binding.btnFetch.setOnClickListener {
            fetchRandomQuote()
        }
    }

    private fun fetchRandomQuote() {
        binding.progressBar.isVisible = true
        activityScope.launch {
            val quote = withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url("https://api.quotable.io/random")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@withContext null
                        val body = response.body?.string() ?: return@withContext null
                        val json = JSONObject(body)
                        json.getString("content")
                    }
                } catch (e: Exception) {
                    null
                }
            }

            binding.progressBar.isVisible = false
            if (quote != null) {
                quotes.add(0, quote)
                adapter.notifyItemInserted(0)
                binding.rvQuotes.scrollToPosition(0)
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.error_fetch), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    // RecyclerView Adapter defined inside the same file
    inner class QuoteAdapter(private val items: List<String>) :
        RecyclerView.Adapter<QuoteAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvQuote: TextView = itemView.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvQuote.text = items[position]
            holder.tvQuote.setTextColor(Color.parseColor("#000000"))
        }

        override fun getItemCount(): Int = items.size
    }
}