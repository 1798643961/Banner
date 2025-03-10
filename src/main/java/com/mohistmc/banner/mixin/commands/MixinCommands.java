package com.mohistmc.banner.mixin.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.mohistmc.banner.injection.commands.InjectionCommands;
import com.mohistmc.banner.util.BukkitDispatcher;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.v1_19_R3.CraftServer;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.spongepowered.asm.mixin.*;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

// TODO fix inject methods
@Mixin(Commands.class)
public abstract class MixinCommands implements InjectionCommands {

    @Mutable
    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;

    @Shadow public abstract int performPrefixedCommand(CommandSourceStack source, String command);

    @Shadow public abstract int performCommand(ParseResults<CommandSourceStack> parseResults, String command);

    @Shadow protected abstract void fillUsableCommands(CommandNode<CommandSourceStack> rootCommandSource, CommandNode<SharedSuggestionProvider> rootSuggestion, CommandSourceStack source, Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> commandNodeToSuggestionNode);

    public void banner$constructor() {
        this.dispatcher = new BukkitDispatcher((Commands) (Object) this);
        this.dispatcher.setConsumer((context, b, i) -> context.getSource().onCommandComplete(context, b, i));
    }

    @Override
    public int performPrefixedCommand(CommandSourceStack commandlistenerwrapper, String s, String label) {
        return this.performPrefixedCommand(commandlistenerwrapper, s);
    }

    @Override
    public int performCommand(ParseResults<CommandSourceStack> parseresults, String s, String label) {
        return this.performCommand(parseresults, s);
    }

    @Override
    public int dispatchServerCommand(CommandSourceStack sender, String command) {
        Joiner joiner = Joiner.on(" ");
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        ServerCommandEvent event = new ServerCommandEvent(sender.getBukkitSender(), command);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return 0;
        }
        command = event.getCommand();

        String[] args = command.split(" ");

        String cmd = args[0];
        if (cmd.startsWith("minecraft:")) cmd = cmd.substring("minecraft:".length());
        if (cmd.startsWith("bukkit:")) cmd = cmd.substring("bukkit:".length());

        // Block disallowed commands
        if (cmd.equalsIgnoreCase("stop") || cmd.equalsIgnoreCase("kick") || cmd.equalsIgnoreCase("op")
                || cmd.equalsIgnoreCase("deop") || cmd.equalsIgnoreCase("ban") || cmd.equalsIgnoreCase("ban-ip")
                || cmd.equalsIgnoreCase("pardon") || cmd.equalsIgnoreCase("pardon-ip") || cmd.equalsIgnoreCase("reload")) {
            return 0;
        }

        // Handle vanilla commands;
        if (sender.getLevel().getCraftServer().getCommandBlockOverride(args[0])) {
            args[0] = "minecraft:" + args[0];
        }

        String newCommand = joiner.join(args);
        return this.performPrefixedCommand(sender, newCommand, newCommand);
    }

    /**
     * @author wdog5
     * @reason PlayerCommandSendEvent
     */
    @Overwrite
    public void sendCommands(ServerPlayer player) {
        Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> map = Maps.newIdentityHashMap();
        RootCommandNode vanillaRoot = new RootCommandNode();

        RootCommandNode<CommandSourceStack> vanilla = CraftServer.vanillaCommandManager.getDispatcher().getRoot();
        map.put(vanilla, vanillaRoot);
        this.fillUsableCommands(vanilla, vanillaRoot, player.createCommandSourceStack(), (Map) map);

        RootCommandNode<SharedSuggestionProvider> rootCommandNode = new RootCommandNode();
        map.put(this.dispatcher.getRoot(), rootCommandNode);
        this.fillUsableCommands(this.dispatcher.getRoot(), rootCommandNode, player.createCommandSourceStack(), map);

        Collection<String> bukkit = new LinkedHashSet<>();
        for (CommandNode node : rootCommandNode.getChildren()) {
            bukkit.add(node.getName());
        }

        PlayerCommandSendEvent event = new PlayerCommandSendEvent(player.getBukkitEntity(), new LinkedHashSet<>(bukkit));
        event.getPlayer().getServer().getPluginManager().callEvent(event);

        // Remove labels that were removed during the event
        for (String orig : bukkit) {
            if (!event.getCommands().contains(orig)) {
                //CommandNodeHooks.removeCommand(rootCommandNode, orig);
            }
        }
        player.connection.send(new ClientboundCommandsPacket(rootCommandNode));
    }

    /**
    @Redirect(method = "fillUsableCommands", at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/brigadier/tree/CommandNode;canUse(Ljava/lang/Object;)Z"))
    private <S> boolean banner$canUse(CommandNode<S> commandNode, S source) {
        return CommandNodeHooks.canUse(commandNode, source);
    }*/
}
