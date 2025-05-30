package net.imknown.android.forefrontinfo.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.base.list.MyViewHolderItem

// Define navigation items outside Content() as they are constant
private val navigationItemsData: List<Pair<String, ImageVector>> = listOf(
    "Home" to Icons.Outlined.Home,
    "Others" to Icons.Outlined.Info,
    "Properties" to Icons.AutoMirrored.Outlined.List,
    "Settings" to Icons.Outlined.Settings
)

class MainActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { Content() }
    }

    @Preview
    @Composable
    fun Content() {
        var selectedItemIndex by remember { mutableIntStateOf(0) }

        val homeListState = rememberLazyListState()
        val othersListState = rememberLazyListState()
        val propertiesListState = rememberLazyListState()

        Scaffold(
            modifier = Modifier.wrapContentWidth(), // Or .fillMaxSize()
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text(stringResource(id = R.string.app_name)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            },
            bottomBar = {
                AppNavigationBar(selectedItemIndex) { newIndex ->
                    selectedItemIndex = newIndex
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = contentColorFor(MaterialTheme.colorScheme.background)
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (selectedItemIndex) {
                    0 -> PageHome(homeListState)
                    1 -> PageOthers(othersListState)
                    2 -> PageProperties(propertiesListState)
                    3 -> PageSettings()
                }
            }
        }
    }
}

@Composable
private fun AppNavigationBar(selectedIndex: Int, onItemClick: (Int) -> Unit) {
    NavigationBar {
        navigationItemsData.forEachIndexed { index, item ->
            val (label, icon) = item
            val onClickLambda = remember(index, onItemClick) { { onItemClick(index) } }
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                selected = index == selectedIndex,
                onClick = onClickLambda
            )
        }
    }
}

@Composable
fun SimpleListPage(
    modifier: Modifier = Modifier,
    pageTitleForItems: String,
    listState: LazyListState = rememberLazyListState()
) {
    // Remember the list. If this data were from a ViewModel, it would already be stable.
    val items = remember(pageTitleForItems) { List(200) { "$pageTitleForItems Item #${it + 1}" } }
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), // Adjusted padding for Card
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it }) { item ->
            MyViewHolderItem(
                title = item,
                detail = "Detail for $item", // Placeholder detail
                circleColor = colorResource(id = R.color.colorNoProblem), // Example color
                onClick = { /* Handle click if needed */ }
            )
        }
    }
}

@Composable
fun PageHome(listState: LazyListState, modifier: Modifier = Modifier) {
    SimpleListPage(pageTitleForItems = "Home", listState = listState, modifier = modifier)
}

@Composable
fun PageOthers(listState: LazyListState, modifier: Modifier = Modifier) {
    SimpleListPage(pageTitleForItems = "Others", listState = listState, modifier = modifier)
}

@Composable
fun PageProperties(listState: LazyListState, modifier: Modifier = Modifier) {
    SimpleListPage(pageTitleForItems = "Properties", listState = listState, modifier = modifier)
}

@Composable
fun PageSettings(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings Screen Content", style = MaterialTheme.typography.headlineMedium)
    }
}
