package com.ericlam.mc.kotlib.command

class CommandError(val mark: ErrorType, data: String) : Exception(data) {
    enum class ErrorType {
        FEW_ARGS, NO_PERMISSION
    }
}