package net.imknown.android.forefrontinfo.ui.base.list

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.persistentListOf
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.theme.AppTheme
import net.imknown.android.forefrontinfo.ui.theme.LocalExtendedColors
import net.imknown.android.forefrontinfo.ui.theme.StatusColor
import net.imknown.android.forefrontinfo.ui.theme.of

/** Size of the status color dot. Legacy XML used 16sp so it scales with font size; Compose sizes only accept dp, hence the runtime conversion. */
private val MyModelColorDotSize = 16.sp

@Composable
fun MyModelCard(
    model: MyModel,
    modifier: Modifier = Modifier,
) {
    Card(
        // Legacy MaterialCardView was clickable + focusable with no click listener = ripple-only
        // feedback; the onClick overload is the Compose way to get that ripple back.
        onClick = {},
        modifier = modifier
            .fillMaxWidth()
            // Animates the card's own height when its content changes (e.g. detail text gains a
            // line). Legacy RecyclerView's DefaultItemAnimator animated these changes; LazyColumn
            // animates only item placement, so the size change must be animated here.
            .animateContentSize(),
        // Legacy Widget.Material3.CardView.Filled has no shadow; Compose Card defaults to 1dp, so zero it out.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ),
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.item_card_padding))
        ) {
            Box { // title fills the width, dot overlays the top-end corner
                Text(
                    text = model.title.asText(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Conditional composition: with no color the dot is simply not composed (vs. composing-then-hiding)
                if (model.color != StatusColor.NONE) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            // sp to dp so the dot still scales with the user's font size
                            .size(with(LocalDensity.current) { MyModelColorDotSize.toDp() })
                            // clip before background, or the color overflows the circle
                            .clip(CircleShape)
                            .background(LocalExtendedColors.current.of(model.color)),
                    )
                }
            }

            Text(
                text = model.detail,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Resolves the sealed [MyModelTitle]: resource ids via stringResource (cached), raw text as-is. */
@Composable
fun MyModelTitle.asText(): String = when (this) {
    is MyModelTitle.Res -> stringResource(id)
    is MyModelTitle.Raw -> text
}

// Sample data shared by the @Previews in this package (internal: visible module-wide,
// also used by the MyModelListScreen preview)
internal val previewModels = persistentListOf(
    MyModel(MyModelTitle.Raw("SELinux"), "Enforcing", color = StatusColor.NO_PROBLEM),
    MyModel(MyModelTitle.Raw("A/B partitions"), "A only", color = StatusColor.CRITICAL),
    MyModel(MyModelTitle.Raw("General entry"), "No status color dot"),
)

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun MyModelCardPreview() {
    AppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            previewModels.forEach { MyModelCard(it) }
        }
    }
}
