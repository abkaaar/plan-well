package com.planwell.app.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.planwell.app.data.local.db.AppDatabaseMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit (Robolectric) coverage for 7→8. Instrumented [com.planwell.app.data.local.Migration7To8Test]
 * under androidTest uses MigrationTestHelper against exported schema JSON when a device is present.
 */
@RunWith(RobolectricTestRunner::class)
class Migration7To8RobolectricTest {

    @Test
    fun migrate7To8_preservesTasksAndCalls_sourceColumnsNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-7-8-robolectric"
        context.deleteDatabase(dbName)

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(7) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createV7Schema(db)
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )

        openHelper.writableDatabase.use { db ->
            db.execSQL(
                """
                INSERT INTO tasks (
                    id, title, description, due_date_ms, priority, category_id,
                    is_completed, completed_at_ms, is_deleted, created_at_ms, updated_at_ms
                ) VALUES (
                    11, 'Keep me', 'notes', NULL, 0, NULL,
                    0, NULL, 0, 1000, 1000
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO scheduled_calls (
                    id, contact_id, display_name, phone_number, scheduled_at_ms,
                    notes, is_completed, created_at_ms, updated_at_ms
                ) VALUES (
                    21, '42', 'Ada', '+15551234567', 2000,
                    'ping', 0, 1000, 1000
                )
                """.trimIndent(),
            )

            AppDatabaseMigrations.MIGRATION_7_8.migrate(db)

            db.query(
                "SELECT title, description, source_type, source_image_path, source_url FROM tasks WHERE id = 11",
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Keep me", c.getString(0))
                assertEquals("notes", c.getString(1))
                assertTrue(c.isNull(2))
                assertTrue(c.isNull(3))
                assertTrue(c.isNull(4))
            }

            db.query(
                """
                SELECT contact_id, display_name, phone_number, scheduled_at_ms, notes
                FROM scheduled_calls WHERE id = 21
                """.trimIndent(),
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("42", c.getString(0))
                assertEquals("Ada", c.getString(1))
                assertEquals("+15551234567", c.getString(2))
                assertEquals(2000L, c.getLong(3))
                assertEquals("ping", c.getString(4))
            }

            db.query(
                "SELECT name FROM sqlite_master WHERE type='index' AND name='index_scheduled_calls_scheduled_at_ms'",
            ).use { c ->
                assertTrue(c.moveToFirst())
            }
        }
    }

    private fun createV7Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `color_hex` TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tasks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `due_date_ms` INTEGER,
                `priority` INTEGER NOT NULL,
                `category_id` INTEGER,
                `is_completed` INTEGER NOT NULL,
                `completed_at_ms` INTEGER,
                `is_deleted` INTEGER NOT NULL,
                `created_at_ms` INTEGER NOT NULL,
                `updated_at_ms` INTEGER NOT NULL,
                FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `scheduled_calls` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `contact_id` TEXT,
                `display_name` TEXT NOT NULL,
                `phone_number` TEXT NOT NULL,
                `scheduled_at_ms` INTEGER NOT NULL,
                `notes` TEXT,
                `is_completed` INTEGER NOT NULL,
                `created_at_ms` INTEGER NOT NULL,
                `updated_at_ms` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_scheduled_calls_scheduled_at_ms` ON `scheduled_calls` (`scheduled_at_ms`)",
        )
    }
}
