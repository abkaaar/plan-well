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
 * Validates additive 4→5 migration keeps task rows and creates opportunity tables.
 * Bootstraps a v4 SQLite schema manually (project uses exportSchema = false).
 */
@RunWith(RobolectricTestRunner::class)
class Migration4To5Test {

    @Test
    fun migrate4To5_preservesTasksAndCreatesOpportunityTables() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-4-5-manual"

        context.deleteDatabase(dbName)

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(4) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createV4Schema(db)
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
                INSERT INTO categories (id, name, color_hex) VALUES (1, 'Work', '#2563EB')
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO tasks (
                    id, title, description, due_date_ms, priority, category_id,
                    is_completed, completed_at_ms, is_deleted, created_at_ms, updated_at_ms
                ) VALUES (
                    10, 'Keep me', 'desc', NULL, 0, 1,
                    0, NULL, 0, 1000, 1000
                )
                """.trimIndent(),
            )

            AppDatabaseMigrations.MIGRATION_4_5.migrate(db)

            db.query("SELECT title FROM tasks WHERE id = 10").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Keep me", cursor.getString(0))
            }

            db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='opportunities'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
            db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='opportunity_attachments'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
            db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='opportunity_dates'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
            db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='opportunity_task_links'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }

            db.execSQL(
                """
                INSERT INTO opportunities (
                    title, description, type, category_id, status, source_url,
                    source_platform, capture_method, is_deleted, created_at_ms, updated_at_ms
                ) VALUES (
                    'From share', NULL, 'OPPORTUNITY', NULL, 'NEEDS_REVIEW', NULL,
                    'UNKNOWN', 'SHARE', 0, 2000, 2000
                )
                """.trimIndent(),
            )
            db.query("SELECT COUNT(*) FROM opportunities").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    private fun createV4Schema(db: SupportSQLiteDatabase) {
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
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)",
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
            "CREATE INDEX IF NOT EXISTS `index_tasks_category_id` ON `tasks` (`category_id`)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminders` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `task_id` INTEGER NOT NULL,
                `trigger_at_ms` INTEGER NOT NULL,
                `repeat_type` TEXT NOT NULL,
                `repeat_interval_ms` INTEGER,
                `is_active` INTEGER NOT NULL,
                FOREIGN KEY(`task_id`) REFERENCES `tasks`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_reminders_task_id` ON `reminders` (`task_id`)",
        )
        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS `tasks_fts`
            USING FTS4(`title` TEXT, `description` TEXT, content=`tasks`)
            """.trimIndent(),
        )
    }
}
