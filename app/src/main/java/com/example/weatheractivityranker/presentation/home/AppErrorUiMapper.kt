package com.example.weatheractivityranker.presentation.home

import com.example.weatheractivityranker.domain.model.AppError

fun AppError.toUserMessage(): String = message!!
fun Throwable.toUserMessage(): String = (this as? AppError)?.toUserMessage() ?: message.orEmpty()
