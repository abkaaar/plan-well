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
 * Validates additive 5→6 migration keeps opportunity rows and adds extraction columns.
 * Bootstraps v5 by applying 4→5 onto a minimal v4 schema (exportSchema = false).
 */
@RunWith(RobolectricTestRunner::class)
class Migration5To6Test {

    @Test
    fun migrate5To6_preservesOpportunitiesAndAddsExtractionSchema() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-5-6-manual"
        context.deleteDatabase(dbName)

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(5) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createV4Core(db)
                            AppDatabaseMigrations.MIGRATION_4_5.migrate(db)
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
                INSERT INTO opportunities (
                    id, title, description, type, category_id, status, source_url,
                    source_platform, capture_method, is_deleted, created_at_ms, updated_at_ms
                ) VALUES (
                    42, 'Keep me', NULL, 'OPPORTUNITY', NULL, 'ACTIVE', 'https://example.com',
                    'UNKNOWN', 'MANUAL', 0, 1000, 1000
                )
                """.trimIndent(),
            )

            AppDatabaseMigrations.MIGRATION_5_6.migrate(db)

            db.query("SELECT title, extraction_status FROM opportunities WHERE id = 42").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Keep me", cursor.getString(0))
                assertEquals("NONE", cursor.getString(1))
            }

            db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='opportunity_candidates'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }

            db.execSQL(
                """
                INSERT INTO opportunity_candidates (
                    opportunity_id, kind, value_text, source_snippet,
                    is_rejected, is_applied, created_at_ms
                ) VALUES (
                    42, 'TITLE', 'Keep me', NULL, 0, 0, 2000
                )
                """.trimIndent(),
            )
            db.query("SELECT COUNT(*) FROM opportunity_candidates").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    private fun createV4Core(db: SupportSQLiteDatabase) {
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
    }
}
