package dev.sizecraft.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.sizecraft.capture.CaptureManager
import dev.sizecraft.config.SizeCraftConfig
import dev.sizecraft.dimension.HammerspaceLayout
import dev.sizecraft.dimension.HammerspacePopulator
import dev.sizecraft.gui.HammerspaceViewMenu
import dev.sizecraft.player.SizeData
import dev.sizecraft.player.SizeDataAttachment
import dev.sizecraft.player.SizeEvents
import dev.sizecraft.registry.SizeCraftDimension
import dev.sizecraft.registry.SizeCraftPermissions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.server.permission.PermissionAPI

object SizeCraftCommand {

    fun onRegisterCommands(event: RegisterCommandsEvent) {
        val dispatcher: CommandDispatcher<CommandSourceStack> = event.dispatcher

        dispatcher.register(
            Commands.literal("sizecraft")
                .then(
                    Commands.argument("steps", DoubleArgumentType.doubleArg(-10.0, 10.0))
                        .executes { ctx -> setSelfSteps(ctx) }
                )
                .then(
                    Commands.literal("get")
                        .executes { ctx -> getSelf(ctx) }
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .requires { src -> hasPermission(src, SizeCraftPermissions.RESIZE_OTHERS) }
                                .executes { ctx -> getOther(ctx) }
                        )
                )
                .then(
                    Commands.literal("set")
                        .requires { src -> hasPermission(src, SizeCraftPermissions.RESIZE_OTHERS) }
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .then(
                                    Commands.argument("steps", DoubleArgumentType.doubleArg(-10.0, 10.0))
                                        .executes { ctx -> setSteps(ctx) }
                                )
                        )
                )
                .then(
                    Commands.literal("reset")
                        .executes { ctx -> resetSelf(ctx) }
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .requires { src -> hasPermission(src, SizeCraftPermissions.RESIZE_OTHERS) }
                                .executes { ctx -> resetOther(ctx) }
                        )
                )
                .then(
                    Commands.literal("min")
                        .requires { src -> hasPermission(src, SizeCraftPermissions.RESIZE_OTHERS) }
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .then(
                                    Commands.argument("steps", DoubleArgumentType.doubleArg(-10.0, 10.0))
                                        .executes { ctx -> setMin(ctx) }
                                )
                        )
                )
                .then(
                    Commands.literal("max")
                        .requires { src -> hasPermission(src, SizeCraftPermissions.RESIZE_OTHERS) }
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .then(
                                    Commands.argument("steps", DoubleArgumentType.doubleArg(-10.0, 10.0))
                                        .executes { ctx -> setMax(ctx) }
                                )
                        )
                )
                .then(
                    Commands.literal("predator")
                        .requires { src -> hasPermission(src, SizeCraftPermissions.PREDATOR) }
                        .executes { ctx -> togglePredator(ctx) }
                        .then(
                            Commands.argument("enabled", BoolArgumentType.bool())
                                .executes { ctx -> setPredator(ctx) }
                        )
                )
                .then(
                    Commands.literal("prey")
                        .requires { src -> hasPermission(src, SizeCraftPermissions.PREY) }
                        .executes { ctx -> togglePrey(ctx) }
                        .then(
                            Commands.argument("enabled", BoolArgumentType.bool())
                                .executes { ctx -> setPrey(ctx) }
                        )
                ),
        )

        dispatcher.register(
            Commands.literal("hammerspace")
                .then(
                    Commands.literal("enter")
                        .executes { ctx -> hammerspaceSelf(ctx) }
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .requires { src -> hasPermission(src, SizeCraftPermissions.DIMENSION_OTHERS) }
                                .executes { ctx -> hammerspaceVisit(ctx) }
                        )
                )
                .then(
                    Commands.literal("leave")
                        .executes { ctx -> hammerspaceReturnSelf(ctx) }
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .requires { src -> hasPermission(src, SizeCraftPermissions.DIMENSION_OTHERS) }
                                .executes { ctx -> hammerspaceReturnOther(ctx) }
                        )
                )
                .then(
                    Commands.literal("allowEscape")
                        .then(
                            Commands.argument("enabled", BoolArgumentType.bool())
                                .executes { ctx -> hammerspaceEscape(ctx) }
                        )
                )
                .then(
                    Commands.literal("preventEscapeForTicks")
                        .then(
                            Commands.argument("ticks", IntegerArgumentType.integer(0, 72000))
                                .executes { ctx -> hammerspaceDelay(ctx) }
                        )
                )
                // Compartment management
                .then(
                    Commands.literal("create")
                        .then(
                            Commands.argument("slotId", StringArgumentType.word())
                                .executes { ctx -> compartmentCreate(ctx) }
                        )
                )
                .then(
                    Commands.literal("delete")
                        .then(
                            Commands.argument("slotId", StringArgumentType.word())
                                .executes { ctx -> compartmentDelete(ctx) }
                        )
                )
                .then(
                    Commands.literal("stomach")
                        .then(
                            Commands.argument("slotId", StringArgumentType.word())
                                .executes { ctx -> compartmentSetStomach(ctx) }
                        )
                )
                .then(
                    Commands.literal("rename")
                        .then(
                            Commands.argument("slotId", StringArgumentType.word())
                                .then(
                                    Commands.argument("displayName", StringArgumentType.greedyString())
                                        .executes { ctx -> compartmentRename(ctx) }
                                )
                        )
                )
                .then(
                    Commands.literal("view")
                        .executes { ctx -> compartmentViewDefault(ctx) }
                        .then(
                            Commands.argument("slotId", StringArgumentType.word())
                                .executes { ctx -> compartmentView(ctx) }
                        )
                )
                .then(
                    Commands.literal("release")
                        .requires { src -> hasPermission(src, SizeCraftPermissions.RELEASE) }
                        .executes { ctx -> releaseAll(ctx) }
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .executes { ctx -> releaseSpecific(ctx) }
                        )
                )
                .then(
                    Commands.literal("escape")
                        .requires { src -> hasPermission(src, SizeCraftPermissions.ESCAPE) }
                        .executes { ctx -> escape(ctx) }
                )
        )
    }

    // --- Permission helper ---

    private fun hasPermission(source: CommandSourceStack, node: net.neoforged.neoforge.server.permission.nodes.PermissionNode<Boolean>): Boolean {
        val player = source.player ?: return source.hasPermission(2)
        return PermissionAPI.getPermission(player, node)
    }

    private fun canResize(source: CommandSourceStack, target: ServerPlayer): Boolean {
        val sourcePlayer = source.player
        if (sourcePlayer != null && sourcePlayer.uuid == target.uuid) {
            if (!SizeCraftConfig.allowSelfResize) {
                source.sendFailure(Component.translatable("sizecraft.command.self_resize_disabled"))
                return false
            }
            return hasPermission(source, SizeCraftPermissions.RESIZE_SELF)
        }
        return hasPermission(source, SizeCraftPermissions.RESIZE_OTHERS)
    }

    // --- Get ---

    private fun getSelf(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val data = player.getData(SizeDataAttachment.SIZE_DATA)
        ctx.source.sendSuccess({
            Component.translatable(
                "sizecraft.command.get.self",
                String.format("%.2f", data.steps),
                String.format("%.3f", data.scale)
            )
        }, false)
        return 1
    }

    private fun getOther(ctx: CommandContext<CommandSourceStack>): Int {
        val target = EntityArgument.getPlayer(ctx, "player")
        val data = target.getData(SizeDataAttachment.SIZE_DATA)
        ctx.source.sendSuccess({
            Component.translatable(
                "sizecraft.command.get.other",
                target.displayName,
                String.format("%.2f", data.steps),
                String.format("%.3f", data.scale)
            )
        }, false)
        return 1
    }

    // --- Set ---

    private fun setSelfSteps(ctx: CommandContext<CommandSourceStack>): Int {
        val target = ctx.source.playerOrException
        val requestedSteps = DoubleArgumentType.getDouble(ctx, "steps")
        return doSetSteps(ctx.source, target, requestedSteps)
    }

    private fun setSteps(ctx: CommandContext<CommandSourceStack>): Int {
        val target = EntityArgument.getPlayer(ctx, "player")
        val requestedSteps = DoubleArgumentType.getDouble(ctx, "steps")
        return doSetSteps(ctx.source, target, requestedSteps)
    }

    private fun doSetSteps(source: CommandSourceStack, target: ServerPlayer, requestedSteps: Double): Int {
        if (!canResize(source, target)) return 0

        val data = target.getData(SizeDataAttachment.SIZE_DATA)
        val clampedSteps = SizeEvents.clampSteps(requestedSteps, data)

        data.steps = clampedSteps
        target.setData(SizeDataAttachment.SIZE_DATA, data)
        SizeEvents.applyScale(target, data.scale)

        source.sendSuccess({
            Component.translatable(
                "sizecraft.command.set",
                target.displayName,
                String.format("%.2f", clampedSteps),
                String.format("%.3f", data.scale)
            )
        }, true)

        if (clampedSteps != requestedSteps) {
            source.sendSuccess({
                Component.translatable(
                    "sizecraft.command.set.clamped",
                    String.format("%.2f", requestedSteps),
                    String.format("%.2f", clampedSteps)
                )
            }, false)
        }
        return 1
    }

    // --- Reset ---

    private fun resetSelf(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        if (!canResize(ctx.source, player)) return 0
        return doReset(ctx.source, player)
    }

    private fun resetOther(ctx: CommandContext<CommandSourceStack>): Int {
        val target = EntityArgument.getPlayer(ctx, "player")
        return doReset(ctx.source, target)
    }

    private fun doReset(source: CommandSourceStack, target: ServerPlayer): Int {
        val data = target.getData(SizeDataAttachment.SIZE_DATA)
        val clampedSteps = SizeEvents.clampSteps(SizeCraftConfig.defaultSteps, data)
        data.steps = clampedSteps
        target.setData(SizeDataAttachment.SIZE_DATA, data)
        SizeEvents.applyScale(target, data.scale)

        source.sendSuccess({
            Component.translatable("sizecraft.command.reset", target.displayName)
        }, true)
        return 1
    }

    // --- Min/Max ---

    private fun setMin(ctx: CommandContext<CommandSourceStack>): Int {
        val target = EntityArgument.getPlayer(ctx, "player")
        val steps = DoubleArgumentType.getDouble(ctx, "steps")
        val data = target.getData(SizeDataAttachment.SIZE_DATA)
        data.minSteps = steps
        target.setData(SizeDataAttachment.SIZE_DATA, data)

        if (data.steps < steps) {
            data.steps = steps
            target.setData(SizeDataAttachment.SIZE_DATA, data)
            SizeEvents.applyScale(target, data.scale)
        }

        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.min", target.displayName, String.format("%.2f", steps))
        }, true)
        return 1
    }

    private fun setMax(ctx: CommandContext<CommandSourceStack>): Int {
        val target = EntityArgument.getPlayer(ctx, "player")
        val steps = DoubleArgumentType.getDouble(ctx, "steps")
        val data = target.getData(SizeDataAttachment.SIZE_DATA)
        data.maxSteps = steps
        target.setData(SizeDataAttachment.SIZE_DATA, data)

        if (data.steps > steps) {
            data.steps = steps
            target.setData(SizeDataAttachment.SIZE_DATA, data)
            SizeEvents.applyScale(target, data.scale)
        }

        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.max", target.displayName, String.format("%.2f", steps))
        }, true)
        return 1
    }

    // --- Hammerspace teleportation ---

    private fun hammerspaceSelf(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        return teleportToHammerspace(ctx.source, player, player)
    }

    private fun hammerspaceVisit(ctx: CommandContext<CommandSourceStack>): Int {
        val operator = ctx.source.playerOrException
        val target = EntityArgument.getPlayer(ctx, "player")
        return teleportToHammerspace(ctx.source, operator, target)
    }

    private fun hammerspaceReturnSelf(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        return returnFromHammerspace(ctx.source, player)
    }

    private fun hammerspaceReturnOther(ctx: CommandContext<CommandSourceStack>): Int {
        val target = EntityArgument.getPlayer(ctx, "player")
        return returnFromHammerspace(ctx.source, target)
    }

    private fun teleportToHammerspace(
        source: CommandSourceStack,
        traveler: ServerPlayer,
        owner: ServerPlayer,
    ): Int {
        val server = source.server
        val hammerspace = server.getLevel(SizeCraftDimension.HAMMERSPACE_LEVEL_KEY)

        if (hammerspace == null) {
            source.sendFailure(Component.translatable("sizecraft.command.hammerspace.not_loaded"))
            return 0
        }

        val travelerData = traveler.getData(SizeDataAttachment.SIZE_DATA)
        travelerData.returnDimension = traveler.serverLevel().dimension().location().toString()
        travelerData.returnX = traveler.x
        travelerData.returnY = traveler.y
        travelerData.returnZ = traveler.z
        travelerData.returnYaw = traveler.yRot
        travelerData.returnPitch = traveler.xRot
        traveler.setData(SizeDataAttachment.SIZE_DATA, travelerData)

        val ownerData = owner.getData(SizeDataAttachment.SIZE_DATA)
        val stomachSlot = ownerData.stomachSlot

        val populator = HammerspacePopulator.get(hammerspace)
        val roomIndex = populator.getOrAllocateCompartment(owner.uuid, stomachSlot)

        if (!populator.isRoomInitialized(roomIndex)) {
            populator.initializeRoom(hammerspace, roomIndex)
        }

        val spawnPos = HammerspaceLayout.getRoomSpawnPos(roomIndex)

        traveler.teleportTo(
            hammerspace,
            spawnPos.x.toDouble() + 0.5,
            spawnPos.y.toDouble(),
            spawnPos.z.toDouble() + 0.5,
            setOf(),
            traveler.yRot,
            traveler.xRot,
            false
        )

        val compartmentEntry = populator.getCompartments(owner.uuid)[stomachSlot]
        val hsName = compartmentEntry?.let { Component.literal(it.displayName) }
            ?: Component.translatable("sizecraft.hammerspace.default_name")

        if (traveler.uuid == owner.uuid) {
            source.sendSuccess({
                Component.translatable("sizecraft.command.hammerspace.teleport.self", hsName)
            }, false)
        } else {
            source.sendSuccess({
                Component.translatable("sizecraft.command.hammerspace.teleport.other", owner.displayName, hsName)
            }, true)
        }
        return 1
    }

    private fun returnFromHammerspace(
        source: CommandSourceStack,
        player: ServerPlayer,
    ): Int {
        val server = source.server
        val data = player.getData(SizeDataAttachment.SIZE_DATA)

        val manager = CaptureManager.get(server.overworld())
        if (manager.isEaten(player.uuid)) {
            source.sendFailure(Component.translatable("sizecraft.command.hammerspace.leave.eaten"))
            return 0
        }

        if (data.returnDimension.isNotEmpty()) {
            val dimensionKey = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.parse(data.returnDimension)
            )
            val targetLevel = server.getLevel(dimensionKey)

            if (targetLevel != null) {
                player.teleportTo(
                    targetLevel,
                    data.returnX, data.returnY, data.returnZ,
                    setOf(), data.returnYaw, data.returnPitch, false
                )
            } else {
                val overworld = server.overworld()
                val spawnPos = overworld.sharedSpawnPos
                player.teleportTo(
                    overworld,
                    spawnPos.x.toDouble() + 0.5, spawnPos.y.toDouble(), spawnPos.z.toDouble() + 0.5,
                    setOf(), player.yRot, player.xRot, false
                )
            }

            data.returnDimension = ""
            player.setData(SizeDataAttachment.SIZE_DATA, data)

            source.sendSuccess({
                Component.translatable("sizecraft.command.hammerspace.return.success", player.displayName)
            }, true)
        } else {
            val overworld = server.overworld()
            val spawnPos = overworld.sharedSpawnPos
            player.teleportTo(
                overworld,
                spawnPos.x.toDouble() + 0.5, spawnPos.y.toDouble(), spawnPos.z.toDouble() + 0.5,
                setOf(), player.yRot, player.xRot, false
            )

            data.returnDimension = ""
            player.setData(SizeDataAttachment.SIZE_DATA, data)

            source.sendSuccess({
                Component.translatable("sizecraft.command.hammerspace.return.no_position", player.displayName)
            }, true)
        }
        return 1
    }

    // --- Compartment management ---

    private fun compartmentCreate(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val slotId = StringArgumentType.getString(ctx, "slotId")
        val server = ctx.source.server
        val hammerspace = server.getLevel(SizeCraftDimension.HAMMERSPACE_LEVEL_KEY)

        if (hammerspace == null) {
            ctx.source.sendFailure(Component.translatable("sizecraft.command.hammerspace.not_loaded"))
            return 0
        }

        val populator = HammerspacePopulator.get(hammerspace)
        val existing = populator.getCompartmentIndex(player.uuid, slotId)
        if (existing != null) {
            ctx.source.sendFailure(
                Component.translatable("sizecraft.command.hammerspace.create.exists", slotId)
            )
            return 0
        }

        populator.getOrAllocateCompartment(player.uuid, slotId)
        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.hammerspace.create.success", slotId)
        }, false)
        return 1
    }

    private fun compartmentDelete(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val slotId = StringArgumentType.getString(ctx, "slotId")
        val server = ctx.source.server
        val hammerspace = server.getLevel(SizeCraftDimension.HAMMERSPACE_LEVEL_KEY)

        if (hammerspace == null) {
            ctx.source.sendFailure(Component.translatable("sizecraft.command.hammerspace.not_loaded"))
            return 0
        }

        val populator = HammerspacePopulator.get(hammerspace)
        if (populator.getCompartmentIndex(player.uuid, slotId) == null) {
            ctx.source.sendFailure(
                Component.translatable("sizecraft.command.hammerspace.delete.not_found", slotId)
            )
            return 0
        }

        val captureManager = CaptureManager.get(server.overworld())
        if (captureManager.getEatenBy(player.uuid, slotId).isNotEmpty()) {
            ctx.source.sendFailure(
                Component.translatable("sizecraft.command.hammerspace.delete.occupied", slotId)
            )
            return 0
        }

        populator.deleteCompartment(player.uuid, slotId)
        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.hammerspace.delete.success", slotId)
        }, false)
        return 1
    }

    private fun compartmentSetStomach(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val slotId = StringArgumentType.getString(ctx, "slotId")
        val server = ctx.source.server
        val hammerspace = server.getLevel(SizeCraftDimension.HAMMERSPACE_LEVEL_KEY)

        if (hammerspace == null) {
            ctx.source.sendFailure(Component.translatable("sizecraft.command.hammerspace.not_loaded"))
            return 0
        }

        val populator = HammerspacePopulator.get(hammerspace)
        if (populator.getCompartmentIndex(player.uuid, slotId) == null) {
            ctx.source.sendFailure(
                Component.translatable("sizecraft.command.hammerspace.stomach.not_found", slotId)
            )
            return 0
        }

        val data = player.getData(SizeDataAttachment.SIZE_DATA)
        data.stomachSlot = slotId
        player.setData(SizeDataAttachment.SIZE_DATA, data)

        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.hammerspace.stomach.success", slotId)
        }, false)
        return 1
    }

    private fun compartmentRename(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val slotId = StringArgumentType.getString(ctx, "slotId")
        val displayName = StringArgumentType.getString(ctx, "displayName").take(64)
        val server = ctx.source.server
        val hammerspace = server.getLevel(SizeCraftDimension.HAMMERSPACE_LEVEL_KEY)

        if (hammerspace == null) {
            ctx.source.sendFailure(Component.translatable("sizecraft.command.hammerspace.not_loaded"))
            return 0
        }

        val populator = HammerspacePopulator.get(hammerspace)
        if (populator.getCompartmentIndex(player.uuid, slotId) == null) {
            ctx.source.sendFailure(
                Component.translatable("sizecraft.command.hammerspace.rename.not_found", slotId)
            )
            return 0
        }

        populator.renameCompartment(player.uuid, slotId, displayName)
        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.hammerspace.rename.success", slotId, displayName)
        }, false)
        return 1
    }

    private fun compartmentViewDefault(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val data = player.getData(SizeDataAttachment.SIZE_DATA)
        return doCompartmentView(ctx, player, data.stomachSlot)
    }

    private fun compartmentView(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val slotId = StringArgumentType.getString(ctx, "slotId")
        return doCompartmentView(ctx, player, slotId)
    }

    private fun doCompartmentView(ctx: CommandContext<CommandSourceStack>, player: ServerPlayer, slotId: String): Int {
        val server = ctx.source.server
        val hammerspace = server.getLevel(SizeCraftDimension.HAMMERSPACE_LEVEL_KEY)

        if (hammerspace == null) {
            ctx.source.sendFailure(Component.translatable("sizecraft.command.hammerspace.not_loaded"))
            return 0
        }

        val populator = HammerspacePopulator.get(hammerspace)
        if (populator.getCompartmentIndex(player.uuid, slotId) == null) {
            ctx.source.sendFailure(
                Component.translatable("sizecraft.command.hammerspace.view.not_found", slotId)
            )
            return 0
        }

        HammerspaceViewMenu.openFor(player, player.uuid, slotId)
        return 1
    }

    // --- Predator / Prey ---

    private fun togglePredator(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val data = player.getData(SizeDataAttachment.SIZE_DATA)
        data.isPredator = !data.isPredator
        player.setData(SizeDataAttachment.SIZE_DATA, data)

        val state = if (data.isPredator) "sizecraft.command.on" else "sizecraft.command.off"
        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.predator", Component.translatable(state))
        }, false)
        return 1
    }

    private fun setPredator(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val enabled = BoolArgumentType.getBool(ctx, "enabled")
        val data = player.getData(SizeDataAttachment.SIZE_DATA)
        data.isPredator = enabled
        player.setData(SizeDataAttachment.SIZE_DATA, data)

        val state = if (enabled) "sizecraft.command.on" else "sizecraft.command.off"
        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.predator", Component.translatable(state))
        }, false)
        return 1
    }

    private fun togglePrey(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val data = player.getData(SizeDataAttachment.SIZE_DATA)
        data.isPrey = !data.isPrey
        player.setData(SizeDataAttachment.SIZE_DATA, data)

        val state = if (data.isPrey) "sizecraft.command.on" else "sizecraft.command.off"
        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.prey", Component.translatable(state))
        }, false)
        return 1
    }

    private fun setPrey(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val enabled = BoolArgumentType.getBool(ctx, "enabled")
        val data = player.getData(SizeDataAttachment.SIZE_DATA)
        data.isPrey = enabled
        player.setData(SizeDataAttachment.SIZE_DATA, data)

        val state = if (enabled) "sizecraft.command.on" else "sizecraft.command.off"
        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.prey", Component.translatable(state))
        }, false)
        return 1
    }

    // --- Release ---

    private fun releaseAll(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val server = ctx.source.server
        val manager = CaptureManager.get(server.overworld())

        val capturedPlayers = manager.getCapturedBy(player.uuid)
        val eatenPlayers = manager.getEatenBy(player.uuid)
        if (capturedPlayers.isEmpty() && eatenPlayers.isEmpty()) {
            ctx.source.sendFailure(Component.translatable("sizecraft.command.release.none"))
            return 0
        }

        var released = 0
        for (capturedUuid in capturedPlayers) {
            val capturedPlayer = server.playerList.getPlayer(capturedUuid)
            manager.release(capturedUuid)
            if (capturedPlayer != null) {
                manager.releasePlayer(capturedPlayer, player, server)
                released++
            }
        }

        for (eatenUuid in eatenPlayers) {
            val eatenPlayer = server.playerList.getPlayer(eatenUuid)
            manager.release(eatenUuid)
            if (eatenPlayer != null) {
                manager.releasePlayer(eatenPlayer, player, server)
                released++
            }
        }

        dev.sizecraft.capture.CaptureEvents.removeAllCapturedItems(player)

        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.release.success", released)
        }, true)
        return released
    }

    private fun releaseSpecific(ctx: CommandContext<CommandSourceStack>): Int {
        val carrier = ctx.source.playerOrException
        val target = EntityArgument.getPlayer(ctx, "player")
        val server = ctx.source.server
        val manager = CaptureManager.get(server.overworld())

        if (!manager.isCaptured(target.uuid) && !manager.isEaten(target.uuid)) {
            ctx.source.sendFailure(Component.translatable("sizecraft.command.release.not_held", target.displayName))
            return 0
        }

        val holderUuid = manager.getCarrier(target.uuid) ?: manager.getEater(target.uuid)

        if (holderUuid != carrier.uuid && !hasPermission(ctx.source, SizeCraftPermissions.ADMIN)) {
            ctx.source.sendFailure(Component.translatable("sizecraft.command.release.not_yours"))
            return 0
        }

        val actualCarrier = holderUuid?.let { server.playerList.getPlayer(it) }
        manager.release(target.uuid)
        manager.releasePlayer(target, actualCarrier ?: carrier, server)

        if (actualCarrier != null) {
            dev.sizecraft.capture.CaptureEvents.removeCapturedPlayerItem(actualCarrier, target.uuid)
        }

        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.release.released", target.displayName)
        }, true)
        return 1
    }

    // --- Escape ---

    private fun escape(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val server = ctx.source.server
        val manager = CaptureManager.get(server.overworld())

        if (!manager.isEaten(player.uuid)) {
            ctx.source.sendFailure(Component.translatable("sizecraft.command.escape.not_eaten"))
            return 0
        }

        val currentTick = server.overworld().gameTime

        if (!manager.canEscape(player.uuid, currentTick, server)) {
            val remaining = manager.getEscapeTicksRemaining(player.uuid, currentTick)
            if (remaining > 0) {
                val seconds = remaining / 20
                ctx.source.sendFailure(Component.translatable("sizecraft.command.escape.wait", seconds))
            } else {
                val hsName = resolveEaterCompartmentName(player, manager, server)
                ctx.source.sendFailure(Component.translatable("sizecraft.command.escape.blocked", hsName))
            }
            return 0
        }

        val eaterUuid = manager.getEater(player.uuid)
        val eater = eaterUuid?.let { server.playerList.getPlayer(it) }
        val hsName = resolveEaterCompartmentName(player, manager, server)

        manager.release(player.uuid)
        manager.releasePlayer(player, eater, server)

        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.escape.success", hsName)
        }, false)

        eater?.sendSystemMessage(
            Component.translatable("sizecraft.command.escape.escaped", player.displayName, hsName)
        )
        return 1
    }

    private fun resolveEaterCompartmentName(
        player: ServerPlayer,
        manager: CaptureManager,
        server: net.minecraft.server.MinecraftServer,
    ): Component {
        val eaterUuid = manager.getEater(player.uuid) ?: return Component.translatable("sizecraft.hammerspace.default_name")
        val slotId = manager.getEatenInCompartment(player.uuid) ?: return Component.translatable("sizecraft.hammerspace.default_name")
        val hammerspace = server.getLevel(SizeCraftDimension.HAMMERSPACE_LEVEL_KEY)
            ?: return Component.translatable("sizecraft.hammerspace.default_name")
        val entry = HammerspacePopulator.get(hammerspace).getCompartments(eaterUuid)[slotId]
        return entry?.let { Component.literal(it.displayName) }
            ?: Component.translatable("sizecraft.hammerspace.default_name")
    }

    // --- Hammerspace settings ---

    private fun hammerspaceEscape(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val enabled = BoolArgumentType.getBool(ctx, "enabled")
        val data = player.getData(SizeDataAttachment.SIZE_DATA)
        data.hammerspaceEscapable = enabled
        player.setData(SizeDataAttachment.SIZE_DATA, data)

        val state = if (enabled) "sizecraft.command.on" else "sizecraft.command.off"
        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.hammerspace.escape", Component.translatable(state))
        }, false)
        return 1
    }

    private fun hammerspaceDelay(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val ticks = IntegerArgumentType.getInteger(ctx, "ticks")

        val clamped = ticks.coerceAtMost(SizeCraftConfig.maxEscapeDelayTicks)
        val data = player.getData(SizeDataAttachment.SIZE_DATA)
        data.escapeDelayTicks = clamped
        player.setData(SizeDataAttachment.SIZE_DATA, data)

        ctx.source.sendSuccess({
            Component.translatable("sizecraft.command.hammerspace.delay", clamped, clamped / 20)
        }, false)
        return 1
    }
}
