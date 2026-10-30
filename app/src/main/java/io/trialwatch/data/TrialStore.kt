package io.trialwatch.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class TrialStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("trialwatch", Context.MODE_PRIVATE)

    fun load(): List<Trial> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Trial(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    url = o.optString("url", ""),
                    price = o.optDouble("price", 0.0),
                    end = o.getLong("end"),
                    done = o.optBoolean("done", false),
                    created = o.optLong("created", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(all: List<Trial>) {
        val arr = JSONArray()
        all.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("url", t.url)
                    put("price", t.price)
                    put("end", t.end)
                    put("done", t.done)
                    put("created", t.created)
                }
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun add(t: Trial) = save(load() + t)

    fun remove(id: String) = save(load().filterNot { it.id == id })

    fun markDone(id: String, done: Boolean = true) =
        save(load().map { if (it.id == id) it.copy(done = done) else it })

    fun active(): List<Trial> = load().filter { !it.done }.sortedBy { it.end }

    private companion object {
        const val KEY = "trials.v1"
    }
}
