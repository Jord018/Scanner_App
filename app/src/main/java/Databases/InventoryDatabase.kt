import Databases.InspectionData
import Databases.ProductDao
import Databases.ProductData
import com.LingTH.fridge.sortandfilter.Setting.Settings
import com.LingTH.fridge.sortandfilter.Setting.SettingsDao
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.LingTH.fridge.migration.MIGRATION_1_2
import com.LingTH.fridge.migration.MIGRATION_2_3

@Database(entities = [ProductData::class, InspectionData::class, Settings::class], version = 3)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: InventoryDatabase? = null

        fun getDatabase(context: Context): InventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InventoryDatabase::class.java,
                    "inventory_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
