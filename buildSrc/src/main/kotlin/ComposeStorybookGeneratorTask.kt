import java.io.File
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class ComposeStorybookGeneratorTask : DefaultTask() {
    @get:InputDirectory
    abstract val componentsDir: DirectoryProperty

    @get:InputFile
    abstract val configFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val config = StorybookConfig.load(configFile.get().asFile)
        val components = componentsDir.get().asFile
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> parseComposableFunctions(file).map { it.copy(file = file) } }
            .filter { component -> isEnabled(component, config) }
            .mapNotNull { component -> component.resolve(config) }
            .sortedBy { it.title }
            .toList()

        val outputRoot = outputDir.get().asFile
        val packagePath = File(outputRoot, "id/andriawan/cofinance/storybook/generated")
        packagePath.mkdirs()

        File(packagePath, "GeneratedStorybookCatalog.kt").writeText(
            buildString {
                appendLine("package id.andriawan.cofinance.storybook.generated")
                appendLine()
                appendLine("import androidx.compose.runtime.Composable")
                appendLine("import androidx.compose.ui.Modifier")
                appendLine("import id.andriawan.cofinance.components.*")
                appendLine("import id.andriawan24.cofinance.andro.ui.presentation.activity.components.*")
                appendLine("import id.andriawan.cofinance.storybook.*")
                appendLine()
                appendLine("object GeneratedStorybookCatalog {")
                appendLine("    val stories: List<StoryDefinition> = listOf(")

                components.forEachIndexed { index, component ->
                    component.writeTo(this)
                    if (index != components.lastIndex) {
                        appendLine(",")
                    } else {
                        appendLine()
                    }
                }

                appendLine("    )")
                appendLine("}")
            }
        )
    }

    private fun isEnabled(component: ParsedComponent, config: StorybookConfig): Boolean {
        return config.components[component.name]?.enabled ?: true
    }

    private fun parseComposableFunctions(file: File): List<ParsedComponent> {
        val content = file.readText()
        val results = mutableListOf<ParsedComponent>()
        var searchIndex = 0

        while (true) {
            val composableIndex = content.indexOf("@Composable", startIndex = searchIndex)
            if (composableIndex == -1) break

            val funIndex = content.indexOf("fun ", startIndex = composableIndex)
            if (funIndex == -1) break

            val declarationPrefix = content.substring(composableIndex, funIndex)
            if (declarationPrefix.contains("@Preview")) {
                searchIndex = funIndex + 4
                continue
            }

            val lineStart = content.lastIndexOf('\n', startIndex = composableIndex).let { index ->
                if (index == -1) 0 else index + 1
            }
            val indentation = content.substring(lineStart, composableIndex)
            if (indentation.isNotBlank()) {
                searchIndex = funIndex + 4
                continue
            }

            val nameStart = funIndex + 4
            val parenStart = content.indexOf('(', startIndex = nameStart)
            if (parenStart == -1) break

            val header = content.substring(composableIndex, parenStart)
            if (
                header.contains("private fun") ||
                header.contains("internal fun") ||
                header.contains("expect fun")
            ) {
                searchIndex = parenStart + 1
                continue
            }

            val rawSignature = content.substring(nameStart, parenStart).trim()
            val functionName = rawSignature.substringAfterLast('.').trim()
            val receiver = rawSignature.substringBeforeLast('.', "")
            val modifiers = rawSignature.removeSuffix(functionName).trim()

            if (functionName.isBlank() || functionName.firstOrNull()?.isUpperCase() != true) {
                searchIndex = parenStart + 1
                continue
            }

            if (
                functionName.endsWith("Preview") ||
                receiver.isNotBlank() ||
                modifiers.contains("private") ||
                modifiers.contains("internal")
            ) {
                searchIndex = parenStart + 1
                continue
            }

            val parenEnd = findMatching(content, parenStart, '(', ')') ?: break
            val headerTail = content.substring(parenEnd + 1, minOf(content.length, parenEnd + 120))
            if (!headerTail.contains("{") && !headerTail.contains("=")) {
                searchIndex = parenEnd + 1
                continue
            }

            val params = splitTopLevel(content.substring(parenStart + 1, parenEnd))
                .mapNotNull { parseParameter(it) }

            results += ParsedComponent(
                name = functionName,
                params = params,
                file = file
            )

            searchIndex = parenEnd + 1
        }

        return results
    }

    private fun findMatching(
        source: String,
        startIndex: Int,
        openChar: Char,
        closeChar: Char
    ): Int? {
        var depth = 0
        var inString = false
        var quote = '\u0000'
        var escaped = false

        for (index in startIndex until source.length) {
            val char = source[index]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == quote) {
                    inString = false
                }
                continue
            }

            if (char == '"' || char == '\'') {
                inString = true
                quote = char
                continue
            }

            if (char == openChar) depth++
            if (char == closeChar) {
                depth--
                if (depth == 0) return index
            }
        }

        return null
    }

    private fun splitTopLevel(source: String): List<String> {
        if (source.isBlank()) return emptyList()

        val result = mutableListOf<String>()
        val current = StringBuilder()
        var angleDepth = 0
        var parenDepth = 0
        var braceDepth = 0
        var inString = false
        var quote = '\u0000'
        var escaped = false

        source.forEachIndexed { index, char ->
            val previousChar = source.getOrNull(index - 1)
            if (inString) {
                current.append(char)
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == quote) {
                    inString = false
                }
                return@forEachIndexed
            }

            when (char) {
                '"', '\'' -> {
                    inString = true
                    quote = char
                    current.append(char)
                }

                '<' -> {
                    angleDepth++
                    current.append(char)
                }

                '>' -> {
                    if (previousChar != '-') {
                        angleDepth--
                    }
                    current.append(char)
                }

                '(' -> {
                    parenDepth++
                    current.append(char)
                }

                ')' -> {
                    parenDepth--
                    current.append(char)
                }

                '{' -> {
                    braceDepth++
                    current.append(char)
                }

                '}' -> {
                    braceDepth--
                    current.append(char)
                }

                ',' -> {
                    if (angleDepth == 0 && parenDepth == 0 && braceDepth == 0) {
                        result += current.toString().trim()
                        current.clear()
                    } else {
                        current.append(char)
                    }
                }

                else -> current.append(char)
            }
        }

        val last = current.toString().trim()
        if (last.isNotBlank()) result += last
        return result
    }

    private fun parseParameter(raw: String): ParsedParam? {
        if (raw.isBlank()) return null
        val cleaned = raw
            .replace('\n', ' ')
            .replace(Regex("\\s+"), " ")
            .replace(Regex("^(@[A-Za-z0-9_().,]+\\s+)+"), "")
            .trim()
        val defaultIndex = topLevelEqualsIndex(cleaned)
        val declaration = if (defaultIndex >= 0) cleaned.substring(0, defaultIndex).trim() else cleaned
        val defaultValue = if (defaultIndex >= 0) cleaned.substring(defaultIndex + 1).trim() else null
        val colonIndex = declaration.indexOf(':')
        if (colonIndex == -1) return null

        return ParsedParam(
            name = declaration.substring(0, colonIndex).trim(),
            type = declaration.substring(colonIndex + 1).trim(),
            defaultValue = defaultValue
        )
    }

    private fun topLevelEqualsIndex(source: String): Int {
        var angleDepth = 0
        var parenDepth = 0
        var braceDepth = 0
        var inString = false
        var quote = '\u0000'
        var escaped = false

        source.forEachIndexed { index, char ->
            val previousChar = source.getOrNull(index - 1)
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == quote) {
                    inString = false
                }
                return@forEachIndexed
            }

            when (char) {
                '"', '\'' -> {
                    inString = true
                    quote = char
                }

                '<' -> angleDepth++
                '>' -> if (previousChar != '-') angleDepth--
                '(' -> parenDepth++
                ')' -> parenDepth--
                '{' -> braceDepth++
                '}' -> braceDepth--
                '=' -> if (angleDepth == 0 && parenDepth == 0 && braceDepth == 0) return index
            }
        }

        return -1
    }
}

private data class ParsedComponent(
    val name: String,
    val params: List<ParsedParam>,
    val file: File
) {
    fun resolve(config: StorybookConfig): GeneratedComponent? {
        val overrides = config.components[name] ?: ComponentOverride()
        val syntheticArgs = mutableListOf<GeneratedArg>()
        val renderArguments = mutableListOf<String>()
        val sourceArguments = mutableListOf<GeneratedSourceArg>()

        params.forEach { param ->
            val resolved = resolveParam(param, overrides, syntheticArgs) ?: return null
            if (resolved.renderArgument != null) {
                renderArguments += "            ${param.name} = ${resolved.renderArgument}"
            }
            if (resolved.sourceArgument != null) {
                sourceArguments += GeneratedSourceArg(
                    name = param.name,
                    snippet = resolved.sourceArgument
                )
            }
        }

        val title = overrides.title ?: "Components/${readableName(name)}"
        val description = overrides.description ?: "Auto-generated story for $name."
        val sourcePath = file.relativeTo(File(file.path.substringBefore("/composeApp/") + "/composeApp")).path

        return GeneratedComponent(
            id = kebabCase(name),
            title = title,
            componentName = name,
            description = description,
            sourcePath = sourcePath,
            args = syntheticArgs,
            renderArguments = renderArguments,
            sourceArguments = sourceArguments
        )
    }

    private fun resolveParam(
        param: ParsedParam,
        overrides: ComponentOverride,
        syntheticArgs: MutableList<GeneratedArg>
    ): ResolvedParam? {
        val override = overrides.args[param.name] ?: ArgOverride()
        if (override.hidden == true) {
            return if (param.defaultValue != null) ResolvedParam(null, null) else null
        }

        if (override.sampleProvider != null) {
            val provider = SAMPLE_PROVIDERS[override.sampleProvider] ?: return null
            return ResolvedParam(
                renderArgument = provider.renderExpression,
                sourceArgument = SourceSnippet(provider.sourceExpression, dynamic = false)
            )
        }

        val slotProvider = override.slotProvider ?: inferSlotProvider(param)
        if (slotProvider != null) {
            val slot = SLOT_PROVIDERS[slotProvider] ?: return null
            val generated = slot.materialize(param, override)
            syntheticArgs += generated.syntheticArgs
            return ResolvedParam(generated.renderExpression, generated.sourceExpression)
        }

        val typeInfo = inferScalarType(param.type, override)
        if (typeInfo != null) {
            val argId = override.argId ?: param.name
            syntheticArgs += GeneratedArg(
                id = argId,
                label = readableName(argId),
                description = override.description ?: "Generated from `${param.name}`.",
                control = typeInfo.control,
                defaultExpression = typeInfo.defaultExpression(override.defaultValue),
                options = typeInfo.options(override)
            )

            val renderExpression = when (typeInfo.control) {
                "text" -> if (typeInfo.nullable) "args.nullableString(\"$argId\")" else "args.string(\"$argId\")"
                "textarea" -> "args.string(\"$argId\")"
                "boolean" -> "args.boolean(\"$argId\")"
                "int" -> "args.int(\"$argId\")"
                "long" -> "args.long(\"$argId\")"
                "float" -> "args.float(\"$argId\")"
                "double" -> "args.double(\"$argId\")"
                "select" -> "args.option(\"$argId\")"
                else -> return null
            }

            return ResolvedParam(
                renderArgument = renderExpression,
                sourceArgument = SourceSnippet("args.sourceLiteral(\"$argId\")", dynamic = true)
            )
        }

        if (isModifier(param.type)) {
            return ResolvedParam(
                renderArgument = if (param.defaultValue == null) "Modifier" else null,
                sourceArgument = if (param.defaultValue == null) {
                    SourceSnippet("Modifier", dynamic = false)
                } else {
                    null
                }
            )
        }

        if (isCallback(param.type)) {
            val callback = callbackExpression(param, syntheticArgs)
            return ResolvedParam(
                renderArgument = callback.renderExpression,
                sourceArgument = SourceSnippet(callback.sourceExpression, dynamic = false)
            )
        }

        return if (param.defaultValue != null) {
            ResolvedParam(null, null)
        } else {
            null
        }
    }

    private fun callbackExpression(
        param: ParsedParam,
        syntheticArgs: MutableList<GeneratedArg>
    ): CallbackCode {
        val updateTarget = Regex("^on([A-Z].*)Changed$").matchEntire(param.name)
            ?.groupValues
            ?.get(1)
            ?.replaceFirstChar { it.lowercase() }
        val callbackPayloadType = callbackPayloadType(param.type)

        if (updateTarget != null && callbackPayloadType != null) {
            return when (callbackPayloadType) {
                "String" -> CallbackCode(
                    renderExpression = "{ value -> args.setString(\"$updateTarget\", value); args.action(\"${param.name}\", value) }",
                    sourceExpression = "{ }"
                )

                "Boolean" -> CallbackCode(
                    renderExpression = "{ value -> args.setBoolean(\"$updateTarget\", value); args.action(\"${param.name}\", value) }",
                    sourceExpression = "{ }"
                )

                "Int" -> CallbackCode(
                    renderExpression = "{ value -> args.setInt(\"$updateTarget\", value); args.action(\"${param.name}\", value) }",
                    sourceExpression = "{ }"
                )

                "Long" -> CallbackCode(
                    renderExpression = "{ value -> args.setLong(\"$updateTarget\", value); args.action(\"${param.name}\", value) }",
                    sourceExpression = "{ }"
                )

                "Float" -> CallbackCode(
                    renderExpression = "{ value -> args.setFloat(\"$updateTarget\", value); args.action(\"${param.name}\", value) }",
                    sourceExpression = "{ }"
                )

                "Double" -> CallbackCode(
                    renderExpression = "{ value -> args.setDouble(\"$updateTarget\", value); args.action(\"${param.name}\", value) }",
                    sourceExpression = "{ }"
                )

                else -> CallbackCode(
                    renderExpression = "{ value -> args.action(\"${param.name}\", value) }",
                    sourceExpression = "{ }"
                )
            }
        }

        return CallbackCode(
            renderExpression = if (callbackPayloadType == null) {
                "{ args.action(\"${param.name}\") }"
            } else {
                "{ value -> args.action(\"${param.name}\", value) }"
            },
            sourceExpression = "{ }"
        )
    }

    private fun inferSlotProvider(param: ParsedParam): String? {
        if (!param.type.contains("@Composable")) return null
        return when {
            param.name.endsWith("Icon", ignoreCase = false) -> "icon"
            param.name == "content" -> "text"
            param.name == "startIcon" -> "icon"
            param.name == "endIcon" -> "icon"
            param.name == "endContent" -> "endAction"
            param.name == "detailChart" -> "pieChartDetail"
            else -> if (param.defaultValue == null) "text" else null
        }
    }

    private fun inferScalarType(type: String, override: ArgOverride): ScalarType? {
        val normalized = type.removeSuffix("?")
        return when {
            override.control == "textarea" -> ScalarType("textarea", type.endsWith("?"))
            override.control == "select" -> ScalarType("select", type.endsWith("?"))
            normalized == "String" -> ScalarType("text", type.endsWith("?"))
            normalized == "Boolean" -> ScalarType("boolean")
            normalized == "Int" -> ScalarType("int")
            normalized == "Long" -> ScalarType("long")
            normalized == "Float" -> ScalarType("float")
            normalized == "Double" -> ScalarType("double")
            else -> null
        }
    }
}

private data class ParsedParam(
    val name: String,
    val type: String,
    val defaultValue: String?
)

private data class ResolvedParam(
    val renderArgument: String?,
    val sourceArgument: SourceSnippet?
)

private data class GeneratedComponent(
    val id: String,
    val title: String,
    val componentName: String,
    val description: String,
    val sourcePath: String,
    val args: List<GeneratedArg>,
    val renderArguments: List<String>,
    val sourceArguments: List<GeneratedSourceArg>
) {
    fun writeTo(builder: StringBuilder) {
        builder.appendLine("        StoryDefinition(")
        builder.appendLine("            id = \"$id\",")
        builder.appendLine("            title = \"${escape(title)}\",")
        builder.appendLine("            componentName = \"$componentName\",")
        builder.appendLine("            description = \"${escape(description)}\",")
        builder.appendLine("            sourcePath = \"$sourcePath\",")
        builder.appendLine("            args = listOf(")
        args.forEachIndexed { index, arg ->
            arg.writeTo(builder)
            if (index != args.lastIndex) builder.appendLine(",") else builder.appendLine()
        }
        builder.appendLine("            ),")
        builder.appendLine("            render = { args ->")
        builder.appendLine("                $componentName(")
        renderArguments.forEachIndexed { index, line ->
            builder.appendLine(
                if (index == renderArguments.lastIndex) line
                else "$line,"
            )
        }
        builder.appendLine("                )")
        builder.appendLine("            },")
        builder.appendLine("            sourceCode = { args ->")
        builder.appendLine("                buildString {")
        builder.appendLine("                    appendLine(\"$componentName(\")")
        sourceArguments.forEachIndexed { index, argument ->
            val suffix = if (index == sourceArguments.lastIndex) "" else ","
            if (argument.snippet.dynamic) {
                builder.appendLine(
                    "                    appendLine(\"    ${argument.name} = \${${argument.snippet.expression}}$suffix\")"
                )
            } else {
                builder.appendLine(
                    "                    appendLine(\"    ${argument.name} = ${escape(argument.snippet.expression)}$suffix\")"
                )
            }
        }
        builder.appendLine("                    append(\")\")")
        builder.appendLine("                }")
        builder.appendLine("            }")
        builder.append("        )")
    }
}

private data class GeneratedArg(
    val id: String,
    val label: String,
    val description: String,
    val control: String,
    val defaultExpression: String,
    val options: List<String> = emptyList()
) {
    fun writeTo(builder: StringBuilder) {
        builder.appendLine("                StoryArgDefinition(")
        builder.appendLine("                    id = \"$id\",")
        builder.appendLine("                    label = \"${escape(label)}\",")
        builder.appendLine("                    description = \"${escape(description)}\",")
        builder.appendLine("                    control = StoryArgControl.${control.uppercase()},")
        builder.appendLine("                    defaultValue = $defaultExpression,")
        builder.appendLine(
            "                    options = listOf(${options.joinToString(", ") { "\"${escape(it)}\"" }})"
        )
        builder.append("                )")
    }
}

private data class GeneratedSourceArg(
    val name: String,
    val snippet: SourceSnippet
)

private data class ScalarType(
    val control: String,
    val nullable: Boolean = false
) {
    fun defaultExpression(overrideValue: String?): String {
        return when (control) {
            "text", "textarea" -> {
                val value = overrideValue ?: if (nullable) "Sample value" else "Sample value"
                "StoryArgValue.Text(${quote(value)}, nullable = $nullable)"
            }

            "boolean" -> "StoryArgValue.BooleanValue(${overrideValue ?: "false"})"
            "int" -> "StoryArgValue.IntValue(${overrideValue ?: "0"})"
            "long" -> "StoryArgValue.LongValue(${overrideValue ?: "0"})"
            "float" -> "StoryArgValue.FloatValue(${overrideValue ?: "0f"})"
            "double" -> "StoryArgValue.DoubleValue(${overrideValue ?: "0.0"})"
            "select" -> {
                val value = overrideValue ?: ""
                "StoryArgValue.Option(${quote(value)})"
            }

            else -> error("Unsupported control: $control")
        }
    }

    fun options(override: ArgOverride): List<String> = override.options
}

private data class CallbackCode(
    val renderExpression: String,
    val sourceExpression: String
)

private data class SourceSnippet(
    val expression: String,
    val dynamic: Boolean
)

private data class SampleProvider(
    val renderExpression: String,
    val sourceExpression: String = renderExpression
)

private data class SlotMaterialized(
    val renderExpression: String,
    val sourceExpression: SourceSnippet,
    val syntheticArgs: List<GeneratedArg> = emptyList()
)

private class SlotProvider(
    val materialize: (ParsedParam, ArgOverride) -> SlotMaterialized
)

private data class StorybookConfig(
    val components: Map<String, ComponentOverride>
) {
    companion object {
        fun load(file: File): StorybookConfig {
            val props = Properties().apply {
                file.inputStream().use { load(it) }
            }

            val grouped = mutableMapOf<String, MutableMap<String, MutableMap<String, String>>>()
            val componentValues = mutableMapOf<String, MutableMap<String, String>>()

            props.forEach { key, value ->
                val textKey = key.toString()
                val textValue = value.toString()
                val parts = textKey.split(".")
                if (parts.size >= 4 && parts[1] == "arg") {
                    val componentName = parts[0]
                    val argName = parts[2]
                    val property = parts.drop(3).joinToString(".")
                    grouped
                        .getOrPut(componentName) { mutableMapOf() }
                        .getOrPut(argName) { mutableMapOf() }[property] = textValue
                } else if (parts.size >= 2) {
                    val componentName = parts[0]
                    val property = parts.drop(1).joinToString(".")
                    componentValues.getOrPut(componentName) { mutableMapOf() }[property] = textValue
                }
            }

            val components = (grouped.keys + componentValues.keys).associateWith { name ->
                val componentProps = componentValues[name].orEmpty()
                val argOverrides = grouped[name].orEmpty().mapValues { (_, values) ->
                    ArgOverride(
                        control = values["control"],
                        description = values["description"],
                        defaultValue = values["default"],
                        options = values["options"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
                        sampleProvider = values["sampleProvider"],
                        slotProvider = values["slotProvider"],
                        hidden = values["hidden"]?.toBooleanStrictOrNull(),
                        argId = values["argId"]
                    )
                }

                ComponentOverride(
                    enabled = componentProps["enabled"]?.toBooleanStrictOrNull(),
                    title = componentProps["title"],
                    description = componentProps["description"],
                    args = argOverrides
                )
            }

            return StorybookConfig(components = components)
        }
    }
}

private data class ComponentOverride(
    val enabled: Boolean? = null,
    val title: String? = null,
    val description: String? = null,
    val args: Map<String, ArgOverride> = emptyMap()
)

private data class ArgOverride(
    val control: String? = null,
    val description: String? = null,
    val defaultValue: String? = null,
    val options: List<String> = emptyList(),
    val sampleProvider: String? = null,
    val slotProvider: String? = null,
    val hidden: Boolean? = null,
    val argId: String? = null
)

private val SAMPLE_PROVIDERS = mapOf(
    "transaction" to SampleProvider("storybookSampleTransaction()"),
    "transactionByDate" to SampleProvider("storybookSampleTransactionByDate()"),
    "statItems" to SampleProvider("storybookSampleStatItems()"),
    "datePickerState" to SampleProvider("storybookRememberDatePickerState()")
)

private val SLOT_PROVIDERS = mapOf(
    "text" to SlotProvider { param, override ->
        val argId = override.argId ?: "${param.name}Text"
        SlotMaterialized(
            renderExpression = "{ StorybookTextSlot(args.string(\"$argId\")) }",
            sourceExpression = SourceSnippet(
                expression = "storybookSourceTextSlot(args.sourceLiteral(\"$argId\"))",
                dynamic = true
            ),
            syntheticArgs = listOf(
                GeneratedArg(
                    id = argId,
                    label = readableName(argId),
                    description = override.description ?: "Synthetic slot text for `${param.name}`.",
                    control = override.control ?: "text",
                    defaultExpression = "StoryArgValue.Text(${quote(override.defaultValue ?: "Sample content")})"
                )
            )
        )
    },
    "icon" to SlotProvider { _, _ ->
        SlotMaterialized(
            renderExpression = "{ StorybookPlaceholderIcon() }",
            sourceExpression = SourceSnippet("storybookSourceIconSlot()", dynamic = true)
        )
    },
    "endAction" to SlotProvider { param, override ->
        val argId = override.argId ?: "${param.name}Text"
        SlotMaterialized(
            renderExpression = "{ StorybookEndAction(args.string(\"$argId\")) }",
            sourceExpression = SourceSnippet(
                expression = "storybookSourceEndAction(args.sourceLiteral(\"$argId\"))",
                dynamic = true
            ),
            syntheticArgs = listOf(
                GeneratedArg(
                    id = argId,
                    label = readableName(argId),
                    description = override.description ?: "Synthetic label for `${param.name}`.",
                    control = override.control ?: "text",
                    defaultExpression = "StoryArgValue.Text(${quote(override.defaultValue ?: "Action")})"
                )
            )
        )
    },
    "pieChartDetail" to SlotProvider { _, _ ->
        SlotMaterialized(
            renderExpression = "{ data -> StorybookPieChartDetail(data) }",
            sourceExpression = SourceSnippet("storybookSourcePieChartDetail()", dynamic = true)
        )
    }
)

private fun isModifier(type: String): Boolean = type.removeSuffix("?") == "Modifier"

private fun isCallback(type: String): Boolean = type.contains("->") && !type.contains("@Composable")

private fun callbackPayloadType(type: String): String? {
    var normalized = type.removeSuffix("?").trim()
    if (normalized.startsWith("(") && normalized.endsWith(")")) {
        normalized = normalized.removePrefix("(").removeSuffix(")").trim()
    }

    if (normalized == "() -> Unit") {
        return null
    }

    val trimmed = normalized.substringBefore("->").trim()
    return when {
        trimmed == "()" -> null
        trimmed.startsWith("(") && trimmed.endsWith(")") -> trimmed
            .removePrefix("(")
            .removeSuffix(")")
            .trim()
            .substringAfterLast(":")
            .trim()
            .ifBlank { null }
        else -> null
    }
}

private fun kebabCase(value: String): String {
    return value.replace(Regex("([a-z0-9])([A-Z])"), "$1-$2").lowercase()
}

private fun readableName(value: String): String {
    return value
        .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
        .replaceFirstChar { it.uppercase() }
}

private fun quote(value: String): String = "\"${escape(value)}\""

private fun escape(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}
