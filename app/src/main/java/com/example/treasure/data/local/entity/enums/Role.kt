package com.example.treasure.data.local.entity.enums

/**
 * Authorization role permissions assigned to application users.
 */
enum class Role {
    /**
     * Standard registered user role granted general catalog, wishlist, and cart capabilities.
     */
    USER,

    /**
     * Administrative role with privileged backend diagnostic and configuration access.
     */
    ADMIN
}
