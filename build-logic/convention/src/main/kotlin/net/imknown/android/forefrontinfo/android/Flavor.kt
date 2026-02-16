package net.imknown.android.forefrontinfo.android

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor
import org.gradle.kotlin.dsl.invoke

private enum class FlavorDimension {
    IssueTracker
}

enum class Flavor(
    val isDefault: Boolean = false, val hasVersionNameSuffix: Boolean = false
) {
    Foss(true, true),
    Firebase,
}

fun configureFlavors(
    commonExtension: CommonExtension,
    flavorConfigurationBlock: ProductFlavor.(flavor: Flavor) -> Unit = {},
) {
    commonExtension.apply {
        flavorDimensions += FlavorDimension.IssueTracker::class.simpleName.toString()

        productFlavors {
            Flavor.entries.forEach { flavor ->
                register(flavor.name) {
                    flavorConfigurationBlock(flavor)
                    if (this@apply is ApplicationExtension && this is ApplicationProductFlavor) {
                        if (flavor.hasVersionNameSuffix) {
                            versionNameSuffix = "-${flavor.name}"
                        }
                        isDefault = flavor.isDefault
                    }
                }
            }
        }
    }
}