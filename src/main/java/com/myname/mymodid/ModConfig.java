package com.myname.mymodid;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class ModConfig {

    public static Configuration config;

    public static int displayDuration = 3000; // 显示时长 (毫秒)
    public static boolean enableRainbow = false; // 是否开启彩虹底条
    public static String[] blacklist = { "Minecraft", "Task Manager", "Explorer", "Chrome", "Edge", "Firefox",
        "Visual Studio Code", "Discord", "Notepad", "文件资源管理器" };

    // 初始化配置
    public static void init(File configFile) {
        if (config == null) {
            config = new Configuration(configFile);
            loadConfig();
        }
    }

    // 读取并同步配置
    public static void loadConfig() {
        // 1. 读取显示时间
        displayDuration = config
            .getInt("DisplayDuration", Configuration.CATEGORY_GENERAL, 3000, 1000, 10000, "HUD显示的时长(毫秒)");

        // 2. 读取黑名单
        blacklist = config.getStringList("Blacklist", Configuration.CATEGORY_GENERAL, blacklist, "忽略的窗口标题关键字");

        // 3. 读取彩虹条开关
        enableRainbow = config.getBoolean("RainbowMode", Configuration.CATEGORY_GENERAL, false, "开启底部线条彩虹色");

        if (config.hasChanged()) {
            config.save();
        }
    }
}
