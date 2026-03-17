package dev.sizecraft.registry

import dev.sizecraft.SizeCraftMod
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContext
import net.neoforged.neoforge.server.permission.nodes.PermissionNode
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes
import java.util.UUID

/**
 * Permission nodes for sizecraft commands.
 * Uses NeoForge's PermissionAPI, compatible with LuckPerms and vanilla OP levels.
 */
object SizeCraftPermissions {

    // --- Size commands ---

    val COMMAND_SELF: PermissionNode<Boolean> = PermissionNode(
        SizeCraftMod.MOD_ID, "command.self", PermissionTypes.BOOLEAN,
        PermissionNode.PermissionResolver<Boolean> { _: ServerPlayer?, _: UUID, _: Array<out PermissionDynamicContext<*>> -> true }
    )

    val RESIZE_SELF: PermissionNode<Boolean> = PermissionNode(
        SizeCraftMod.MOD_ID, "command.resize.self", PermissionTypes.BOOLEAN,
        PermissionNode.PermissionResolver<Boolean> { _: ServerPlayer?, _: UUID, _: Array<out PermissionDynamicContext<*>> -> true }
    )

    val RESIZE_OTHERS: PermissionNode<Boolean> = PermissionNode(
        SizeCraftMod.MOD_ID, "command.resize.others", PermissionTypes.BOOLEAN,
        PermissionNode.PermissionResolver<Boolean> { player: ServerPlayer?, _: UUID, _: Array<out PermissionDynamicContext<*>> ->
            player?.hasPermissions(2) ?: false
        }
    )

    val DIMENSION_OTHERS: PermissionNode<Boolean> = PermissionNode(
        SizeCraftMod.MOD_ID, "command.dimension.others", PermissionTypes.BOOLEAN,
        PermissionNode.PermissionResolver<Boolean> { player: ServerPlayer?, _: UUID, _: Array<out PermissionDynamicContext<*>> ->
            player?.hasPermissions(2) ?: false
        }
    )

    val ADMIN: PermissionNode<Boolean> = PermissionNode(
        SizeCraftMod.MOD_ID, "command.admin", PermissionTypes.BOOLEAN,
        PermissionNode.PermissionResolver<Boolean> { player: ServerPlayer?, _: UUID, _: Array<out PermissionDynamicContext<*>> ->
            player?.hasPermissions(4) ?: false
        }
    )

    // --- Capture commands ---

    val CAPTURE: PermissionNode<Boolean> = PermissionNode(
        SizeCraftMod.MOD_ID, "command.capture", PermissionTypes.BOOLEAN,
        PermissionNode.PermissionResolver<Boolean> { _: ServerPlayer?, _: UUID, _: Array<out PermissionDynamicContext<*>> -> true }
    )

    val PREDATOR: PermissionNode<Boolean> = PermissionNode(
        SizeCraftMod.MOD_ID, "command.predator", PermissionTypes.BOOLEAN,
        PermissionNode.PermissionResolver<Boolean> { _: ServerPlayer?, _: UUID, _: Array<out PermissionDynamicContext<*>> -> true }
    )

    val PREY: PermissionNode<Boolean> = PermissionNode(
        SizeCraftMod.MOD_ID, "command.prey", PermissionTypes.BOOLEAN,
        PermissionNode.PermissionResolver<Boolean> { _: ServerPlayer?, _: UUID, _: Array<out PermissionDynamicContext<*>> -> true }
    )

    val RELEASE: PermissionNode<Boolean> = PermissionNode(
        SizeCraftMod.MOD_ID, "command.release", PermissionTypes.BOOLEAN,
        PermissionNode.PermissionResolver<Boolean> { _: ServerPlayer?, _: UUID, _: Array<out PermissionDynamicContext<*>> -> true }
    )

    val ESCAPE: PermissionNode<Boolean> = PermissionNode(
        SizeCraftMod.MOD_ID, "command.escape", PermissionTypes.BOOLEAN,
        PermissionNode.PermissionResolver<Boolean> { _: ServerPlayer?, _: UUID, _: Array<out PermissionDynamicContext<*>> -> true }
    )

    fun onRegisterPermissions(event: PermissionGatherEvent.Nodes) {
        event.addNodes(
            COMMAND_SELF, RESIZE_SELF, RESIZE_OTHERS, DIMENSION_OTHERS, ADMIN,
            CAPTURE, PREDATOR, PREY, RELEASE, ESCAPE
        )
    }
}
