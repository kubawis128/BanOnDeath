package io.github.kubawis128;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class main extends JavaPlugin implements Listener {
    Plugin NoKaut;
        @Override
    public void onEnable() {
        //Plugin NoKaut = Bukkit.getPluginManager().getPlugin("nokaut"); //próbowałem hook
        Bukkit.getPluginManager().registerEvents(this, this);
            try {
            if (!getDataFolder().isDirectory() && !getDataFolder().mkdirs()) {
                getLogger().warning(Messages.CREATE_FOLDER_FAILED_LOG);
            }
            bansFile = new File(getDataFolder(), "bans.lst");
            banMap = new HashMap<>();

            loadBans();

            getServer().getPluginManager().registerEvents(this, this);
        } catch (
    IOException e) {
        getLogger().log(Level.SEVERE, Messages.LOAD_FAILED_LOG, e);
        getServer().getPluginManager().disablePlugin(this);
    } catch (Exception e) {
                getLogger().log(Level.SEVERE, Messages.STARTUP_EXCEPTION_LOG, e);
                getServer().getPluginManager().disablePlugin(this);
            }
    }
    @Override
    public void onDisable() {
        saveBans();

        bansFile = null;
        banMap = null;

        HandlerList.unregisterAll((JavaPlugin) this);
    }
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
            //if(NoKaut.getConfig().getString("DeathOnEnd").equals("true"))
            //{
                UUID idd = event.getEntity().getPlayer().getUniqueId();
                long ONE_MINUTE_IN_MILLIS=60000;//millisecs

                Calendar date = Calendar.getInstance();
                long t = date.getTimeInMillis();
                Date tenplus = new Date(t + (10 * ONE_MINUTE_IN_MILLIS));
                banPlayer(idd,tenplus,ChatColor.RED + "Zginołeś \n(Ban: 10min)","DEAD");
                //Bukkit.broadcastMessage(ChatColor.RED + "TEST: SOMEONE IS DED");
            //}

    }
    private File bansFile;
    private Map<UUID, BanEntry> banMap;
    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        BanEntry ban = banMap.get(uuid);
        if (ban != null) {
            if (ban.isActive()) {
                e.disallow(PlayerLoginEvent.Result.KICK_BANNED, Messages.getBannedMessage(ban));
            } else {
                banMap.remove(ban.getPlayerUUID());
                getLogger().info(() -> Messages.getBanExpiredLog(uuid));
                e.getPlayer().sendMessage(Messages.BAN_EXPIRED_MESSAGE);
                saveBans();
            }
        }
    }

    public UUID getUUIDForPlayer(String id) {
        Player p = getServer().getPlayer(id);
        if (p != null) {
            return p.getUniqueId();
        } else {
            try {
                return UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public void kickPlayers(UUID id, String reason, String source) {
        Player p = getServer().getPlayer(id);
        if (p != null) {
            p.kickPlayer(ChatColor.RED + reason);
        }
        getLogger().info(() -> Messages.getKickLog(id, reason, source));
    }

    public void banPlayer(UUID id, Date endTime, String reason, String source) {
        BanEntry ban = new BanEntry(id, endTime, reason);
        banMap.put(id, ban);

        kickPlayers(id, reason, source);
        saveBans();
        getLogger().info(() -> Messages.getBanLog(ban, source));
    }

    public void pardonPlayer(UUID id, String source) {
        banMap.remove(id);
        saveBans();
        getLogger().info(() -> Messages.getPardonLog(id, source));
    }

    private void loadBans() throws IOException {
        if (bansFile.isFile()) {
            try (Stream<String> lines = Files.lines(bansFile.toPath())) {
                lines.forEach(this::loadBanEntry);
            }
        } else {
            getLogger().warning(Messages.BAN_FILE_MISSING_LOG);
        }
    }

    private void loadBanEntry(String line) {
        try {
            BanEntry ban = BanEntry.parse(line);
            banMap.put(ban.getPlayerUUID(), ban);
        } catch (IllegalArgumentException e) {
            getLogger().log(Level.SEVERE, Messages.getLoadBanFailedLog(line), e);
        }
    }

    private void saveBans() {
        try (Stream<String> stream = banMap.values().stream().map(Object::toString)) {
            Files.write(bansFile.toPath(), (Iterable<String>) stream::iterator);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, Messages.SAVE_FAILED_LOG, e);
        }
    }

    public BanEntry getBanEntry(UUID uuid) {
        return banMap.get(uuid);
    }
}
