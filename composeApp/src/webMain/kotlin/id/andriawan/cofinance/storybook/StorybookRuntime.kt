package id.andriawan.cofinance.storybook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.ic_account
import id.andriawan.cofinance.domain.model.response.Account
import id.andriawan.cofinance.domain.model.response.Transaction
import id.andriawan.cofinance.domain.model.response.TransactionByDate
import id.andriawan.cofinance.pages.stats.StatItem
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.enums.TransactionCategory
import id.andriawan.cofinance.utils.enums.TransactionType
import kotlin.time.Clock
import org.jetbrains.compose.resources.painterResource

@Immutable
data class StoryDefinition(
    val id: String,
    val title: String,
    val componentName: String,
    val description: String,
    val sourcePath: String,
    val args: List<StoryArgDefinition>,
    val render: @Composable (StoryArgsState) -> Unit,
    val sourceCode: (StoryArgsState) -> String
)

@Immutable
data class StoryArgDefinition(
    val id: String,
    val label: String,
    val description: String,
    val control: StoryArgControl,
    val defaultValue: StoryArgValue,
    val options: List<String> = emptyList()
)

enum class StoryArgControl {
    TEXT,
    TEXTAREA,
    BOOLEAN,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    SELECT
}

sealed interface StoryArgValue {
    fun sourceLiteral(): String

    data class Text(val value: String, val nullable: Boolean = false) : StoryArgValue {
        override fun sourceLiteral(): String {
            if (nullable && value.isBlank()) return "null"
            return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        }
    }

    data class BooleanValue(val value: Boolean) : StoryArgValue {
        override fun sourceLiteral(): String = value.toString()
    }

    data class IntValue(val value: Int) : StoryArgValue {
        override fun sourceLiteral(): String = value.toString()
    }

    data class LongValue(val value: Long) : StoryArgValue {
        override fun sourceLiteral(): String = "${value}L"
    }

    data class FloatValue(val value: Float) : StoryArgValue {
        override fun sourceLiteral(): String = "${value}f"
    }

    data class DoubleValue(val value: Double) : StoryArgValue {
        override fun sourceLiteral(): String = value.toString()
    }

    data class Option(val value: String) : StoryArgValue {
        override fun sourceLiteral(): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }
}

@Stable
class StoryArgsState(story: StoryDefinition) {
    private val values = mutableStateMapOf<String, StoryArgValue>()
    val definitions = story.args
    val events = mutableStateListOf<String>()

    init {
        story.args.forEach { definition ->
            values[definition.id] = definition.defaultValue
        }
    }

    fun string(id: String): String = (values.getValue(id) as StoryArgValue.Text).value

    fun nullableString(id: String): String? {
        val value = (values.getValue(id) as StoryArgValue.Text).value
        return value.ifBlank { null }
    }

    fun boolean(id: String): Boolean = (values.getValue(id) as StoryArgValue.BooleanValue).value
    fun int(id: String): Int = (values.getValue(id) as StoryArgValue.IntValue).value
    fun long(id: String): Long = (values.getValue(id) as StoryArgValue.LongValue).value
    fun float(id: String): Float = (values.getValue(id) as StoryArgValue.FloatValue).value
    fun double(id: String): Double = (values.getValue(id) as StoryArgValue.DoubleValue).value
    fun option(id: String): String = (values.getValue(id) as StoryArgValue.Option).value

    fun setString(id: String, value: String) {
        val current = values.getValue(id) as StoryArgValue.Text
        values[id] = current.copy(value = value)
    }

    fun setBoolean(id: String, value: Boolean) {
        values[id] = StoryArgValue.BooleanValue(value)
    }

    fun setInt(id: String, value: Int) {
        values[id] = StoryArgValue.IntValue(value)
    }

    fun setLong(id: String, value: Long) {
        values[id] = StoryArgValue.LongValue(value)
    }

    fun setFloat(id: String, value: Float) {
        values[id] = StoryArgValue.FloatValue(value)
    }

    fun setDouble(id: String, value: Double) {
        values[id] = StoryArgValue.DoubleValue(value)
    }

    fun setOption(id: String, value: String) {
        values[id] = StoryArgValue.Option(value)
    }

    fun sourceLiteral(id: String): String = values.getValue(id).sourceLiteral()

    fun action(name: String, payload: Any? = null) {
        val suffix = payload?.let { ": $it" }.orEmpty()
        events.add(0, "$name$suffix")
        if (events.size > 24) {
            events.removeLast()
        }
    }
}

@Composable
fun rememberStoryArgsState(story: StoryDefinition): StoryArgsState {
    return remember(story.id) { StoryArgsState(story) }
}

@Composable
fun StoryCanvas(
    story: StoryDefinition,
    argsState: StoryArgsState
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SIZE_24),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
        ) {
            Text(
                text = story.componentName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )

            story.render(argsState)
        }
    }
}

@Composable
fun StoryControlsPanel(argsState: StoryArgsState) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SIZE_20),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
        ) {
            Text(
                text = "Controls",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )

            argsState.definitions.forEach { definition ->
                Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)) {
                    Text(
                        text = definition.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    if (definition.description.isNotBlank()) {
                        Text(
                            text = definition.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    when (definition.control) {
                        StoryArgControl.TEXT, StoryArgControl.TEXTAREA -> {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = argsState.string(definition.id),
                                onValueChange = { argsState.setString(definition.id, it) },
                                minLines = if (definition.control == StoryArgControl.TEXTAREA) 3 else 1
                            )
                        }

                        StoryArgControl.BOOLEAN -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = if (argsState.boolean(definition.id)) "Enabled" else "Disabled",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Switch(
                                    checked = argsState.boolean(definition.id),
                                    onCheckedChange = { argsState.setBoolean(definition.id, it) }
                                )
                            }
                        }

                        StoryArgControl.INT -> NumericField(
                            value = argsState.int(definition.id).toString(),
                            onValueChange = { argsState.setInt(definition.id, it.toIntOrNull() ?: 0) }
                        )

                        StoryArgControl.LONG -> NumericField(
                            value = argsState.long(definition.id).toString(),
                            onValueChange = { argsState.setLong(definition.id, it.toLongOrNull() ?: 0L) }
                        )

                        StoryArgControl.FLOAT -> NumericField(
                            value = argsState.float(definition.id).toString(),
                            onValueChange = { argsState.setFloat(definition.id, it.toFloatOrNull() ?: 0f) }
                        )

                        StoryArgControl.DOUBLE -> NumericField(
                            value = argsState.double(definition.id).toString(),
                            onValueChange = { argsState.setDouble(definition.id, it.toDoubleOrNull() ?: 0.0) }
                        )

                        StoryArgControl.SELECT -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
                            ) {
                                definition.options.forEach { option ->
                                    FilterChip(
                                        selected = argsState.option(definition.id) == option,
                                        onClick = { argsState.setOption(definition.id, option) },
                                        label = { Text(option) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumericField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange
    )
}

@Composable
fun StoryEventsPanel(argsState: StoryArgsState) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SIZE_20),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
        ) {
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )

            if (argsState.events.isEmpty()) {
                Text(
                    text = "Interact with the preview to log callbacks here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)) {
                    argsState.events.forEach { event ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                modifier = Modifier.padding(
                                    horizontal = Dimensions.SIZE_12,
                                    vertical = Dimensions.SIZE_8
                                ),
                                text = event,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryCodePanel(source: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SIZE_20),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
        ) {
            Text(
                text = "Code",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(Dimensions.SIZE_16)
                    )
                    .padding(Dimensions.SIZE_16)
            ) {
                Text(
                    text = source,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
fun StorybookTextSlot(text: String) {
    Text(text = text)
}

fun storybookSourceTextSlot(textLiteral: String): String {
    return "{ StorybookTextSlot($textLiteral) }"
}

@Composable
fun StorybookPlaceholderIcon() {
    Icon(
        painter = painterResource(Res.drawable.ic_account),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
    )
}

fun storybookSourceIconSlot(): String = "{ StorybookPlaceholderIcon() }"

@Composable
fun StorybookEndAction(label: String) {
    TextButton(onClick = { }) {
        Text(label)
    }
}

fun storybookSourceEndAction(labelLiteral: String): String {
    return "{ StorybookEndAction($labelLiteral) }"
}

@Composable
fun StorybookPieChartDetail(data: List<StatItem>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
    ) {
        data.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimensions.SIZE_12)
                        .background(
                            color = item.category.iconColor,
                            shape = RoundedCornerShape(Dimensions.SIZE_4)
                        )
                )

                Text(
                    modifier = Modifier.padding(start = Dimensions.SIZE_12),
                    text = item.category.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

fun storybookSourcePieChartDetail(): String {
    return "{ data -> StorybookPieChartDetail(data) }"
}

fun storybookSampleTransaction(): Transaction {
    return Transaction(
        id = "storybook-transaction",
        amount = 184_000,
        category = TransactionCategory.FOOD.name,
        date = "2026-04-28T12:30:00Z",
        notes = "Lunch and coffee",
        account = Account(name = "BCA Wallet"),
        type = TransactionType.EXPENSE
    )
}

fun storybookSampleTransactionByDate(): TransactionByDate {
    return TransactionByDate(
        dateLabel = "2026-04-28T00:00:00Z",
        totalAmount = 454_000,
        transactions = listOf(
            storybookSampleTransaction(),
            storybookSampleTransaction().copy(
                id = "storybook-transaction-2",
                amount = 270_000,
                category = TransactionCategory.TRANSPORT.name,
                notes = "Airport ride"
            )
        )
    )
}

fun storybookSampleStatItems(): List<StatItem> {
    return listOf(
        StatItem(
            category = TransactionCategory.FOOD,
            amount = 1_200_000,
            percentage = 40f,
            sweepAngle = 144f
        ),
        StatItem(
            category = TransactionCategory.TRANSPORT,
            amount = 900_000,
            percentage = 30f,
            sweepAngle = 108f
        ),
        StatItem(
            category = TransactionCategory.SUBSCRIPTION,
            amount = 450_000,
            percentage = 15f,
            sweepAngle = 54f
        ),
        StatItem(
            category = TransactionCategory.OTHERS,
            amount = 450_000,
            percentage = 15f,
            sweepAngle = 54f
        )
    )
}

@Composable
fun storybookRememberDatePickerState() = rememberDatePickerState(
    initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds()
)
