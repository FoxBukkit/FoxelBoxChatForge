package com.foxelbox.chat;

import com.foxelbox.dependencies.config.Configuration;
import com.foxelbox.dependencies.redis.RedisManager;
import com.foxelbox.dependencies.threading.SimpleThreadCreator;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

@Mod(modid = FoxelBoxChatMod.MODID, name = "FoxelBoxChat", version = FoxelBoxChatMod.VERSION, serverSideOnly = true, acceptableRemoteVersions="*")
public class FoxelBoxChatMod
{
    public static final String MODID = "foxelboxchat";
    public static final String VERSION = "1.0";

    private final Queue<Runnable> nextTickQueue = new LinkedBlockingQueue<>();

    public void runNextTick(Runnable runnable) {
        synchronized (nextTickQueue) {
            nextTickQueue.add(runnable);
        }
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event) {
        getDataFolder().mkdirs();
        configuration = new Configuration(getDataFolder());
        redisManager = new RedisManager(new SimpleThreadCreator(), configuration);
        playerHelper = new PlayerHelper(this);
        redisHandler = new RedisHandler(this);

        loadRedisCommands();

        MinecraftForge.EVENT_BUS.register(new FBChatListener());

        final ICommandManager realManager = MinecraftServer.getServer().getCommandManager();

        Utils.setPrivateValueByType(MinecraftServer.class, MinecraftServer.getServer(), ICommandManager.class, new CommandHandler() {
            @Override
            public int executeCommand(ICommandSender sender, String rawCommand) {
                if (sender instanceof EntityPlayerMP) {
                    final EntityPlayerMP ply = (EntityPlayerMP) sender;
                    final String baseCmd = rawCommand.substring(1).trim();

                    int posSpace = baseCmd.indexOf(' ');
                    String cmd;
                    String argStr;
                    if (posSpace < 0) {
                        cmd = baseCmd.toLowerCase();
                        argStr = "";
                    } else {
                        cmd = baseCmd.substring(0, posSpace).trim().toLowerCase();
                        argStr = baseCmd.substring(posSpace).trim();
                    }

                    if (redisCommands.contains(cmd)) {
                        redisHandler.sendMessage(ply, "/" + cmd + " " + argStr);
                        return 0;
                    }
                }

                return realManager.executeCommand(sender, rawCommand);
            }

            @Override
            public List getTabCompletionOptions(ICommandSender sender, String input, BlockPos pos) {
                return realManager.getTabCompletionOptions(sender, input, pos);
            }

            @Override
            public List getPossibleCommands(ICommandSender sender) {
                return realManager.getPossibleCommands(sender);
            }

            @Override
            public Map getCommands() {
                return realManager.getCommands();
            }
        });
    }

    @EventHandler
    public void serverTick(TickEvent.ServerTickEvent event) {
        synchronized (nextTickQueue) {
            while (!nextTickQueue.isEmpty()) {
                nextTickQueue.poll().run();
            }
        }
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        playerHelper.refreshPlayerListRedis(null);
    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new ICommand() {
            @Override
            public String getName() {
                return "reloadclcommands";
            }

            @Override
            public String getCommandUsage(ICommandSender sender) {
                return "reloadclcommands";
            }

            @Override
            public List getAliases() {
                return new ArrayList();
            }

            @Override
            public void execute(ICommandSender sender, String[] args) throws CommandException {
                loadRedisCommands();
            }

            @Override
            public boolean canCommandSenderUse(ICommandSender sender) {
                return !(sender instanceof EntityPlayerMP);
            }

            @Override
            public List addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
                return null;
            }

            @Override
            public boolean isUsernameIndex(String[] args, int index) {
                return false;
            }

            @Override
            public int compareTo(Object o) {
                return 0;
            }
        });
    }

    HashSet<UUID> registeredPlayers = new HashSet<>();

    public Configuration configuration;
    public RedisManager redisManager;
    public RedisHandler redisHandler;
    public PlayerHelper playerHelper;

    public String getPlayerNick(EntityPlayerMP ply) {
        return getPlayerNick(EntityPlayerMP.getUUID(ply.getGameProfile()));
    }

    public String getPlayerNick(UUID uuid) {
        return playerHelper.playerNicks.get(uuid.toString());
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }

    public RedisHandler getRedisHandler() {
        return redisHandler;
    }

    private final HashSet<String> redisCommands = new HashSet<>();
    public void loadRedisCommands() {
        Set<String> commands = redisManager.smembers("chatLinkCommands");
        synchronized (redisCommands) {
            redisCommands.clear();
            for (String str : commands)
                redisCommands.add(str);
        }
    }

    public File getDataFolder() {
        return new File("config/FoxelBoxChat");
    }

    public void onDisable() {
        redisManager.stop();
    }

    class FBChatListener {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onPlayerCommandPreprocess(ServerChatEvent event) {
            event.setCanceled(true);
            redisHandler.sendMessage(event.player, event.message);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onPlayerJoin(FMLNetworkEvent.ServerConnectionFromClientEvent event) {
            final EntityPlayerMP player = ((NetHandlerPlayServer)event.handler).playerEntity;
            playerHelper.refreshUUID(player);
            playerHelper.refreshPlayerListRedis(null);
            if(registeredPlayers.add(EntityPlayerMP.getUUID(player.getGameProfile()))) {
                redisHandler.sendMessage(player, "join", "playerstate");
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onPlayerQuit(FMLNetworkEvent.ServerDisconnectionFromClientEvent event) {
            final EntityPlayerMP player = ((NetHandlerPlayServer)event.handler).playerEntity;
            playerHelper.refreshPlayerListRedis(player);
            if(registeredPlayers.add(EntityPlayerMP.getUUID(player.getGameProfile()))) {
                redisHandler.sendMessage(player, "quit", "playerstate");
            }
        }
    }
}
