package net.easecation.ghosty;

import cn.nukkit.Player;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDespawnEvent;
import cn.nukkit.event.entity.EntitySpawnEvent;
import cn.nukkit.event.level.ChunkUnloadEvent;
import cn.nukkit.event.level.WeatherChangeEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.event.server.DataPacketSendEvent;
import cn.nukkit.level.Level;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Utils;
import net.easecation.ghosty.entity.PlaybackNPC;
import net.easecation.ghosty.recording.LevelRecordEngine;
import net.easecation.ghosty.recording.PlayerRecordEngine;
import net.easecation.ghosty.recording.player.PlayerRecord;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GhostyPlugin extends PluginBase implements Listener {

    private static GhostyPlugin instance;
    public static final boolean DEBUG_DUMP = false;
    public static final int DATA_SAVE_PROTOCOL = Utils.dynamic(ProtocolInfo.v1_20_0);

    /* 录制完成的成品 */
    private final List<PlayerRecord> playerRecords = new ArrayList<>();
    public final Map<Player, PlayerRecordEngine> recordingPlayerEngines = new HashMap<>();
    public final Map<Level, LevelRecordEngine> recordingLevelEngines = new HashMap<>();

    public static GhostyPlugin getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        InputStream skinStream = this.getResource("skin.png");
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(skinStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PlaybackNPC.defaultSkin = new Skin();
        if (bufferedImage != null) {
            PlaybackNPC.defaultSkin.setSkinData(bufferedImage);
        } else {
            PlaybackNPC.defaultSkin = Skin.NO_PERSONA_SKIN;
        }
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getScheduler().scheduleRepeatingTask(this, () -> {
            recordingPlayerEngines.entrySet().removeIf(e -> !e.getKey().isOnline() || e.getValue().isStopped());
            recordingLevelEngines.entrySet().removeIf(e -> e.getKey().getProvider() == null || !e.getValue().isRecording());
        }, 1);
        this.getLogger().notice("GhostyPlugin enabled!");
    }

    public List<PlayerRecord> getPlayerRecords() {
        return playerRecords;
    }

    @EventHandler
    public void onDataPacketSend(DataPacketSendEvent event) {
        PlayerRecordEngine engine = recordingPlayerEngines.get(event.getPlayer());
        if (engine != null) {
            engine.onPacketSendEvent(event.getPacket());
        }
    }

    @EventHandler
    public void onDataPacketReceive(DataPacketReceiveEvent event) {
        PlayerRecordEngine engine = recordingPlayerEngines.get(event.getPlayer());
        if (engine != null) {
            engine.onPacketReceiveEvent(event.getPacket());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitySpawn(EntitySpawnEvent event) {
        LevelRecordEngine levelRecordEngine = recordingLevelEngines.get(event.getEntity().getLevel());
        if (levelRecordEngine != null) {
            levelRecordEngine.onEntitySpawn(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDespawn(EntityDespawnEvent event) {
        LevelRecordEngine levelRecordEngine = recordingLevelEngines.get(event.getEntity().getLevel());
        if (levelRecordEngine != null) {
            levelRecordEngine.onEntityDespawn(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelWeatherChange(WeatherChangeEvent event) {
        if (event.isCancelled()) {
            return;
        }
        LevelRecordEngine levelRecordEngine = recordingLevelEngines.get(event.getLevel());
        if (levelRecordEngine != null) {
            levelRecordEngine.onWeatherChange(event.toWeatherState(), event.getIntensity());
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (event.getChunk().getEntities().values().stream().anyMatch(e -> e instanceof PlaybackNPC)) {
            event.setCancelled(true);
        }
    }

}
