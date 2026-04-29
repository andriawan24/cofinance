package id.andriawan.cofinance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.andriawan.cofinance.storybook.StoryCanvas
import id.andriawan.cofinance.storybook.StoryCodePanel
import id.andriawan.cofinance.storybook.StoryControlsPanel
import id.andriawan.cofinance.storybook.StoryDefinition
import id.andriawan.cofinance.storybook.StoryEventsPanel
import id.andriawan.cofinance.storybook.rememberStoryArgsState
import id.andriawan.cofinance.storybook.generated.GeneratedStorybookCatalog
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions

@Composable
fun ComponentCatalogApp() {
    val stories = remember { GeneratedStorybookCatalog.stories }
    var selectedStoryId by rememberSaveable { androidx.compose.runtime.mutableStateOf(stories.firstOrNull()?.id.orEmpty()) }
    val selectedStory = stories.firstOrNull { it.id == selectedStoryId } ?: stories.first()

    CofinanceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                if (maxWidth < 1040.dp) {
                    CompactCatalog(stories, selectedStory, onSelect = { selectedStoryId = it })
                } else {
                    WideCatalog(stories, selectedStory, onSelect = { selectedStoryId = it })
                }
            }
        }
    }
}

@Composable
private fun WideCatalog(
    stories: List<StoryDefinition>,
    selectedStory: StoryDefinition,
    onSelect: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight(),
            stories = stories,
            selectedStory = selectedStory,
            onSelect = onSelect
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        )

        StoryScreen(
            modifier = Modifier.weight(1f),
            story = selectedStory
        )
    }
}

@Composable
private fun CompactCatalog(
    stories: List<StoryDefinition>,
    selectedStory: StoryDefinition,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CatalogHeader(
            modifier = Modifier.padding(
                start = Dimensions.SIZE_20,
                top = Dimensions.SIZE_20,
                end = Dimensions.SIZE_20,
                bottom = Dimensions.SIZE_12
            ),
            storyCount = stories.size
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Dimensions.SIZE_20),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
        ) {
            items(stories, key = { it.id }) { story ->
                StoryChip(
                    title = story.title,
                    selected = story.id == selectedStory.id,
                    onClick = { onSelect(story.id) }
                )
            }
        }

        StoryScreen(
            modifier = Modifier.weight(1f),
            story = selectedStory
        )
    }
}

@Composable
private fun Sidebar(
    modifier: Modifier = Modifier,
    stories: List<StoryDefinition>,
    selectedStory: StoryDefinition,
    onSelect: (String) -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Dimensions.SIZE_16),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
        ) {
            item {
                CatalogHeader(
                    modifier = Modifier.padding(bottom = Dimensions.SIZE_8),
                    storyCount = stories.size
                )
            }

            items(stories, key = { it.id }) { story ->
                SidebarItem(
                    title = story.title,
                    selected = story.id == selectedStory.id,
                    onClick = { onSelect(story.id) }
                )
            }
        }
    }
}

@Composable
private fun CatalogHeader(
    modifier: Modifier = Modifier,
    storyCount: Int
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
    ) {
        Text(
            text = "Cofinance Storybook",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Generated from public Compose components with Storybook-style args, argTypes, controls, and code preview.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "$storyCount generated stories",
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun SidebarItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            modifier = Modifier.padding(Dimensions.SIZE_16),
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun StoryChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = Dimensions.SIZE_16,
                vertical = Dimensions.SIZE_10
            ),
            text = title.substringAfterLast('/'),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun StoryScreen(
    modifier: Modifier = Modifier,
    story: StoryDefinition
) {
    val argsState = rememberStoryArgsState(story)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(Dimensions.SIZE_24)
    ) {
        val availableWidth = maxWidth
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_20)
        ) {
            Text(
                text = story.title,
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = story.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = Dimensions.SIZE_12,
                        vertical = Dimensions.SIZE_8
                    ),
                    text = story.sourcePath,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            if (availableWidth < 1240.dp) {
                StoryCanvas(story = story, argsState = argsState)
                StoryControlsPanel(argsState = argsState)
                StoryEventsPanel(argsState = argsState)
                StoryCodePanel(source = story.sourceCode(argsState))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_20),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_20)
                    ) {
                        StoryCanvas(story = story, argsState = argsState)
                        StoryCodePanel(source = story.sourceCode(argsState))
                    }

                    Column(
                        modifier = Modifier.weight(0.8f),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_20)
                    ) {
                        StoryControlsPanel(argsState = argsState)
                        StoryEventsPanel(argsState = argsState)
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.SIZE_24))
        }
    }
}
