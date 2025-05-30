package net.imknown.android.forefrontinfo.ui.base.list

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import net.imknown.android.forefrontinfo.R

@Composable
fun MyViewHolderItem(
    title: String,
    detail: String,
    circleColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .focusable(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(id = R.dimen.item_card_padding),
                    vertical = dimensionResource(id = R.dimen.item_card_padding)
                )
        ) {
            val (tvTitleRef, sivColorRef, tvDetailRef) = createRefs()

            Text(
                text = title,
                modifier = Modifier.constrainAs(tvTitleRef) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    width = Dimension.fillToConstraints
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )

            Box(
                modifier = Modifier
                    .constrainAs(sivColorRef) {
                        end.linkTo(parent.end)
                        top.linkTo(tvTitleRef.top)
                    }
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(circleColor)
            )

            Text(
                text = detail,
                modifier = Modifier.constrainAs(tvDetailRef) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(tvTitleRef.bottom)
                    width = Dimension.fillToConstraints
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp
            )
        }
    }
}

@Preview
@Composable
fun PreviewMyViewHolderItem() {
    MyViewHolderItem(
        title = stringResource(id = R.string.android_info_title),
        detail = stringResource(id = R.string.android_info_detail),
        circleColor = colorResource(id = R.color.colorNoProblem),
        onClick = {}
    )
}
