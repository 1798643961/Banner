package com.mohistmc.banner.api;

import com.mohistmc.banner.util.ServerUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServerAPI {

    public static HashSet<String> modlists_Client = new HashSet<>();
    public static HashSet<String> modlists_Server = new HashSet<>();
    public static Set<String> modlists_Inside = Set.of("minecraft", "forge", "mohist");
    public static HashSet<String> modlists_All = new HashSet<>();

    public static HashSet<String> channels = new HashSet<>();
    public static Map<String, String> forgecmdper = new ConcurrentHashMap<>();
    public static List<Command> forgecmd = new ArrayList<>();
    public static Map<net.minecraft.world.entity.EntityType<?>, String> entityTypeMap = new ConcurrentHashMap<>();

    static {
        for (ModContainer modInfo : FabricLoader.getInstance().getAllMods()) {
            modlists_All.add(modInfo.getMetadata().getId());
            if (modInfo.getMetadata().getEnvironment() == ModEnvironment.SERVER) {
                modlists_Client.add(modInfo.getMetadata().getId());
            } else if (modInfo.getMetadata().getEnvironment() == ModEnvironment.SERVER) {
                modlists_Server.add(modInfo.getMetadata().getId());
            }
        }
    }

    // Don't count the default number of mods
    public static int getModSize() {
        return FabricLoader.getInstance().getAllMods().size();
    }

    public static Boolean hasMod(String modid) {
        return modlists_All.contains(modid);
    }

    public static Boolean hasPlugin(String pluginname) {
        return Bukkit.getPluginManager().getPlugin(pluginname) != null;
    }

    public static void putBukkitEvents(Listener listener, Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    public static MinecraftServer getNMSServer() {
        return ServerUtils.getServer();
    }
}
