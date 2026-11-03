package com.example.treasure.utils

/**
 * Represents the discrete lifecycle execution states of an asynchronous user action or background operation.
 *
 * This status enum is typically consumed by ViewModels and composable UI layers to orchestrate
 * user feedback mechanisms such as progress spinners, confirmation toasts, or interactive button states.
 */
enum class ActionStatus {
    /**
     * Initial resting state when no operation is currently in progress.
     */
    IDLE,

    /**
     * An asynchronous operation is actively executing.
     * UI consumers should reflect this with loading indicators or by disabling interactive triggers.
     */
    LOADING,

    /**
     * The asynchronous operation completed successfully.
     * UI consumers may acknowledge completion or reset state back to [IDLE].
     */
    SUCCESS
}
