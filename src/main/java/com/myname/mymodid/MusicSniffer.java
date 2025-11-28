package com.myname.mymodid;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

public class MusicSniffer {

    // 黑名单：过滤掉不是音乐播放器的窗口
    // private static final String[] BLACKLIST = {
    // "Minecraft", "Chrome", "Firefox", "Edge", "Explorer", "Task Manager", "Visual Studio Code"
    // };

    public static String getPlayingMusic() {
        final String[] result = { "" }; // 用数组存结果，方便在内部类修改

        // 调用 Windows API 遍历所有窗口
        User32.INSTANCE.EnumWindows(new User32.WNDENUMPROC() {

            @Override
            // ⬇️ 修复点：第二个参数必须是 Pointer 类型
            public boolean callback(HWND hWnd, Pointer data) {
                // 1. 获取窗口标题长度
                int length = User32.INSTANCE.GetWindowTextLength(hWnd) + 1;
                char[] buffer = new char[length];

                // 2. 获取标题内容
                User32.INSTANCE.GetWindowText(hWnd, buffer, length);
                String title = Native.toString(buffer);

                // 3. 过滤逻辑
                if (isMusicTitle(title)) {
                    result[0] = title;
                    return false; // 找到了，停止遍历
                }
                return true; // 继续找下一个
            }
        }, null);

        return result[0];
    }

    // 判断逻辑
    private static boolean isMusicTitle(String title) {
        if (title.isEmpty()) return false;
        if (!title.contains(" - ")) return false;

        // --- 使用配置文件的黑名单 ---
        for (String black : ModConfig.blacklist) {
            if (title.contains(black)) return false;
        }

        if (title.contains("1.7.10")) return false;
        return true;
    }
}
