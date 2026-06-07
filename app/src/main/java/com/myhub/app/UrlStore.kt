package com.myhub.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 사용자가 설정한 사이트 한 개. */
data class SiteEntry(
    val id: String,
    val label: String,
    val url: String,
    val color: String
)

/**
 * URL 목록을 SharedPreferences 에 JSON 으로 저장/로드한다.
 * 설정 화면(WebView)과 메인 화면(네이티브)이 같은 저장소를 공유한다.
 */
object UrlStore {
    private const val PREF = "myhub_prefs"
    private const val KEY_SITES = "sites"
    private const val KEY_LAST = "last_page"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun loadJson(ctx: Context): String =
        prefs(ctx).getString(KEY_SITES, "[]") ?: "[]"

    fun saveJson(ctx: Context, json: String) {
        // 유효성 가벼운 검증 후 저장 (깨진 JSON 으로 메인이 죽지 않도록)
        val normalized = try {
            JSONArray(json).toString()
        } catch (e: Exception) {
            "[]"
        }
        prefs(ctx).edit().putString(KEY_SITES, normalized).apply()
    }

    fun load(ctx: Context): List<SiteEntry> {
        val out = ArrayList<SiteEntry>()
        try {
            val arr = JSONArray(loadJson(ctx))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val url = o.optString("url").trim()
                if (url.isEmpty()) continue
                out.add(
                    SiteEntry(
                        id = o.optString("id", url.hashCode().toString()),
                        label = o.optString("label").ifBlank { hostOf(url) },
                        url = normalizeUrl(url),
                        color = o.optString("color", "#2DD4BF")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return out
    }

    fun lastPage(ctx: Context): Int = prefs(ctx).getInt(KEY_LAST, 0)
    fun setLastPage(ctx: Context, idx: Int) =
        prefs(ctx).edit().putInt(KEY_LAST, idx).apply()

    fun normalizeUrl(raw: String): String {
        val u = raw.trim()
        if (u.isEmpty()) return u
        return if (u.startsWith("http://") || u.startsWith("https://")) u
        else "https://$u"
    }

    fun hostOf(raw: String): String {
        return try {
            val u = normalizeUrl(raw)
            val noScheme = u.substringAfter("://")
            noScheme.substringBefore("/").substringBefore("?")
        } catch (_: Exception) {
            raw
        }
    }
}
