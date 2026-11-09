package net.imknown.android.forefrontinfo.ui.home.repository

import android.app.admin.DevicePolicyManager
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.model.EncryptionState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.Shadows.shadowOf
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The domain contract of detectEncryption (spec 2026-10-25-22-16-01, subtask-04 batch 7): the
// status read rides on DevicePolicyManager, whose Robolectric shadow drives every branch, so
// per the means table this entry is a Robolectric unit test. Exact wording and color belong to
// the mapper and are covered by the golden snapshot comparison.
@ExtendWith(RobolectricExtension::class)
class DetectEncryptionTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    private fun detectWithStatus(status: Int): DetectResult<EncryptionState> {
        val devicePolicyManager =
            MyApplication.instance.getSystemService(DevicePolicyManager::class.java)
        shadowOf(devicePolicyManager).setStorageEncryptionStatus(status)
        return repository.detectEncryption()
    }

    @Test
    fun `whole device encryption is found as active`() {
        assertEquals(
            DetectResult.Found(EncryptionState(EncryptionState.Status.Active)),
            detectWithStatus(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE)
        )
    }

    @Test
    fun `per user encryption is found as active per user`() {
        assertEquals(
            DetectResult.Found(EncryptionState(EncryptionState.Status.ActivePerUser)),
            detectWithStatus(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER)
        )
    }

    @Test
    fun `default key encryption is found as active default key`() {
        assertEquals(
            DetectResult.Found(EncryptionState(EncryptionState.Status.ActiveDefaultKey)),
            detectWithStatus(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY)
        )
    }

    @Test
    fun `no encryption is found as inactive`() {
        assertEquals(
            DetectResult.Found(EncryptionState(EncryptionState.Status.Inactive)),
            detectWithStatus(DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE)
        )
    }

    @Test
    fun `unsupported encryption is found as unsupported`() {
        assertEquals(
            DetectResult.Found(EncryptionState(EncryptionState.Status.Unsupported)),
            detectWithStatus(DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED)
        )
    }

    @Test
    fun `transitioning status falls into the unknown bucket like the old else branch`() {
        assertEquals(
            DetectResult.Found(EncryptionState(EncryptionState.Status.Unknown)),
            detectWithStatus(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING)
        )
    }
}
