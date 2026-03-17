package dev.sizecraft.dimension

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class CompartmentEntry(val roomIndex: Int, val displayName: String) {
    companion object {
        val CODEC: Codec<CompartmentEntry> = RecordCodecBuilder.create { builder ->
            builder.group(
                Codec.INT.fieldOf("room_index").forGetter(CompartmentEntry::roomIndex),
                Codec.STRING.fieldOf("display_name").forGetter(CompartmentEntry::displayName)
            ).apply(builder, ::CompartmentEntry)
        }
    }
}
