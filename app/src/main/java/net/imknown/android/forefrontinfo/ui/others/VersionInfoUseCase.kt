package net.imknown.android.forefrontinfo.ui.others

import net.imknown.android.forefrontinfo.ui.others.data.VersionInfoRepository

class VersionInfoUseCase(versionInfoRepository: VersionInfoRepository) {
    private val formatter = SimpleDateFormat(
        versionInfoRepository.getPreferredDateFormat(),
        versionInfoRepository.getPreferredLocale()
    )

    operator fun invoke(date: Date): String {
        return formatter.format(date)
    }
}