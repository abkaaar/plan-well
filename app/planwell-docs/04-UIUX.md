# Plan Well — UI/UX Design Specifications

**Version:** 1.0  
**Design System:** Material Design 3  
**UI Framework:** Jetpack Compose

---

## 1. Logo Concept

### Design Rationale
The logo uses a clipboard/checklist icon with a gradient check-mark — conveying task completion and forward motion. The wordmark is lowercase for approachability and warmth. The gradient shifts blue → green representing "deliberate planning → positive results."

### App Icon Spec

```
Icon canvas : 108 × 108 dp (adaptive icon)
Foreground  : Centered checklist SVG on transparent background
Background  : Gradient fill #2563EB → #10B981 (135°)
Safe zone   : 72 × 72 dp (icon must fit within this)
Corner mask : Applied by launcher (do not bake in)
Play Store  : 512 × 512 px flat PNG (no transparency)
```

### Icon SVG Path (for drawable)

```xml
<!-- ic_launcher_foreground.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M45,30 H35a8,8 0 00-8,8 v44a8,8 0 008,8 h38a8,8 0 008,-8 V38a8,8 0 00-8,-8 H63
                          M45,30 a8,8 0 018,-8 h8a8,8 0 018,8
                          M40,56 l8,8 16,-16"/>
</vector>
```

---

## 2. Colour System

### Light Theme

```kotlin
// Color.kt
val PrimaryBlue    = Color(0xFF2563EB)  // Primary actions, FAB, active states
val PrimaryDark    = Color(0xFF1D4ED8)  // Pressed / darker primary
val AccentGreen    = Color(0xFF10B981)  // Success, completion, "done" states
val WarningAmber   = Color(0xFFF59E0B)  // Medium priority, warnings
val DangerRed      = Color(0xFFEF4444)  // High priority, errors, destructive actions
val TextPrimary    = Color(0xFF0F172A)  // Body text, titles
val TextSecondary  = Color(0xFF64748B)  // Supporting text, descriptions
val TextFaint      = Color(0xFF94A3B8)  // Placeholders, timestamps, disabled
val Background     = Color(0xFFF8FAFC)  // Screen backgrounds
val Surface        = Color(0xFFFFFFFF)  // Cards, sheets, dialogs
val SurfaceVariant = Color(0xFFF1F5F9)  // Input fields, secondary surfaces
val DividerColor   = Color(0xFFE2E8F0)  // Dividers, borders
```

### Dark Theme

```kotlin
val PrimaryBlue_Dark    = Color(0xFF3B82F6)
val AccentGreen_Dark    = Color(0xFF34D399)
val WarningAmber_Dark   = Color(0xFFFBBF24)
val DangerRed_Dark      = Color(0xFFF87171)
val TextPrimary_Dark    = Color(0xFFF1F5F9)
val TextSecondary_Dark  = Color(0xFF94A3B8)
val Background_Dark     = Color(0xFF0F172A)
val Surface_Dark        = Color(0xFF1E293B)
val SurfaceVariant_Dark = Color(0xFF334155)
val DividerColor_Dark   = Color(0xFF334155)
```

### Priority Colour Mapping

| Priority | Light Theme | Dark Theme | Semantic |
|----------|-------------|------------|---------|
| High | `#EF4444` | `#F87171` | Red — urgent |
| Medium | `#F59E0B` | `#FBBF24` | Amber — moderate |
| Low | `#10B981` | `#34D399` | Green — casual |
| None | `#94A3B8` | `#64748B` | Grey — unset |

---

## 3. Typography Scale

```kotlin
// Type.kt
val PlanWellTypography = Typography(
    displayLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,    fontSize = 24.sp, letterSpacing = (-0.5).sp),
    titleLarge    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 22.sp),
    bodyMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,   fontSize = 13.sp, lineHeight = 20.sp),
    labelLarge    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,   fontSize = 12.sp),
    labelSmall    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,   fontSize = 11.sp),
)
```

---

## 4. Spacing & Shape System

```kotlin
// Shapes
val PlanWellShapes = Shapes(
    small  = RoundedCornerShape(6.dp),   // Chips, small elements
    medium = RoundedCornerShape(12.dp),  // Cards
    large  = RoundedCornerShape(16.dp),  // Bottom sheets, dialogs
    extraLarge = RoundedCornerShape(24.dp) // FAB
)

// Spacing scale (use multiples of 4 dp)
// xs   = 4.dp
// sm   = 8.dp
// md   = 16.dp
// lg   = 24.dp
// xl   = 32.dp
// xxl  = 48.dp
```

---

## 5. Component Guidelines

### FAB (Floating Action Button)
- Size: 56 × 56 dp
- Icon: `+` (Add)
- Position: Bottom-right, 16 dp margin from screen edge
- Colour: `PrimaryBlue`
- Action: Opens AddEditTaskScreen

### Task Card

```
┌─────────────────────────────────────────┐
│ ○  Review project proposal          🔴  │
│    Work · Due today, 2:00 PM            │
└─────────────────────────────────────────┘
```

- Left: circular checkbox (tap to complete)
- Right: priority colour dot
- Subtitle: Category name · Due date/time
- Completed state: title gets strikethrough, card background slightly dimmed
- Swipe-right: complete; swipe-left: delete (with undo snackbar)

### Priority Chip

```kotlin
@Composable
fun PriorityChip(priority: Priority) {
    val (label, color) = when (priority) {
        Priority.HIGH   -> "High"   to DangerRed
        Priority.MEDIUM -> "Medium" to WarningAmber
        Priority.LOW    -> "Low"    to AccentGreen
        Priority.NONE   -> "None"   to TextFaint
    }
    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = { Box(Modifier.size(8.dp).background(color, CircleShape)) }
    )
}
```

### Empty State

Every empty list must show:
1. A descriptive SVG illustration (no photography)
2. A headline: e.g. "Nothing planned yet"
3. A subtext: e.g. "Tap + to add your first task"
4. The FAB remains visible

---

## 6. Screen Inventory & Wireframes

### Screen 1: Home / Today View

```
┌─────────────────────────────────┐
│ ☰  Today          🔍  ⋮        │  ← TopAppBar
│    Monday, 29 Sept              │
├─────────────────────────────────┤
│ [All] [Work] [Personal] [Health]│  ← Category filter chips (scrollable)
├─────────────────────────────────┤
│  3 tasks due today              │  ← Section header
│ ┌─────────────────────────────┐ │
│ │ ○ Review proposal      🔴  │ │
│ │   Work · 2:00 PM           │ │
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ ○ Buy groceries        🟡  │ │
│ │   Personal · 5:00 PM        │ │
│ └─────────────────────────────┘ │
│                                 │
│  Upcoming                       │
│ ┌─────────────────────────────┐ │
│ │ ○ Doctor appointment    🔴  │ │
│ │   Health · Tomorrow, 10 AM  │ │
│ └─────────────────────────────┘ │
│                                 │
│                           [+]   │  ← FAB
├─────────────────────────────────┤
│  🏠 Home  📋 Tasks  🔍 Search ⚙ │  ← BottomNavBar
└─────────────────────────────────┘
```

### Screen 2: Add / Edit Task

```
┌─────────────────────────────────┐
│ ←  New Task                     │
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ Task title…                 │ │  ← OutlinedTextField
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ Description (optional)      │ │
│ │                             │ │  ← OutlinedTextField (multiline)
│ └─────────────────────────────┘ │
│                                 │
│  📅  Due date         Not set → │
│  🔔  Reminder         Not set → │
│  🏷  Category         None    → │
│                                 │
│  Priority                       │
│  [ None ] [ Low ] [Medium] [High]│  ← SegmentedButton
│                                 │
│  ┌───────────────────────────┐  │
│  │       Save Task           │  │  ← Primary button (full width)
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

### Screen 3: Task Detail

```
┌─────────────────────────────────┐
│ ←  Task Detail        ✏️  🗑    │
├─────────────────────────────────┤
│  ○  Review project proposal     │
│     🔴 High Priority            │
│                                 │
│  📅  Due: Today, 2:00 PM        │
│  🔔  Reminder: 1:45 PM          │
│  🏷  Category: Work             │
│                                 │
│  Description                    │
│  ─────────────────────────────  │
│  Review the Q4 proposal and     │
│  send feedback to the team by   │
│  end of day.                    │
│                                 │
│  ┌───────────────────────────┐  │
│  │    ✓  Mark as Complete    │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

### Screen 4: Search

```
┌─────────────────────────────────┐
│ ←  🔍  Search tasks…            │
├─────────────────────────────────┤
│  Results for "grocery"  (2)     │
│ ┌─────────────────────────────┐ │
│ │ ○ Buy groceries        🟡  │ │
│ │   Personal · Today 5:00 PM  │ │
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ ○ Grocery list for party 🟢 │ │
│ │   Personal · Friday         │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

### Screen 5: Categories

```
┌─────────────────────────────────┐
│ ←  Categories             +     │
├─────────────────────────────────┤
│  ● Work            (5 tasks)  → │
│  ● Personal        (3 tasks)  → │
│  ● Health          (2 tasks)  → │
│  ● Finance         (1 task)   → │
├─────────────────────────────────┤
│  Tap + to add a new category    │
└─────────────────────────────────┘
```

---

## 7. Motion & Animation Guidelines

| Interaction | Animation | Duration |
|-------------|-----------|----------|
| Task completion | Checkbox fill + strikethrough | 250 ms |
| Screen transition | Shared element (list → detail) | 300 ms |
| FAB tap | Ripple + scale to 0.95 | 100 ms |
| Task swipe complete | Slide-right + green fill | 200 ms |
| Task swipe delete | Slide-left + red fill | 200 ms |
| Bottom sheet open | Slide-up from bottom | 280 ms |
| Snackbar appear | Slide-up from bottom | 200 ms |

**Rule:** No animation should play passively without user action. Respect `Settings > Accessibility > Remove Animations`.

```kotlin
val shouldAnimate = !LocalAccessibilityManager.current.isAnimationEnabled.not()
```

---

## 8. Accessibility Checklist

- [ ] All `Icon` composables have `contentDescription`
- [ ] All `IconButton` composables have `contentDescription`
- [ ] Minimum touch target: `Modifier.minimumInteractiveComponentSize()` applied to all tappable items
- [ ] Colour is never the only means of conveying information (priority also shows as text label)
- [ ] All text passes WCAG AA contrast ratio (4.5:1 for normal, 3:1 for large)
- [ ] TalkBack announcement order matches visual reading order
- [ ] Date pickers support keyboard input as alternative to touch
