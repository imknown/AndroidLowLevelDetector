package net.imknown.android.forefrontinfo.base;

import net.imknown.android.forefrontinfo.base.shell.ShellResult;

interface IUserService {
    void destroy() = 16777114; // Destroy method defined by Shizuku server

    void exit() = 1; // Exit method defined by user

    ShellResult execute(String command) = 2;
}