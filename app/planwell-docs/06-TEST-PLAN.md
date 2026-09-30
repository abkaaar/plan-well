# Plan Well — Test Plan & QA Documentation

**Version:** 1.0  
**Coverage Target:** ≥ 70% (domain + data layers ≥ 85%)  
**Tools:** JUnit5 · MockK · Turbine · Room In-Memory · Espresso · Compose Test · JaCoCo

---

## 1. Testing Strategy

### Testing Pyramid

```
        ┌──────────┐
        │  UI/E2E  │  10% — Espresso + Compose Test (key flows)
        ├──────────┤
        │Integrat. │  20% — Room in-memory DB, WorkManager
        ├──────────┤
        │  Unit    │  70% — ViewModels, UseCases, Repository, Scheduler
        └──────────┘
```

**Philosophy:**
- Unit tests run in JVM (fast, no emulator needed).
- Integration tests run on emulator or device using Room in-memory database.
- UI tests cover critical user journeys only — not every screen.

---

## 2. Test Configuration

### `build.gradle.kts` (test dependencies)

```kotlin
dependencies {
    // Unit testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    // Android integration testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.work:work-testing:2.9.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// JUnit5 support
tasks.withType<Test> {
    useJUnitPlatform()
}
```

### JaCoCo Coverage Report

```kotlin
// jacoco.gradle.kts
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required = true
        html.required = true
    }
    sourceDirectories.setFrom(files("src/main/java"))
    classDirectories.setFrom(
        fileTree("build/intermediates/javac/debug") {
            exclude("**/R.class", "**/BuildConfig.*", "**/*_Hilt*", "**/*Module*")
        }
    )
    executionData.setFrom(fileTree(buildDir) { include("**/*.exec", "**/*.ec") })
}
```

---

## 3. Unit Tests

### 3.1 UseCase Tests

```kotlin
// CreateTaskUseCaseTest.kt
@ExtendWith(MockKExtension::class)
class CreateTaskUseCaseTest {

    @MockK lateinit var taskRepository: TaskRepository
    private lateinit var useCase: CreateTaskUseCase

    @BeforeEach
    fun setUp() {
        useCase = CreateTaskUseCase(taskRepository)
    }

    @Test
    fun `blank title throws ValidationException`() = runTest {
        assertThrows<ValidationException> {
            useCase(title = "", description = null, dueDateMs = null, priority = Priority.NONE)
        }
    }

    @Test
    fun `title exceeding 100 chars throws ValidationException`() = runTest {
        val longTitle = "A".repeat(101)
        assertThrows<ValidationException> {
            useCase(title = longTitle, description = null, dueDateMs = null, priority = Priority.NONE)
        }
    }

    @Test
    fun `valid task is saved to repository and returns id`() = runTest {
        coEvery { taskRepository.insertTask(any()) } returns 42L

        val id = useCase(title = "Buy milk", description = null, dueDateMs = null, priority = Priority.LOW)

        assertEquals(42L, id)
        coVerify(exactly = 1) { taskRepository.insertTask(match { it.title == "Buy milk" }) }
    }
}
```

### 3.2 ViewModel Tests

```kotlin
// TaskViewModelTest.kt
@ExtendWith(MockKExtension::class)
class TaskViewModelTest {

    @MockK lateinit var getTasksUseCase: GetTasksUseCase
    @MockK lateinit var completeTaskUseCase: CompleteTaskUseCase
    @MockK lateinit var deleteTaskUseCase: DeleteTaskUseCase

    private lateinit var viewModel: TaskViewModel

    @BeforeEach
    fun setUp() {
        val fakeTasks = listOf(
            Task(id = 1L, title = "Task A", priority = Priority.HIGH),
            Task(id = 2L, title = "Task B", priority = Priority.LOW)
        )
        every { getTasksUseCase() } returns flowOf(fakeTasks)
        viewModel = TaskViewModel(getTasksUseCase, completeTaskUseCase, deleteTaskUseCase)
    }

    @Test
    fun `uiState emits tasks on init`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.tasks.size)
            assertEquals("Task A", state.tasks[0].title)
        }
    }

    @Test
    fun `completeTask calls use case with correct id`() = runTest {
        coEvery { completeTaskUseCase(any()) } just Runs

        viewModel.completeTask(1L)

        coVerify { completeTaskUseCase(1L) }
    }
}
```

### 3.3 ReminderScheduler Tests

```kotlin
// ReminderSchedulerTest.kt
@ExtendWith(MockKExtension::class)
class ReminderSchedulerTest {

    @MockK lateinit var context: Context
    @MockK lateinit var alarmManager: AlarmManager
    private lateinit var scheduler: ReminderScheduler

    @BeforeEach
    fun setUp() {
        mockkStatic(PendingIntent::class)
        scheduler = ReminderScheduler(context, alarmManager)
    }

    @Test
    fun `schedule calls setExactAndAllowWhileIdle on API 23+`() {
        val reminder = ReminderEntity(id = 1, taskId = 10L, triggerAtMs = 1_700_000_000_000L)
        every { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) } just Runs
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk()

        scheduler.schedule(reminder)

        verify { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 1_700_000_000_000L, any()) }
    }

    @Test
    fun `cancel calls alarmManager cancel`() {
        every { alarmManager.cancel(any<PendingIntent>()) } just Runs
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk()

        scheduler.cancel(reminderId = 1)

        verify { alarmManager.cancel(any<PendingIntent>()) }
    }
}
```

### 3.4 SearchUseCase Tests

```kotlin
// SearchTasksUseCaseTest.kt
class SearchTasksUseCaseTest {

    @MockK lateinit var taskRepository: TaskRepository
    private lateinit var useCase: SearchTasksUseCase

    @BeforeEach fun setUp() { useCase = SearchTasksUseCase(taskRepository) }

    @Test
    fun `empty query returns empty flow`() = runTest {
        useCase(query = "").test {
            val result = awaitItem()
            assertTrue(result.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `valid query delegates to repository`() = runTest {
        val expected = listOf(Task(id = 1L, title = "Buy groceries"))
        every { taskRepository.search("grocery") } returns flowOf(expected)

        useCase(query = "grocery").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Buy groceries", result[0].title)
        }
    }

    @Test
    fun `special characters in query are handled`() = runTest {
        every { taskRepository.search(any()) } returns flowOf(emptyList())
        assertDoesNotThrow { useCase(query = "test' OR '1'='1").first() }
    }
}
```

---

## 4. Integration Tests

### 4.1 Room DAO Tests

```kotlin
// TaskDaoTest.kt
@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        taskDao = db.taskDao()
    }

    @After fun tearDown() { db.close() }

    @Test
    fun insertTask_andQueryById_returnsCorrectEntity() = runTest {
        val entity = TaskEntity(title = "Test task", priority = 2)
        val id = taskDao.insertTask(entity)
        val retrieved = taskDao.getTaskById(id)
        assertNotNull(retrieved)
        assertEquals("Test task", retrieved!!.title)
        assertEquals(2, retrieved.priority)
    }

    @Test
    fun softDelete_task_doesNotAppearInActiveQuery() = runTest {
        val id = taskDao.insertTask(TaskEntity(title = "Delete me"))
        taskDao.softDelete(id)
        taskDao.getActiveTasks().first().let { tasks ->
            assertTrue(tasks.none { it.id == id })
        }
    }

    @Test
    fun markComplete_setsIsCompletedAndTimestamp() = runTest {
        val id = taskDao.insertTask(TaskEntity(title = "Finish this"))
        taskDao.markComplete(id)
        val task = taskDao.getTaskById(id)
        assertTrue(task!!.isCompleted)
        assertNotNull(task.completedAtMs)
    }

    @Test
    fun deleteCategory_nullifiesTaskCategoryId() = runTest {
        val catId = db.categoryDao().insertCategory(CategoryEntity(name = "Work", colorHex = "#000"))
        taskDao.insertTask(TaskEntity(title = "Work task", categoryId = catId))
        db.categoryDao().deleteCategory(catId)
        val tasks = taskDao.getActiveTasks().first()
        assertTrue(tasks.all { it.categoryId == null })
    }
}
```

### 4.2 FTS Search Integration Test

```kotlin
// TaskFtsDaoTest.kt
@RunWith(AndroidJUnit4::class)
class TaskFtsDaoTest {
    // ... setup same as above

    @Test
    fun ftsSearch_returnsMatchingTask() = runTest {
        taskDao.insertTask(TaskEntity(title = "Buy groceries", description = "From the market"))
        taskDao.insertTask(TaskEntity(title = "Pay electricity bill"))
        taskDao.insertTask(TaskEntity(title = "Doctor appointment"))

        val results = db.taskFtsDao().search("grocery*").first()
        assertEquals(1, results.size)
        assertEquals("Buy groceries", results[0].title)
    }
}
```

### 4.3 Reminder Cascade Delete Test

```kotlin
@Test
fun deleteTask_cascadesDeleteToReminders() = runTest {
    val taskId = taskDao.insertTask(TaskEntity(title = "Task with reminder"))
    db.reminderDao().insertReminder(ReminderEntity(taskId = taskId, triggerAtMs = 9999999L))

    taskDao.permanentlyDelete(TaskEntity(id = taskId, title = "Task with reminder"))

    val reminders = db.reminderDao().getAllActiveReminders()
    assertTrue(reminders.none { it.taskId == taskId })
}
```

---

## 5. UI / Compose Tests

```kotlin
// AddTaskFlowTest.kt
@RunWith(AndroidJUnit4::class)
class AddTaskFlowTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addTask_appearsInList() {
        // Tap FAB
        composeTestRule.onNodeWithContentDescription("Add task").performClick()

        // Fill title
        composeTestRule.onNodeWithText("Task title…").performTextInput("Write test cases")

        // Save
        composeTestRule.onNodeWithText("Save Task").performClick()

        // Verify task appears in home list
        composeTestRule.onNodeWithText("Write test cases").assertIsDisplayed()
    }

    @Test
    fun completeTask_movesToCompletedSection() {
        // Pre-condition: task exists (insert via ViewModel/UseCase)
        // Tap checkbox
        composeTestRule.onNodeWithContentDescription("Complete task: Write test cases").performClick()

        // Verify strikethrough or moved to completed
        composeTestRule.onNodeWithText("Completed (1)").assertIsDisplayed()
    }
}
```

---

## 6. UAT Scenarios (Manual)

| # | Scenario | Steps | Expected Result | Pass / Fail |
|---|----------|-------|-----------------|-------------|
| UAT-01 | Create task with all fields | Open app → FAB → fill title, desc, due date, priority → Save | Task appears in list with correct info | — |
| UAT-02 | Reminder fires on time | Add task with reminder 2 min away → lock screen → wait | Notification appears within 60 sec of trigger time | — |
| UAT-03 | Reminder survives reboot | Set reminder → restart device → wait for trigger time | Notification fires after reboot | — |
| UAT-04 | Mark task complete | Swipe right on task | Task moves to Completed; progress ring updates | — |
| UAT-05 | Delete with undo | Swipe left on task → tap Undo | Task reappears in list | — |
| UAT-06 | Search finds task | Tap 🔍 → type "grocery" | Matching task appears within 1 second | — |
| UAT-07 | Offline operation | Enable Airplane Mode → use full app | All features work with no network | — |
| UAT-08 | Dark mode | Settings → Display → Dark → reopen app | All screens render correctly in dark theme | — |
| UAT-09 | TalkBack | Enable TalkBack → navigate to Add Task → complete | All elements announced; task can be created by voice | — |
| UAT-10 | Performance | Cold start on mid-range device | Home screen visible in < 1.5 seconds | — |

---

## 7. Bug Severity Classification

| Level | Description | SLA |
|-------|-------------|-----|
| **P0 — Blocker** | App crashes, data loss, reminders never fire | Fix before release |
| **P1 — Critical** | Key feature broken (can't create/save tasks) | Fix before release |
| **P2 — Major** | Feature partially broken, workaround exists | Fix in next patch |
| **P3 — Minor** | UI cosmetic issue, edge-case behaviour | Backlog |
