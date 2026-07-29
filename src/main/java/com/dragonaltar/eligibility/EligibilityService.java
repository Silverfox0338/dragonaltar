package com.dragonaltar.eligibility;

import com.dragonaltar.soul.DragonSoulService;
import org.bukkit.GameMode;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import com.dragonaltar.config.ConfigService;
import org.bukkit.entity.Player;

import java.util.*;

public final class EligibilityService {
    private final ConfigService configs;
    private final DragonSoulService souls;
    private final Map<UUID,Long> joinedAt=new HashMap<>();
    public EligibilityService(ConfigService configs, DragonSoulService souls) { this.configs=configs;this.souls=souls; }

    public Result check(Player player) {
        FileConfiguration config=configs.general();List<String> modes = config.getStringList("eligibility.allowed-game-modes");
        String required = config.getString("eligibility.required-permission", "");
        String excluded = config.getString("eligibility.exclusion-permission", "dragonaltar.eligibility.excluded");
        long grace=config.getLong("eligibility.recently-joined-grace-seconds",0)*1000L;
        return EligibilityRules.evaluate(new EligibilitySnapshot(player.isOnline(),modes.isEmpty()||modes.contains(player.getGameMode().name()),player.getStatistic(Statistic.PLAY_ONE_MINUTE)>=config.getLong("eligibility.minimum-playtime-ticks",0),required==null||required.isBlank()||player.hasPermission(required),excluded==null||excluded.isBlank()||!player.hasPermission(excluded),souls.byHolder(player.getUniqueId()).isEmpty(),!metadataTrue(player,"afk")&&!metadataTrue(player,"essentials_afk"),!metadataTrue(player,"vanished"),!player.isDead()&&player.getGameMode()!=GameMode.SPECTATOR,System.currentTimeMillis()-joinedAt.getOrDefault(player.getUniqueId(),0L)>=grace));
    }
    public void markJoined(Player player){joinedAt.put(player.getUniqueId(),System.currentTimeMillis());}
    public List<Player> eligible(Collection<? extends Player> players) { return players.stream().filter(p -> check(p).eligible()).map(Player.class::cast).toList(); }
    public record Result(boolean eligible, Map<String, Boolean> checks) { public Result { checks = Map.copyOf(checks); } }
    private static boolean metadataTrue(Player player,String key){return player.hasMetadata(key)&&player.getMetadata(key).stream().anyMatch(value->value.asBoolean());}
}
