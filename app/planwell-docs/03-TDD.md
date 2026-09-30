# Plan Well — Technical Design Document (TDD)

**Version:** 1.0  
**Architecture:** MVVM + Clean Architecture  
**Database:** Room (SQLite + FTS5)  
**Reminders:** AlarmManager + WorkManager

---

## 1. Architecture Overview

**Pattern:** MVVM (Model-View-ViewModel) with Clean Architecture layers.

```
┌─────────────────────────────────────────────┐
│                  UI Layer                   │
│  Compose Screens → ViewModels → UI State    │
└──────────────────────┬──────────────────────┘
                       │ calls
┌──────────────────────▼──────────────────────┐
│               Domain Layer                  │
│     UseCases / Interactors + Models         │
└──────────────────────┬──────────────────────┘
                       │ calls
┌──────────────────────▼──────────────────────┐
│                Data Layer                   │
│   Repository Impl → DAO → Room (SQLite)     │
└─────────────────────────────────────────────┘
```

Single-Activity + Jetpack Navigation Component for all screen transitions.

---

## 2. Project Structure

```
app/src/main/java/com/planwell/app/
│
├── ui/
│   ├── MainActivity.kt               ← Single activity host
│   ├── NavGraph.kt                   ← Navigation compose graph
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── task/
│   │   ├── TaskListScreen.kt
│   │   ├── TaskDetailScreen.kt
│   │   ├── AddEditTaskScreen.kt
│   │   └── TaskViewModel.kt
│   ├── reminder/
│   │   ├── ReminderPickerSheet.kt
│   │   └── ReminderViewModel.kt
│   ├── category/
│   │   ├── CategoryScreen.kt
│   │   └── CategoryViewModel.kt
│   ├── search/
│   │   ├── SearchScreen.kt
│   │   └── SearchViewModel.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── domain/
│   ├── model/
│   │   ├── Task.kt
│   │   ├── Reminder.kt
│   │   └── Category.kt
│   ├── repository/
│   │   ├── TaskRepository.kt         ← Interface
│   │   ├── ReminderRepository.kt     ← Interface
│   │   └── CategoryRepository.kt    ← Interface
│   └── usecase/
│       ├── task/
│       │   ├── CreateTaskUseCase.kt
│       │   ├── UpdateTaskUseCase.kt
│       │   ├── DeleteTaskUseCase.kt
│       │   ├── CompleteTaskUseCase.kt
│       │   └── GetTasksUseCase.kt
│       ├── reminder/
│       │   ├── ScheduleReminderUseCase.kt
│       │   └── CancelReminderUseCase.kt
│       └── search/
│           └── SearchTasksUseCase.kt
│
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt        ← Room database
│   │   │   ├── entity/
│   │   │   │   ├── TaskEntity.kt
│   │   │   │   ├── ReminderEntity.kt
│   │   │   │   └── CategoryEntity.kt
│   │   │   └── migration/
│   │   │       └── Migrations.kt
│   │   └── dao/
│   │       ├── TaskDao.kt
│   │       ├── TaskFtsDao.kt
│   │       ├── ReminderDao.kt
│   │       └── CategoryDao.kt
│   ├── repository/
│   │   ├── TaskRepositoryImpl.kt
│   │   ├── ReminderRepositoryImpl.kt
│   │   └── CategoryRepositoryImpl.kt
│   └── alarm/
│       ├── ReminderScheduler.kt
│       ├── ReminderReceiver.kt       ← BroadcastReceiver
│       └── BootReceiver.kt           ← BroadcastReceiver (reboot)
│
└── di/
    ├── DatabaseModule.kt
    ├── RepositoryModule.kt
    └── AlarmModule.kt
```

---

## 3. Technology Stack

| Layer | Technology | Version | Reason |
|-------|-----------|---------|--------|
| Language | Kotlin | 2.0 | Null-safety, coroutines, idiomatic Android |
| UI | Jetpack Compose | 1.6.x | Declarative UI, less boilerplate than XML |
| Navigation | Navigation Compose | 2.7.x | Type-safe single-activity navigation |
| Database | Room | 2.6.x | SQLite abstraction, FTS5 support, coroutine integration |
| DI | Hilt | 2.51 | Compile-time DI, Android lifecycle aware |
| Reminders | AlarmManager + WorkManager | — | AlarmManager for exact alarms; WorkManager for recurring/boot-recovery |
| Async | Kotlin Coroutines + StateFlow | 1.8.x | Structured concurrency; reactive streams |
| Serialisation | Kotlinx Serialization | 1.6.x | JSON export without reflection |
| Testing (unit) | JUnit5 + MockK + Turbine | latest | Concise ViewModel/UseCase testing; Flow test support |
| Testing (UI) | Espresso + Compose Test | latest | End-to-end user flow testing |
| Coverage | JaCoCo | latest | Code coverage reporting |

---

## 4. Database Schema

### 4.1 `tasks` Table

```sql
CREATE TABLE tasks (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    title           TEXT    NOT NULL,                   -- max 100 chars
    description     TEXT,                               -- nullable, max 500 chars
    due_date_ms     INTEGER,                            -- nullable, epoch millis
    priority        INTEGER NOT NULL DEFAULT 0,         -- 0=None, 1=Low, 2=Medium, 3=High
    category_id     INTEGER REFERENCES categories(id),  -- nullable FK
    is_completed    INTEGER NOT NULL DEFAULT 0,         -- 0 or 1
    completed_at_ms INTEGER,                            -- nullable, epoch millis
    is_deleted      INTEGER NOT NULL DEFAULT 0,         -- soft delete flag
    created_at_ms   INTEGER NOT NULL,
    updated_at_ms   INTEGER NOT NULL
);

-- FTS5 virtual table for full-text search
CREATE VIRTUAL TABLE tasks_fts USING fts5(
    title,
    description,
    content='tasks',
    content_rowid='id'
);
```

### 4.2 `reminders` Table

```sql
CREATE TABLE reminders (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id             INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    trigger_at_ms       INTEGER NOT NULL,           -- epoch millis
    repeat_type         TEXT    NOT NULL DEFAULT 'NONE', -- NONE | DAILY | WEEKLY | CUSTOM
    repeat_interval_ms  INTEGER,                    -- nullable, used for CUSTOM type
    is_active           INTEGER NOT NULL DEFAULT 1  -- 1 = scheduled, 0 = cancelled
);
```

### 4.3 `categories` Table

```sql
CREATE TABLE categories (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL UNIQUE,
    color_hex   TEXT    NOT NULL DEFAULT '#2563EB'
);
```

---

## 5. Room Entity Definitions (Kotlin)

```kotlin
// TaskEntity.kt
@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["category_id"],
        onDelete = ForeignKey.SET_NULL
    )]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    @ColumnInfo(name = "due_date_ms") val dueDateMs: Long? = null,
    val priority: Int = 0,
    @ColumnInfo(name = "category_id", index = true) val categoryId: Long? = null,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "completed_at_ms") val completedAtMs: Long? = null,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis()
)

// ReminderEntity.kt
@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["task_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id", index = true) val taskId: Long,
    @ColumnInfo(name = "trigger_at_ms") val triggerAtMs: Long,
    @ColumnInfo(name = "repeat_type") val repeatType: String = "NONE",
    @ColumnInfo(name = "repeat_interval_ms") val repeatIntervalMs: Long? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)

// CategoryEntity.kt
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "color_hex") val colorHex: String = "#2563EB"
)
```

---

## 6. DAO Definitions

```kotlin
// TaskDao.kt
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE is_deleted = 0 ORDER BY due_date_ms ASC, priority DESC")
    fun getActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("""
        SELECT * FROM tasks 
        WHERE is_deleted = 0 
          AND due_date_ms BETWEEN :startMs AND :endMs
        ORDER BY due_date_ms ASC
    """)
    fun getTasksForToday(startMs: Long, endMs: Long): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET is_deleted = 1, updated_at_ms = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun permanentlyDelete(task: TaskEntity)

    @Query("UPDATE tasks SET is_completed = 1, completed_at_ms = :time WHERE id = :id")
    suspend fun markComplete(id: Long, time: Long = System.currentTimeMillis())
}

// TaskFtsDao.kt
@Dao
interface TaskFtsDao {
    @Query("SELECT * FROM tasks WHERE id IN (SELECT rowid FROM tasks_fts WHERE tasks_fts MATCH :query)")
    fun search(query: String): Flow<List<TaskEntity>>
}

// ReminderDao.kt
@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE task_id = :taskId AND is_active = 1")
    suspend fun getActiveRemindersForTask(taskId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE is_active = 1")
    suspend fun getAllActiveReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Query("UPDATE reminders SET is_active = 0 WHERE id = :id")
    suspend fun cancelReminder(id: Long)

    @Query("UPDATE reminders SET is_active = 0 WHERE task_id = :taskId")
    suspend fun cancelAllRemindersForTask(taskId: Long)
}
```

---

## 7. AppDatabase Setup

```kotlin
// AppDatabase.kt
@Database(
    entities = [TaskEntity::class, ReminderEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskFtsDao(): TaskFtsDao
    abstract fun reminderDao(): ReminderDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DB_NAME = "plan_well_db"
    }
}

// DatabaseModule.kt (Hilt)
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DB_NAME)
            .addMigrations(/* add future migrations here */)
            .build()

    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
    @Provides fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
}
```

---

## 8. Reminder Implementation

### 8.1 ReminderScheduler

```kotlin
// ReminderScheduler.kt
class ReminderScheduler @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager
) {
    fun schedule(reminder: ReminderEntity) {
        val intent = buildPendingIntent(reminder.id.toInt(), reminder.taskId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Fallback to inexact alarm
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtMs,
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES / 4, // 3.75 min window
                    intent
                )
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, reminder.triggerAtMs, intent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminder.triggerAtMs, intent)
        }
    }

    fun cancel(reminderId: Int) {
        alarmManager.cancel(buildPendingIntent(reminderId, taskId = 0))
    }

    private fun buildPendingIntent(reminderId: Int, taskId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            reminderId,
            Intent(context, ReminderReceiver::class.java).apply {
                putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId.toLong())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
```

### 8.2 ReminderReceiver

```kotlin
// ReminderReceiver.kt
class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (taskId == -1L) return

        // Show notification
        NotificationHelper(context).showTaskReminder(taskId)

        // Re-schedule if recurring (handled by RescheduleWorker via WorkManager)
        val workRequest = OneTimeWorkRequestBuilder<RescheduleReminderWorker>()
            .setInputData(workDataOf("reminder_id" to reminderId))
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
```

### 8.3 BootReceiver

```kotlin
// BootReceiver.kt
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Enqueue a WorkManager job to re-schedule all active reminders from DB
        val workRequest = OneTimeWorkRequestBuilder<RestoreRemindersWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
```

---

## 9. ViewModel Pattern

```kotlin
// TaskViewModel.kt
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTasksUseCase().collect { tasks ->
                _uiState.update { it.copy(tasks = tasks, isLoading = false) }
            }
        }
    }

    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            completeTaskUseCase(taskId)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            deleteTaskUseCase(taskId)
        }
    }
}

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
```

---

## 10. Dependency Graph Summary

```
MainActivity
  └── NavHost (Navigation Compose)
       ├── HomeScreen ← HomeViewModel ← GetTasksUseCase ← TaskRepositoryImpl ← TaskDao ← AppDatabase
       ├── AddEditTaskScreen ← TaskViewModel ← CreateTaskUseCase / UpdateTaskUseCase
       │                                     └── ScheduleReminderUseCase ← ReminderScheduler (AlarmManager)
       ├── SearchScreen ← SearchViewModel ← SearchTasksUseCase ← TaskFtsDao
       └── CategoryScreen ← CategoryViewModel ← CategoryRepositoryImpl ← CategoryDao
```

All dependencies injected by Hilt at compile time.
