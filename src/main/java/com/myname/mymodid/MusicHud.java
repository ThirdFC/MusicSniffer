package com.myname.mymodid;

import java.awt.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class MusicHud extends Gui {

    // --- 变量定义 ---
    private Minecraft mc;
    private RenderItem itemRender;

    // 歌名存储
    private String currentSong = "";
    private String lastSong = "";

    // 线程通信变量 (volatile 保证线程安全)
    private static volatile String pendingSong = "";

    // 动画控制
    private long notificationTime = 0L; // 弹窗开始的时间戳
    private boolean showing = false; // 当前是否正在显示

    // 构造函数
    public MusicHud() {
        this.mc = Minecraft.getMinecraft();
        this.itemRender = new RenderItem(); // 初始化物品渲染器
        startPollingThread(); // 启动后台检测线程
    }

    // --- 🚀 后台线程：防止游戏卡顿的核心 ---
    private void startPollingThread() {
        Thread poller = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        // 1. 调用 MusicSniffer 去问 Windows (耗时操作)
                        String result = MusicSniffer.getPlayingMusic();

                        // 2. 如果抓到了有效的歌名，存入待处理变量
                        if (result != null && !result.isEmpty()) {
                            pendingSong = result;
                        }

                        // 3. 休息 1 秒再检测下次
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        // 设置为守护线程，游戏关闭时它会自动停止
        poller.setDaemon(true);
        poller.setName("MusicSniffer-Thread");
        poller.start();
    }

    // --- 🎨 渲染部分：动态宽度黑卡片 (已修复层级遮挡) ---
    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event) {
        // 🛠️ 修复点1：改为在 TEXT 层渲染，比 HELMET 更晚，能覆盖经验条等
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;

        // 如果没有触发显示，直接跳过
        if (!showing) return;

        // 1. 计算动画进度
        // 使用 Config 中的 displayDuration 来计算进度
        double duration = (double) ModConfig.displayDuration;
        double timeElapsed = (System.currentTimeMillis() - notificationTime) / duration;

        // 如果时间到了，停止显示
        if (timeElapsed < 0.0D || timeElapsed > 1.0D) {
            showing = false;
            return;
        }

        // 2. 计算弹性滑入滑出动画 (原版成就动画算法)
        double scale = timeElapsed * 2.0D;
        if (scale > 1.0D) scale = 2.0D - scale;
        scale *= 4.0D;
        scale = 1.0D - scale;
        if (scale < 0.0D) scale = 0.0D;
        scale *= scale;
        scale *= scale;

        // Y轴位置 (从屏幕顶部滑下来 32 像素)
        int yPos = (int) (0 - 32 * scale);

        // 3. 智能计算卡片宽度
        int songWidth = this.mc.fontRenderer.getStringWidth(this.currentSong);
        // 卡片总宽 = 文字宽 + 50像素(图标+边距)，且最小宽度为120
        int boxWidth = Math.max(120, songWidth + 50);

        // X轴位置 (屏幕总宽 - 卡片宽 -> 靠右对齐)
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int xPos = res.getScaledWidth() - boxWidth;

        // --- 开始绘制图形 ---

        GL11.glPushMatrix(); // 🛠️ 修复点2：保存状态，防止影响其他UI
        GL11.glTranslated(0, 0, 100); // 🛠️ 修复点3：Z轴垫高100，强行覆盖在小地图上面

        // A. 画背景框 (半透明黑色 0xCC000000)
        drawRect(xPos, yPos, res.getScaledWidth(), yPos + 32, 0xCC000000);

        // B. 画底部装饰线条
        int lineColor = 0xFF55FF55; // 默认绿色

        // 如果配置里开启了彩虹模式
        if (ModConfig.enableRainbow) {
            long time = System.currentTimeMillis();
            // HSB算法生成彩虹色 (2秒转一圈)
            int rainbow = Color.HSBtoRGB((time % 2000L) / 2000.0f, 0.8f, 1.0f);
            lineColor = 0xFF000000 | rainbow; // 加上 Alpha 通道 (不透明)
        }

        // 画线 (高度为1像素)
        drawRect(xPos, yPos + 31, res.getScaledWidth(), yPos + 32, lineColor);

        // C. 画文字
        // "Now Playing" 小标题 (灰色，支持汉化)
        this.mc.fontRenderer
            .drawString(StatCollector.translateToLocal("hud.now_playing"), xPos + 30, yPos + 4, 0xAAAAAA);

        // 歌名 (白色，完整显示)
        this.mc.fontRenderer.drawString(this.currentSong, xPos + 30, yPos + 16, 0xFFFFFF);

        // D. 画图标 (唱片)
        // 开启物品渲染光照模式
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glEnable(GL11.GL_LIGHTING);

        // 在左侧画一个 "C418 - 13" 唱片
        this.itemRender.renderItemAndEffectIntoGUI(
            this.mc.fontRenderer,
            this.mc.getTextureManager(),
            new ItemStack(Items.record_13),
            xPos + 6,
            yPos + 8);

        // 关闭光照，恢复环境
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        GL11.glPopMatrix(); // 🛠️ 修复点4：恢复状态，避免后续渲染错乱
    }

    // --- 🔄 逻辑更新部分 ---
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        // 从后台线程获取最新结果
        String newSong = pendingSong;

        // 如果发现歌名变了，触发弹窗
        if (newSong != null && !newSong.isEmpty() && !newSong.equals(lastSong)) {
            triggerNotification(newSong);
        }
    }

    // 触发新通知的方法
    public void triggerNotification(String newSongName) {
        this.currentSong = newSongName;
        this.lastSong = newSongName;
        // 重置开始时间为当前时间，触发动画
        this.notificationTime = System.currentTimeMillis();
        this.showing = true;
    }
}
