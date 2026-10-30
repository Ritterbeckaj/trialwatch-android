package io.trialwatch.data

import java.net.URLEncoder

/**
 * The one thing that makes "cancel before the charge" actually possible:
 * landing on the cancel page, not the marketing homepage.
 */
object CancelUrls {

    private val table: List<Pair<String, String>> = listOf(
        "netflix" to "https://www.netflix.com/cancelplan",
        "spotify" to "https://www.spotify.com/account/subscription/",
        "disneyplus" to "https://www.disneyplus.com/account",
        "hulu" to "https://secure.hulu.com/account/cancel",
        "max" to "https://auth.max.com/subscription",
        "primevideo" to "https://www.amazon.com/gp/video/settings",
        "amazonprime" to "https://www.amazon.com/mc",
        "audible" to "https://www.audible.com/account/overview",
        "kindleunlimited" to "https://www.amazon.com/kindleunlimited",
        "apple" to "https://apps.apple.com/account/subscriptions",
        "icloud" to "https://www.icloud.com/settings/",
        "googleplay" to "https://play.google.com/store/account/subscriptions",
        "youtubepremium" to "https://www.youtube.com/paid_memberships",
        "adobe" to "https://account.adobe.com/plans",
        "microsoft365" to "https://account.microsoft.com/services",
        "xbox" to "https://account.microsoft.com/services",
        "playstation" to "https://www.playstation.com/acct/subscriptions",
        "nintendo" to "https://accounts.nintendo.com/",
        "dropbox" to "https://www.dropbox.com/account/plan",
        "googleone" to "https://one.google.com/settings",
        "notion" to "https://www.notion.so/profile/billing",
        "chatgpt" to "https://chatgpt.com/#settings/Subscription",
        "claude" to "https://claude.ai/settings/billing",
        "perplexity" to "https://www.perplexity.ai/settings/account",
        "midjourney" to "https://www.midjourney.com/account",
        "githubcopilot" to "https://github.com/settings/copilot",
        "cursor" to "https://www.cursor.com/settings",
        "canva" to "https://www.canva.com/settings/billing",
        "figma" to "https://www.figma.com/settings",
        "framer" to "https://www.framer.com/account/billing",
        "grammarly" to "https://account.grammarly.com/subscription",
        "linkedin" to "https://www.linkedin.com/premium/manage/",
        "duolingo" to "https://www.duolingo.com/settings/subscription",
        "headspace" to "https://www.headspace.com/subscriptions",
        "calm" to "https://www.calm.com/account",
        "peloton" to "https://www.onepeloton.com/membership",
        "strava" to "https://www.strava.com/settings/subscription",
        "nytimes" to "https://www.nytimes.com/account/settings/subscription",
        "wsj" to "https://account.wsj.com/",
        "paramount" to "https://www.paramountplus.com/account/",
        "peacock" to "https://www.peacocktv.com/account/subscription",
        "crunchyroll" to "https://www.crunchyroll.com/account",
        "twitch" to "https://www.twitch.tv/subscriptions",
        "substack" to "https://substack.com/settings",
        "tinder" to "https://tinder.com/app/settings/subscription",
        "bumble" to "https://bumble.com/get-premium",
        "masterclass" to "https://www.masterclass.com/account",
        "blinkist" to "https://www.blinkist.com/en/settings/account",
        "everand" to "https://www.everand.com/account",
        "nordvpn" to "https://my.nordaccount.com/dashboard/",
        "onepassword" to "https://my.1password.com/",
        "premium" to "https://x.com/settings/premium"
    )

    fun resolve(name: String): String {
        val q = name.lowercase().filter { it.isLetterOrDigit() }
        var best: String? = null
        var score = 0
        if (q.isNotEmpty()) {
            for ((label, url) in table) {
                val n = label.filter { it.isLetterOrDigit() }
                val s = when {
                    n == q -> 100
                    n.startsWith(q) || q.startsWith(n) -> 80
                    n.contains(q) || q.contains(n) -> 40
                    else -> 0
                }
                if (s > score) {
                    score = s
                    best = url
                }
            }
        }
        return best ?: ("https://www.google.com/search?q=" +
            URLEncoder.encode("cancel $name subscription", "UTF-8"))
    }
}
