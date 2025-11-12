package com.melodical.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MelodicalApplication

fun main(args: Array<String>) {
    runApplication<MelodicalApplication>(*args)
}