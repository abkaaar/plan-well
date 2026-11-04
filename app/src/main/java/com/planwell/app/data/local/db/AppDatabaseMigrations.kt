package com.planwell.app.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {

    /**
     * Additive v4 → v5: opportunity tables + task link junction.
     * Existing tasks / categories / reminders / FTS are untouched.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `opportunities` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `type` TEXT NOT NULL,
                    `category_id` INTEGER,
                    `status` TEXT NOT NULL,
                    `source_url` TEXT,
                    `source_platform` TEXT NOT NULL,
                    `capture_method` TEXT NOT NULL,
                    `is_deleted` INTEGER NOT NULL,
                    `created_at_ms` INTEGER NOT NULL,
                    `updated_at_ms` INTEGER NOT NULL,
                    FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_opportunities_category_id` ON `opportunities` (`category_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_opportunities_status` ON `opportunities` (`status`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_opportunities_is_deleted` ON `opportunities` (`is_deleted`)",
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `opportunity_attachments` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `opportunity_id` INTEGER NOT NULL,
                    `relative_path` TEXT NOT NULL,
                    `mime_type` TEXT,
                    `display_name` TEXT,
                    `kind` TEXT NOT NULL,
                    `created_at_ms` INTEGER NOT NULL,
                    FOREIGN KEY(`opportunity_id`) REFERENCES `opportunities`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_opportunity_attachments_opportunity_id` ON `opportunity_attachments` (`opportunity_id`)",
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `opportunity_dates` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `opportunity_id` INTEGER NOT NULL,
                    `type` TEXT NOT NULL,
                    `label` TEXT,
                    `date_ms` INTEGER,
                    `extracted_date_ms` INTEGER,
                    `is_user_confirmed` INTEGER NOT NULL,
                    FOREIGN KEY(`opportunity_id`) REFERENCES `opportunities`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_opportunity_dates_opportunity_id` ON `opportunity_dates` (`opportunity_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_opportunity_dates_type` ON `opportunity_dates` (`type`)",
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `opportunity_task_links` (
                    `opportunity_id` INTEGER NOT NULL,
                    `task_id` INTEGER NOT NULL,
                    PRIMARY KEY(`opportunity_id`, `task_id`),
                    FOREIGN KEY(`opportunity_id`) REFERENCES `opportunities`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`task_id`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_opportunity_task_links_opportunity_id` ON `opportunity_task_links` (`opportunity_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_opportunity_task_links_task_id` ON `opportunity_task_links` (`task_id`)",
            )
        }
    }

    /**
     * Additive v5 → v6: extraction provenance columns + non-date candidates table.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `opportunities` ADD COLUMN `extraction_status` TEXT NOT NULL DEFAULT 'NONE'",
            )
            db.execSQL(
                "ALTER TABLE `opportunities` ADD COLUMN `extraction_error` TEXT",
            )
            db.execSQL(
                "ALTER TABLE `opportunities` ADD COLUMN `extracted_raw_text` TEXT",
            )
            db.execSQL(
                "ALTER TABLE `opportunities` ADD COLUMN `last_extraction_at_ms` INTEGER",
            )
            db.execSQL(
                "ALTER TABLE `opportunities` ADD COLUMN `extraction_engine` TEXT",
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `opportunity_candidates` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `opportunity_id` INTEGER NOT NULL,
                    `kind` TEXT NOT NULL,
                    `value_text` TEXT NOT NULL,
                    `source_snippet` TEXT,
                    `is_rejected` INTEGER NOT NULL,
                    `is_applied` INTEGER NOT NULL,
                    `created_at_ms` INTEGER NOT NULL,
                    FOREIGN KEY(`opportunity_id`) REFERENCES `opportunities`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_opportunity_candidates_opportunity_id` ON `opportunity_candidates` (`opportunity_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_opportunity_candidates_kind` ON `opportunity_candidates` (`kind`)",
            )
        }
    }

    /**
     * Additive v6 → v7: scheduled calls table for the call scheduler.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
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

    /**
     * Additive v7 → v8: task import provenance columns.
     * scheduled_calls index already created in [MIGRATION_6_7] — do not recreate.
     */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `source_type` TEXT")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `source_image_path` TEXT")
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `source_url` TEXT")
        }
    }
}
