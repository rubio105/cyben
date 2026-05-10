package eu.cyben.mobile.utils

import eu.cyben.mobile.api.CybenApi
import eu.cyben.mobile.api.CheckBreachesRequest
import eu.cyben.mobile.data.DataBreachInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BreachChecker @Inject constructor(
    private val api: CybenApi
) {
    // Known service names for popular apps
    private val appToServiceMapping = mapOf(
        "com.facebook.katana" to "Facebook",
        "com.facebook.lite" to "Facebook",
        "com.facebook.orca" to "Facebook Messenger",
        "com.instagram.android" to "Instagram",
        "com.whatsapp" to "WhatsApp",
        "com.twitter.android" to "Twitter",
        "com.linkedin.android" to "LinkedIn",
        "com.adobe.reader" to "Adobe",
        "com.adobe.acrobat" to "Adobe",
        "com.dropbox.android" to "Dropbox",
        "com.spotify.music" to "Spotify",
        "com.netflix.mediaclient" to "Netflix",
        "com.amazon.mShop.android.shopping" to "Amazon",
        "com.ebay.mobile" to "eBay",
        "com.paypal.android.p2pmobile" to "PayPal",
        "com.yahoo.mobile.client.android.mail" to "Yahoo",
        "com.microsoft.office.outlook" to "Microsoft",
        "com.google.android.gm" to "Google",
        "com.snapchat.android" to "Snapchat",
        "com.pinterest" to "Pinterest",
        "org.telegram.messenger" to "Telegram",
        "com.tinder" to "Tinder",
        "com.airbnb.android" to "Airbnb",
        "com.ubercab" to "Uber",
        "com.canva.editor" to "Canva",
        "com.duolingo" to "Duolingo",
        "com.discord" to "Discord",
        "com.reddit.frontpage" to "Reddit",
        "com.tumblr" to "Tumblr",
        "com.myfitnesspal.android" to "MyFitnessPal",
        "com.zynga.words3" to "Zynga",
        "com.booking" to "Booking.com"
    )

    suspend fun checkAppsForBreaches(packageNames: List<String>): List<DataBreachInfo> {
        val serviceNames = packageNames.mapNotNull { appToServiceMapping[it] }.distinct()

        if (serviceNames.isEmpty()) return emptyList()

        return try {
            val response = api.checkBreaches(CheckBreachesRequest(serviceNames))
            if (response.isSuccessful) {
                response.body()?.breaches?.map { breach ->
                    DataBreachInfo(
                        serviceName = breach.serviceName,
                        packageName = appToServiceMapping.entries
                            .find { it.value == breach.serviceName }?.key,
                        breachDate = breach.breachDate,
                        recordsAffected = breach.recordsAffected,
                        dataTypes = breach.dataTypes,
                        description = breach.description
                    )
                } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getServiceName(packageName: String): String? {
        return appToServiceMapping[packageName]
    }
}
