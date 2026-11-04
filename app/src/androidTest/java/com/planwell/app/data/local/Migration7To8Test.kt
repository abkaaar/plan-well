package com.planwell.app.data.local

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.planwell.app.data.local.db.AppDatabase
import com.planwell.app.data.local.db.AppDatabaseMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented migration coverage for v7 → v8.
 *
 * - [migrate7To8_preservesTasksAndCalls_viaHelper] uses [MigrationTestHelper]
 *   against exported `app/schemas` JSON when kotlinx.serialization is aligned.
 * - [migrate7To8_preservesTasksAndCalls_viaSql] always runs: builds a v7 file,
 *   applies [AppDatabaseMigrations.MIGRATION_7_8], asserts row preservation
 *   and null-safe source_* columns (matches createAll nullability).
 */
@RunWith(AndroidJUnit4::class)
class Migration7To8Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate7To8_preservesTasksAndCalls_viaHelper() {
        val helperUsable = try {
            helper.createDatabase(TEST_DB_PROBE, 7).close()
            true
        } catch (_: AbstractMethodError) {
            false
        } catch (_: Exception) {
            false
        }
        assumeTrue(
            "MigrationTestHelper schema JSON load requires kotlinx-serialization 1.8+",
            helperUsable,
        )

        helper.createDatabase(TEST_DB, 7).use { db ->
            seedRows(db)
        }
        helper.runMigrationsAndValidate(
            TEST_DB,
            8,
            true,
            AppDatabaseMigrations.MIGRATION_7_8,
        ).use { db ->
            assertPreserved(db)
        }
    }

    @Test
    fun migrate7To8_preservesTasksAndCalls_viaSql() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        openV7(context, TEST_DB_SQL).use { db ->
            seedRows(db)
            AppDatabaseMigrations.MIGRATION_7_8.migrate(db)
            db.execSQL("PRAGMA user_version = 8")
            assertPreserved(db)
            assertEquals(
                setOf("source_type", "source_image_path", "source_url"),
                taskSourceColumns(db),
            )
        }
    }

    @Test
    fun migrate7To8_sourceColumnsMatchFreshV8Nullability() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        openV7(context, TEST_DB_NULLABILITY).use { db ->
            AppDatabaseMigrations.MIGRATION_7_8.migrate(db)
            // Fresh createAll uses nullable TEXT with no DEFAULT — migrated cols must be SQL NULL-able.
            db.query("PRAGMA table_info(tasks)").use { c ->
                val nameIdx = c.getColumnIndex("name")
                val notnullIdx = c.getColumnIndex("notnull")
                val dfltIdx = c.getColumnIndex("dflt_value")
                var found = 0
                while (c.moveToNext()) {
                    val name = c.getString(nameIdx)
                    if (name in SOURCE_COLUMNS) {
                        found++
                        assertEquals("column $name must be nullable", 0, c.getInt(notnullIdx))
                        assertTrue("column $name must have no default", c.isNull(dfltIdx))
                    }
                }
                assertEquals(3, found)
            }
        }
    }

    private fun openV7(context: Context, name: String): SupportSQLiteDatabase {
        context.deleteDatabase(name)
        return FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(7) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createV7TasksAndCalls(db)
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        ).writableDatabase
    }

    private fun createV7TasksAndCalls(db: SupportSQLiteDatabase) {
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
                `updated_at_ms` INTEGER NOT NULL
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

    private fun seedRows(db: SupportSQLiteDatabase) {
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
    }

    private fun assertPreserved(db: SupportSQLiteDatabase) {
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
    }

    private fun taskSourceColumns(db: SupportSQLiteDatabase): Set<String> {
        val found = mutableSetOf<String>()
        db.query("PRAGMA table_info(tasks)").use { c ->
            val nameIdx = c.getColumnIndex("name")
            while (c.moveToNext()) {
                val name = c.getString(nameIdx)
                if (name in SOURCE_COLUMNS) found += name
            }
        }
        return found
    }

    companion object {
        private val SOURCE_COLUMNS = setOf("source_type", "source_image_path", "source_url")
        private const val TEST_DB_PROBE = "migration-7-8-probe"
        private const val TEST_DB = "migration-7-8-preserve"
        private const val TEST_DB_SQL = "migration-7-8-sql"
        private const val TEST_DB_NULLABILITY = "migration-7-8-nullability"
    }
}
