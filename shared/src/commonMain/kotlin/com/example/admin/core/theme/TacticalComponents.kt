package com.example.admin.core.theme
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.admin.core.icons.*
import com.example.admin.core.model.MutationResult

@Composable fun OpsPage(content: LazyListScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 1100.dp).fillMaxSize(), contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp), content = content)
    }
}
val LocalPageBack = staticCompositionLocalOf<(() -> Unit)?> { null }
@Composable fun GlassIconButton(symbol: OpsSymbol, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    IconButton(onClick, modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).liquidGlass(AdminColors.Slate, RoundedCornerShape(12.dp)), enabled = enabled) {
        OpsIcon(symbol, Modifier.size(22.dp), tint = if (enabled) AdminColors.Navy else AdminColors.Slate.copy(alpha = .45f), contentDescription = label)
    }
}
@Composable fun PageTitle(title: String, subtitle: String, eyebrow: String? = null, action: (@Composable () -> Unit)? = null) {
    val back = LocalPageBack.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            back?.let { GlassIconButton(OpsSymbol.Back, "Back", it) }
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
            action?.invoke()
        }
        Text(subtitle, color = AdminColors.Slate)
    }
}
@Composable fun OpsCard(modifier: Modifier = Modifier, color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = color, border = BorderStroke(1.dp, AdminColors.BorderLight)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable fun SectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AdminColors.Slate)
    }
}
@Composable fun OpsButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, secondary: Boolean = false, enabled: Boolean = true, symbol: OpsSymbol? = null, danger: Boolean = false) {
    val content: @Composable RowScope.() -> Unit = {
        if (symbol != null) { OpsIcon(symbol, Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    if (secondary) OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled, shape = RoundedCornerShape(12.dp), content = content)
    else Button(onClick, modifier.heightIn(min = 48.dp), enabled, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = if (danger) AdminColors.CompassionCoral else AdminColors.VolunteerBlue), content = content)
}
@Composable fun OpsField(label: String, value: String, onChange: (String) -> Unit, multiline: Boolean = false, numeric: Boolean = false, supporting: String? = null) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = !multiline, minLines = if (multiline) 3 else 1, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text), supportingText = supporting?.let { { Text(it) } })
}
@Composable fun SearchBox(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), placeholder = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }, leadingIcon = { OpsIcon(OpsSymbol.Search) }, trailingIcon = if (value.isNotEmpty()) ({ IconButton({ onChange("") }) { OpsIcon(OpsSymbol.Close, contentDescription = "Clear search") } }) else null, singleLine = true, shape = RoundedCornerShape(14.dp))
}
fun displayStatus(value: String) = when(value) { "pending" -> "Planned"; "deployed" -> "Assigned"; else -> value.replaceFirstChar { it.uppercase() } }
@OptIn(ExperimentalLayoutApi::class)
@Composable fun Choices(values: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        values.forEach { value -> FilterChip(selected == value, { onSelect(value) }, label = { Text(displayStatus(value)) }) }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable fun Tags(values: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { values.forEach { LiquidGlassTag(it) } }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable fun Actions(content: @Composable FlowRowScope.() -> Unit) { FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
@Composable fun KeyValue(label: String, value: String, symbol: OpsSymbol? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        if (symbol != null) OpsIcon(symbol, Modifier.size(20.dp).padding(top = 2.dp), AdminColors.Slate)
        Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.bodySmall, color = AdminColors.Slate); Text(value, style = MaterialTheme.typography.bodyLarge) }
    }
}
@Composable fun Capacity(assigned: Int, total: Int, label: String = "People assigned") {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$assigned / $total · $label", style = MaterialTheme.typography.labelMedium, color = AdminColors.Slate)
        LinearProgressIndicator(progress = { if (total > 0) (assigned.toFloat() / total).coerceIn(0f,1f) else 0f }, Modifier.fillMaxWidth().height(6.dp), color = AdminColors.VolunteerBlue, trackColor = AdminColors.SoftSky)
    }
}
data class Metric(val value: String, val label: String, val note: String, val symbol: OpsSymbol)
@Composable fun Metrics(items: List<Metric>) {
    BoxWithConstraints {
        val columns = if (maxWidth >= 760.dp) 4 else if (maxWidth >= 300.dp) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { item -> OpsCard(Modifier.weight(1f)) {
                        OpsIcon(item.symbol, Modifier.size(22.dp), AdminColors.VolunteerBlue)
                        Text(item.value, style = MaterialTheme.typography.headlineLarge)
                        Text(item.label, style = MaterialTheme.typography.titleSmall)
                        Text(item.note, style = MaterialTheme.typography.bodySmall, color = AdminColors.Slate)
                    } }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
@Composable fun Feedback(result: MutationResult?) {
    if (result != null) LiquidGlassAlert(if (result.success) "Workspace updated" else "Check this detail", result.message, if (result.success) GlassVariant.PRIMARY else GlassVariant.WARNING)
}
@Composable fun EmptyState(title: String = "No matching records", message: String = "Try another search or clear your filters.") {
    OpsCard { OpsIcon(OpsSymbol.Search, tint = AdminColors.Slate); SectionTitle(title, message) }
}
@Composable fun DetailSheet(title: String, onDismiss: () -> Unit, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val scroll = key(title) { rememberScrollState() }
    val scrolled by remember(scroll) { derivedStateOf { scroll.value > 0 } }
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp), contentAlignment = Alignment.Center) {
            Surface(Modifier.widthIn(max = 620.dp).fillMaxWidth().heightIn(max = maxHeight), shape = RoundedCornerShape(24.dp), color = Color.White) {
                Column {
                    Column(Modifier.zIndex(1f).shadow(if (scrolled) 5.dp else 0.dp).background(AdminColors.CanvasBg)) {
                        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            onBack?.let { GlassIconButton(OpsSymbol.Back, "Back", it) }
                            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                            IconButton(onDismiss) { OpsIcon(OpsSymbol.Close, contentDescription = "Close dialog") }
                        }
                        HorizontalDivider(color = AdminColors.BorderLight)
                    }
                    Column(Modifier.weight(1f, fill = false).verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
                }
            }
        }
    }
}
@Composable fun ToggleRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(description, style = MaterialTheme.typography.bodyMedium, color = AdminColors.Slate) }
        Switch(checked, onChange)
    }
}

