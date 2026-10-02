package net.imknown.android.forefrontinfo.ui.common

import com.topjohnwu.superuser.Shell
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import net.imknown.android.forefrontinfo.base.shell.IShell
import net.imknown.android.forefrontinfo.base.shell.ShellResult

// The libsu implementation of IShell: the object contributes the IShell binding to
// AppScope directly -- Metro does no constructor injection on objects, so the
// singleton comes for free (the companion action of issues-cn #02). It lives in
// :app's ui/common instead of :base: :base holds interfaces only, keeping the
// interface module free of a Metro dependency.
@ContributesBinding(AppScope::class, binding<IShell>())
object ShellLibSu : IShell {
    override fun execute(cmd: String): ShellResult {
        val result = Shell.cmd(cmd).exec()
        return ShellResult(result.out, result.isSuccess, result.code)
    }
}