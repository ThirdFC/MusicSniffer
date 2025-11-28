package com.myname.mymodid;

import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

// 1. 确认 guiFactory 指向正确的包名 (com.myname.mymodid)
@Mod(modid = "musicsniffer", version = "1.0", name = "MusicSniffer", guiFactory = "com.myname.mymodid.ConfigGuiFactory")
public class MyMod {

    public static final Logger LOG = LogManager.getLogger("MyMod");

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(event.getSuggestedConfigurationFile());
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide()
            .isClient()) {
            MusicHud hud = new MusicHud();
            MinecraftForge.EVENT_BUS.register(hud);
            FMLCommonHandler.instance()
                .bus()
                .register(hud);
            FMLCommonHandler.instance()
                .bus()
                .register(this);
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        // 2. 修复点：这里必须判断 "musicsniffer"，要和 @Mod 里的 modid 完全一致！
        // 之前写的是 "mymodid"，所以保存配置没反应
        if (event.modID.equals("musicsniffer")) {
            ModConfig.loadConfig();
            LOG.info("Configuration reloaded!");
        }
    }
}
