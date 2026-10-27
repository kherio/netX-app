package com.kherio.nax

enum class Status { ONLINE, OFFLINE, DOWN, ARCHIVED }

data class Device(
    val mac: String,
    val name: String,
    val ip: String,
    val vendor: String,
    val type: String,
    val status: Status,
    val isNew: Boolean,
    val favorite: Boolean,
    val lastConnection: String,
    val firstConnection: String,
) {
    val title: String get() = name.ifBlank { mac }
}

data class NetEvent(
    val mac: String,
    val ip: String,
    val dateTime: String,
    val type: String,
    val info: String,
)

data class Totals(
    val devices: Int,
    val connected: Int,
    val favorites: Int,
    val newDevices: Int,
    val down: Int,
    val archived: Int,
) {
    companion object {
        fun from(list: List<Device>) = Totals(
            devices = list.count { it.status != Status.ARCHIVED },
            connected = list.count { it.status == Status.ONLINE },
            favorites = list.count { it.favorite },
            newDevices = list.count { it.isNew },
            down = list.count { it.status == Status.DOWN },
            archived = list.count { it.status == Status.ARCHIVED },
        )
    }
}
