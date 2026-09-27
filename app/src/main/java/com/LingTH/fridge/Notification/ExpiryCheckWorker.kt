import Databases.ProductData
import Databases.daysUntilExpiry
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.LingTH.fridge.R
import com.LingTH.fridge.sortandfilter.Setting.DefaultSettings
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId


class ExpiryCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("ExpiryWorker", "✅ Worker is running...")

        val db = InventoryDatabase.getDatabase(applicationContext)
        val productDao = db.productDao()
        val settingsDao = db.settingsDao()

        // Fresh installs have no settings row until the user saves Settings once
        val settings = settingsDao.getSettings() ?: DefaultSettings

        val alertDaysList = parseAlertDays(settings.alertBeforeExpiry)
        val alertMode = settings.alertMode // 🆕 ดึงโหมดจาก Settings
        val productList = productDao.getAllProducts().first()

        Log.d("ExpiryWorker", "🧾 Products found: ${productList.size}")
        Log.d("ExpiryWorker", "📢 Alert days list: $alertDaysList")
        Log.d("ExpiryWorker", "🎛️ Alert mode: $alertMode")

        for (product in productList) {
            val daysLeft = product.daysUntilExpiry()
            val expiryDate = product.expiration_date?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            }

            Log.d("ExpiryWorker", "🔍 Checking ${product.product_name}, expiry: $expiryDate, daysLeft: $daysLeft")

            when {
                daysLeft == null -> {
                    Log.w("ExpiryWorker", "⚠️ Skipping ${product.product_name} (no expiration date or failed to parse date)")
                }

                daysLeft < 0 -> {
                    Log.i("ExpiryWorker", "💀 ${product.product_name} has already expired (${kotlin.math.abs(daysLeft)} days ago)")
                    sendNotification(
                        context = applicationContext,
                        product = product,
                        mode = alertMode, // 👈 ส่ง mode จาก settings
                        status = "expired"
                    )
                }

                alertDaysList.contains(daysLeft.toInt()) -> {
                    Log.d("ExpiryWorker", "🔔 Alert: ${product.product_name} will expire in $daysLeft day(s)")
                    sendNotification(
                        context = applicationContext,
                        product = product,
                        mode = alertMode, // 👈 ส่ง mode จาก settings
                        status = "expiry"
                    )
                }

                daysLeft >= 0 -> {
                    Log.d("ExpiryWorker", "🟢 ${product.product_name} is not yet within the alert range ($daysLeft day(s) left)")
                }

                else -> {
                    Log.w("ExpiryWorker", "❓ Unable to process ${product.product_name} (daysLeft = $daysLeft)")
                }
            }
        }

        return Result.success()
    }
}

fun sendNotification(context: Context, product: ProductData, mode: String, status: String) {
    val currentHour = LocalTime.now().hour
    if (currentHour !in 8..23) {
        Log.d("ExpiryWorker", "⏰ Outside notification hours (9–21), skipping notification.")
        return
    }

    val channelId = "expiry_channel_v2"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        val channel = NotificationChannel(channelId, "Expiry Alerts", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

    val daysLeft = product.daysUntilExpiry() ?: return
    val message = buildNotificationMessage(product.product_name, daysLeft.toInt(), status, mode)

    val title = when (status) {
        "expired" -> when (mode) {
            "E-Girlfriend" -> "😢 It's too late..."
            "Aggressive" -> "💀 You messed up!"
            "Friendly" -> "⏰ Just a heads-up!"
            else -> "⚠️ Product Expired!"
        }
        else -> when (mode) {
            "E-Girlfriend" -> "💖 Don't forget!"
            "Aggressive" -> "👊 Get moving!"
            "Friendly" -> "😊 Heads-up!"
            else -> "📦 Product Near Expiry"
        }
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.notification_icon)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    notificationManager.notify(product.id, notification)
}



fun buildNotificationMessage(productName: String, daysLeft: Int, status: String, style: String): String {
    Log.d("ExpiryWorker", "📨 style = $style | status = $status")

    return when (style) {
        "E-Girlfriend" -> when (status) {
            "expired" -> "You forgot again, didn’t you? 😢 $productName expired ${kotlin.math.abs(daysLeft)} day(s) ago!"
            "expiry" -> "Just $daysLeft day(s) before $productName expires! Take care 💖"
            else -> "$productName status unknown, but I’m still thinking of you 💭"
        }

        "Aggressive" -> when (status) {
            "expired" -> "Hey! $productName expired ${kotlin.math.abs(daysLeft)} day(s) ago! Are you blind?"
            "expiry" -> "$productName will expire in $daysLeft day(s) — use it or lose it!"
            else -> "$productName? Figure it out!"
        }

        "Friendly" -> when (status) {
            "expired" -> "Hey, $productName expired ${kotlin.math.abs(daysLeft)} day(s) ago. Just a heads-up!"
            "expiry" -> "Heads-up! $productName will expire in $daysLeft day(s)."
            else -> "$productName status unknown — have a nice day!"
        }

        else -> when (status) {
            "expired" -> "Product: $productName expired ${kotlin.math.abs(daysLeft)} day(s) ago"
            "expiry" -> "Product: $productName will expire in $daysLeft day(s)"
            else -> "Unknown status for: $productName"
        }
    }
}






fun parseAlertDays(input: String): List<Int> {
    Log.d("ExpiryWorker", "🛠 Parsing alert days from input: \"$input\"")

    return input.split(",").mapNotNull { raw ->
        // Old builds seeded Thai values such as "ก่อน 1 วัน"; map them to the English form
        val trimmed = raw.trim().lowercase()
            .removePrefix("ก่อน").trim()
            .replace(Regex("""(\d+)\s*วัน"""), "$1 days")
            .replace(Regex("""(\d+)\s*สัปดาห์"""), "$1 weeks")
            .replace(Regex("""(\d+)\s*เดือน"""), "$1 months")

        when {
            trimmed.matches(Regex("""\d+ day[s]?""")) -> {
                trimmed.split(" ")[0].toIntOrNull()
            }
            trimmed.matches(Regex("""\d+ week[s]?""")) -> {
                trimmed.split(" ")[0].toIntOrNull()?.times(7)
            }
            trimmed.matches(Regex("""\d+ month[s]?""")) -> {
                trimmed.split(" ")[0].toIntOrNull()?.times(30)
            }
            else -> {
                Log.w("ExpiryWorker", "⚠️ Unrecognized alert format: \"$trimmed\"")
                null
            }
        }
    }
}



