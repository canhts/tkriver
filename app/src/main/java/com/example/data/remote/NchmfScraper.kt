package com.example.data.remote

import com.example.data.local.NewsArticleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NchmfScraper {

    suspend fun scrapeHydrologicalBulletins(
        targetCount: Int = MAX_BULLETIN_COUNT,
        maxPages: Int = 6
    ): List<NewsArticleEntity> = withContext(Dispatchers.IO) {
        val collected = LinkedHashMap<String, NewsArticleEntity>(targetCount)
        val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", LOCALE_VI).apply {
            isLenient = true
        }
        val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", LOCALE_VI).apply {
            isLenient = true
        }

        for (pageIndex in 1..maxPages) {
            if (collected.size >= targetCount) break

            val pageUrl = if (pageIndex == 1) {
                BASE_LIST_URL
            } else {
                "$BASE_LIST_URL?pageindex=$pageIndex"
            }

            val document = Jsoup.connect(pageUrl)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get()

            val linkElements = document.select(LIST_ITEM_SELECTOR)
            if (linkElements.isEmpty()) break

            for (aElement in linkElements) {
                val rawSpanText = aElement.selectFirst("span")?.text().orEmpty()
                val pubDateStr = rawSpanText
                    .replace("(", "")
                    .replace(")", "")
                    .trim()

                val ownTitle = aElement.ownText().trim()
                val title = if (ownTitle.isNotEmpty()) {
                    ownTitle
                } else {
                    aElement.text().replace(rawSpanText, "").trim()
                }

                val articleUrl = aElement.attr("abs:href").trim()
                if (title.isEmpty() || articleUrl.isEmpty() || pubDateStr.isEmpty()) {
                    continue
                }

                if (collected.containsKey(articleUrl)) {
                    continue
                }

                val parsedDate: Date = try {
                    dateTimeFormat.parse(pubDateStr)
                        ?: dateOnlyFormat.parse(pubDateStr.substringBefore(" "))
                        ?: continue
                } catch (e: Exception) {
                    continue
                }

                val articleTimestamp = parsedDate.time
                val isHighlight = isTraKhucOrQuangNgai(title)

                collected[articleUrl] = NewsArticleEntity(
                    url = articleUrl,
                    title = title,
                    pubDateStr = pubDateStr,
                    timestampMillis = articleTimestamp,
                    isHighlight = isHighlight,
                    cachedContent = null
                )

                if (collected.size >= targetCount) {
                    break
                }
            }
        }

        // Sắp xếp theo thứ tự thời gian từ mới nhất đếm xuống và lấy đúng 30 bài viết
        collected.values
            .sortedByDescending { it.timestampMillis }
            .take(targetCount)
    }

    suspend fun scrapeArticleDetailContent(articleUrl: String): String = withContext(Dispatchers.IO) {
        val document = Jsoup.connect(articleUrl)
            .userAgent(USER_AGENT)
            .timeout(TIMEOUT_MS)
            .get()

        val contentNews = document.selectFirst("div.content-news")
            ?: throw IllegalStateException("Không tìm thấy khối nội dung bài viết (div.content-news).")

        // Loại bỏ hoàn toàn hình ảnh và mã rác ngay trên phần tử div.content-news
        contentNews.select("img, script, style, #modalShowImage, iframe, figure").remove()

        // Xóa tiêu đề h2.tt-content-news khỏi phần thân để không bị lặp lại với tiêu đề trên Header của BottomSheet
        contentNews.select("h2.tt-content-news").remove()

        val bodyContainer = contentNews.selectFirst("div.text-content-news") ?: contentNews

        // Xóa mọi thuộc tính style inline gây vỡ khung (như width: 835px)
        bodyContainer.select("[style], [width], [height]").forEach { el ->
            el.removeAttr("style")
            el.removeAttr("width")
            el.removeAttr("height")
        }

        val paragraphs = stripTrailingChiTietSection(extractCleanParagraphs(bodyContainer))
        if (paragraphs.isNotEmpty()) {
            paragraphs.joinToString("\n\n")
        } else {
            val fallbackLines = bodyContainer.wholeText()
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()
            stripTrailingChiTietSection(fallbackLines).joinToString("\n\n")
        }
    }

    private fun extractCleanParagraphs(root: Element): List<String> {
        val result = ArrayList<String>()
        val blockSelectors = "h1, h2, h3, h4, p, li, tr, div"
        val elements = root.select(blockSelectors)

        if (elements.isEmpty()) {
            val plain = root.text().trim()
            if (plain.isNotEmpty()) result.add(plain)
            return result
        }

        for (el in elements) {
            val tag = el.tagName().lowercase(Locale.ROOT)
            // Nếu là thẻ div hoặc tr chứa các thẻ khối con bên trong thì bỏ qua thẻ cha để tránh lặp chữ
            if (tag == "div" && el.select("h1, h2, h3, h4, p, li, table, tr, div").size > 1) {
                continue
            }
            if (tag == "tr") {
                val rowCells = el.select("th, td")
                    .map { it.text().trim() }
                    .filter { it.isNotEmpty() }
                if (rowCells.isNotEmpty()) {
                    val rowLine = rowCells.joinToString(" • ")
                    if (result.lastOrNull() != rowLine) {
                        result.add(rowLine)
                    }
                }
                continue
            }

            // Giữ các ngắt dòng <br> bên trong đoạn văn nếu có
            el.select("br").append("\\n")
            val rawText = el.text().replace("\\n", "\n")
            rawText.lineSequence()
                .map { it.replace(WHITESPACE_REGEX, " ").trim() }
                .filter { it.isNotEmpty() }
                .forEach { cleanLine ->
                    // Tránh lặp lại dòng vừa thêm
                    if (result.lastOrNull() != cleanLine) {
                        result.add(cleanLine)
                    }
                }
        }

        return result
    }

    companion object {
        const val MAX_BULLETIN_COUNT = 30
        const val BASE_LIST_URL = "https://www.nchmf.gov.vn/kttv/vi-VN/1/thuy-van-nguy-hiem-15-18.html"
        const val LIST_ITEM_SELECTOR = "div.grp-list-item ul.uk-list li div.uk-width-expand a"
        private const val TIMEOUT_MS = 15_000
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        val LOCALE_VI: Locale = Locale("vi", "VN")
        private val WHITESPACE_REGEX = Regex("\\s+")

        fun stripTrailingChiTietSection(lines: List<String>): List<String> {
            return lines.takeWhile { line ->
                val lower = line.lowercase(LOCALE_VI).trim()
                !lower.contains("chi tiết tin") &&
                    !lower.contains("chi tiết trong bản tin đính kèm") &&
                    !lower.contains("chi tiết tại bản tin đính kèm") &&
                    !lower.startsWith("chi tiết trong bản tin")
            }
        }

        fun isTraKhucOrQuangNgai(text: String): Boolean {
            val upper = text.uppercase(LOCALE_VI)
            return upper.contains("TRÀ KHÚC") ||
                upper.contains("TRA KHUC") ||
                upper.contains("QUẢNG NGÃI") ||
                upper.contains("QUANG NGAI") ||
                upper.contains("SÔNG VỆ") ||
                upper.contains("SONG VE")
        }

        fun normalizeToDayStartMillis(timestampMillis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestampMillis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun subtractOneDayMillis(dayStartMillis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = dayStartMillis
            cal.add(Calendar.DAY_OF_YEAR, -1)
            return cal.timeInMillis
        }
    }
}
