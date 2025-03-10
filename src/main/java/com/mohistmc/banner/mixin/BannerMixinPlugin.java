package com.mohistmc.banner.mixin;

import com.google.common.collect.ImmutableSet;
import com.mohistmc.banner.config.BannerConfig;
import com.mohistmc.banner.library.Library;
import com.mohistmc.banner.library.LibraryManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.*;

import static com.mohistmc.banner.library.LibraryManager.HashAlgorithm.MD5;

public class BannerMixinPlugin implements IMixinConfigPlugin {

    private final Logger LOGGER = LogManager.getLogger("Mohist Banner");
    private static final String MOHIST = "https://maven.mohistmc.com/libraries";
    private static final String CHINA = "http://s1.devicloud.cn:25119/libraries";

    @Override
    public void onLoad(String mixinPackage) {
        try {
            BannerConfig.setup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("Loading libraries, please wait...");
        loadLibs();
    }

    public static void loadLibs() {
        List<Library> libraries = new ArrayList<>();
        libraries.add(new Library("org.yaml", "snakeyaml", "1.33", MD5, "e0164a637c691c8cf01d29f90a709c02"));
        libraries.add(new Library("org.apache.maven", "maven-resolver-provider", "3.8.5", MD5, "9021a5ebbabc4a591bab0331589f6614"));

        libraries.add(new Library("org.apache.maven.resolver", "maven-resolver-connector-basic", "1.7.3", MD5, "c73f00574fa73f7d1c0842050abf765a"));
        libraries.add(new Library("org.apache.maven.resolver", "maven-resolver-transport-http", "1.7.3", MD5, "da0fb93034859a03f9e7baf4215e4bec"));
        libraries.add(new Library("org.fusesource.jansi", "jansi", "1.18", MD5, "6ee32de8880da9f02552474f60ab6fbd"));
        libraries.add(new Library("jline", "jline", "2.14.6", MD5, "480423551649bc6980b43f09e4717272"));
        libraries.add(new Library("com.googlecode.json-simple", "json-simple", "1.1.1", MD5, "5cc2c478d73e8454b4c369cee66c5bc7"));
        libraries.add(new Library("org.xerial", "sqlite-jdbc", "3.41.0.0", MD5, "0d63ee5b583e9a75ea1717ffce63fed8"));
        libraries.add(new Library("com.mysql", "mysql-connector-j", "8.0.32", MD5, "25bf3b3cd262065283962078dc82e99c"));
        libraries.add(new Library("net.md-5", "SpecialSource", "1.11.0", MD5, "815529d90faff79cb61c12f47a4259b5"));
        libraries.add(new Library("net.md-5", "bungeecord-chat", "1.16-R0.4", MD5, "bf6464395a0951675c3bca47d3e1a13a"));
        libraries.add(new Library("io.izzel", "tools", "1.3.0", MD5, "440242a418ab84713632b0026144eea4"));
        libraries.add(new Library("com.mohistmc.remapper", "mohistremapper", "0.3", MD5, "327e55217d11b8c93c1f4bec14ddb3a3"));
        libraries.add(new Library("com.mohistmc", "dynamicenum", "0.1", MD5, "ae876076bdcc7ee5673b445c313d3be5"));
        libraries.add(new Library("com.mohistmc", "i18n", "0.2", MD5, "956a8d30a6e464eef86027fb74fd7177"));
        libraries.add(new Library("org.apache.logging.log4j", "log4j-iostreams", "2.20.0", MD5, "d612855f572573a409bb59957c7c24c8"));
        libraries.add(new Library("commons-io", "commons-io", "2.11.0", MD5, "3b4b7ccfaeceeac240b804839ee1a1ca"));
        libraries.add(new Library("commons-lang", "commons-lang", "2.6-mohist", MD5, "755f5cdb5de00d072215340e8370daff"));
        new LibraryManager(getRepository(), "banner-libs", true, 6, libraries).run();
    }

    private static String getRepository() {
        return isCN() ? CHINA : MOHIST;
    }

    public static boolean isCN() {
        TimeZone timeZone = TimeZone.getDefault();
        return "Asia/Shanghai".equals(timeZone.getID());
    }

    private final Set<String> modifyConstructor = ImmutableSet.<String>builder()
            .add("net.minecraft.world.level.Level")
            .add("net.minecraft.server.level.ServerLevel")
            .add("net.minecraft.world.SimpleContainer")
            .add("net.minecraft.world.level.block.ComposterBlock")
            .add("net.minecraft.world.level.block.ComposterBlock$EmptyContainer")
            .add("net.minecraft.world.food.FoodData")
            .add("net.minecraft.world.inventory.CraftingContainer")
            .add("net.minecraft.world.inventory.PlayerEnderChestContainer")
            .add("net.minecraft.world.item.trading.MerchantOffer")
            .add("net.minecraft.world.inventory.LecternMenu")
            .add("net.minecraft.server.level.ServerEntity")
            .add("net.minecraft.network.protocol.game.ServerboundContainerClosePacket")
            .add("net.minecraft.network.chat.TextColor")
            .add("net.minecraft.commands.Commands")
            .add("net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess")
            .add("net.minecraft.network.protocol.game.ClientboundSystemChatPacket")
            .add("net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket")
            .build();

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        modifyConstructor(targetClassName, targetClass);
    }

    private void modifyConstructor(String targetClassName, ClassNode classNode) {
        if (modifyConstructor.contains(targetClassName)) {
            Set<String> presentCtor = new HashSet<>();
            Set<String> overrideCtor = new HashSet<>();
            for (MethodNode method : classNode.methods) {
                if (method.name.equals("<init>")) {
                    presentCtor.add(method.desc);
                }
                if (method.name.equals("banner$constructor$override")) {
                    overrideCtor.add(method.desc);
                }
            }
            ListIterator<MethodNode> iterator = classNode.methods.listIterator();
            while (iterator.hasNext()) {
                MethodNode methodNode = iterator.next();
                if (methodNode.name.equals("banner$constructor")) {
                    String desc = methodNode.desc;
                    if (presentCtor.contains(desc)) {
                        iterator.remove();
                    } else {
                        methodNode.name = "<init>";
                        presentCtor.add(methodNode.desc);
                        remapCtor(classNode, methodNode);
                    }
                }
                if (methodNode.name.equals("banner$constructor$super")) {
                    iterator.remove();
                }
                if (methodNode.name.equals("<init>") && overrideCtor.contains(methodNode.desc)) {
                    iterator.remove();
                } else if (methodNode.name.equals("banner$constructor$override")) {
                    methodNode.name = "<init>";
                    remapCtor(classNode, methodNode);
                }
            }
        }
    }

    private void remapCtor(ClassNode classNode, MethodNode methodNode) {
        boolean initialized = false;
        for (AbstractInsnNode node : methodNode.instructions) {
            if (node instanceof MethodInsnNode methodInsnNode) {
                if (methodInsnNode.name.equals("banner$constructor")) {
                    if (initialized) {
                        throw new ClassFormatError("Duplicate constructor call");
                    } else {
                        methodInsnNode.setOpcode(Opcodes.INVOKESPECIAL);
                        methodInsnNode.name = "<init>";
                        initialized = true;
                    }
                }
                if (methodInsnNode.name.equals("banner$constructor$super")) {
                    if (initialized) {
                        throw new ClassFormatError("Duplicate constructor call");
                    } else {
                        methodInsnNode.setOpcode(Opcodes.INVOKESPECIAL);
                        methodInsnNode.owner = classNode.superName;
                        methodInsnNode.name = "<init>";
                        initialized = true;
                    }
                }
            }
        }
        if (!initialized) {
            if (classNode.superName.equals("java/lang/Object")) {
                InsnList insnList = new InsnList();
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
                methodNode.instructions.insert(insnList);
            } else {
                throw new ClassFormatError("No super constructor call present: " + classNode.name);
            }
        }
    }
}
