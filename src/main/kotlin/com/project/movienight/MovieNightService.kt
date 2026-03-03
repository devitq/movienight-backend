package com.project.movienight

import com.project.movienight.application.ports.input.CreateFilmCommand
import com.project.movienight.application.ports.input.CreateUserCommand
import com.project.movienight.application.ports.input.EditFilmCommand
import com.project.movienight.application.ports.input.EditUserCommand
import com.project.movienight.application.services.FilmService
import com.project.movienight.application.services.UserService
import com.project.movienight.config.FilmServiceProperties
import com.project.movienight.config.UserServiceProperties
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class MovieNightService(
    private val userService: UserService,
    private val userConfig: UserServiceProperties,
    private val filmService: FilmService,
    private val filmConfig: FilmServiceProperties,
) : CommandLineRunner {
    fun performActions() {
        println("Заблокированные имена пользователей: ${userConfig.blockedNames}")
        println("Заблокированные паттерны в описании фильмов: ${filmConfig.blockedPatterns}")

        println("Создание пользователей...")

        var userAboba = userService.create(CreateUserCommand(name = "абоба", email = "aboba228@mail.com"))
        println(userAboba)

        val userAlice = userService.create(CreateUserCommand(name = "алиса", email = "alice@mail.com"))
        println(userAlice)

        val userBob = userService.create(CreateUserCommand(name = "боб", email = "bob@mail.com"))
        println(userBob)

        println("Создание фильмов...")

        var filmSpiderMan =
            filmService.create(
                CreateFilmCommand(
                    "Spider-Man: Beyond the Spider-Verse",
                    "Spider-Man: Beyond the Spider-Verse is an upcoming American animated superhero film based on Marvel Comics featuring the character Miles Morales / Spider-Man.",
                ),
            )
        println(filmSpiderMan)

        val filmStarWars =
            filmService.create(
                CreateFilmCommand(
                    "Star Wars: Episode III – Revenge of the Sith",
                    "is a 2005 American epic space opera film written and directed by George Lucas. The sequel to Attack of the Clones (2002), it is the sixth film in the Star Wars film series.",
                ),
            )
        println(filmStarWars)

        println("Редактирование фильма ${filmSpiderMan.title}")
        filmSpiderMan = filmService.edit(filmSpiderMan.id, EditFilmCommand("Delayed", "-"))
        println(filmSpiderMan)

        println("Редактирование пользователя ${userAboba.name}")
        userAboba = userService.edit(userAboba.id, EditUserCommand("абеме"))
        println(userAboba)

        println("Создание пользователя с запрещённым именем")
        try {
            userService.create(CreateUserCommand(name = "admin", email = "admin@mail.com"))
        } catch (e: IllegalArgumentException) {
            println("error: ${e.message}")
        }

        println("Создание фильмов с запрещённым паттернами")
        try {
            filmService.create(CreateFilmCommand("Python", "Python is the best language"))
        } catch (e: IllegalArgumentException) {
            println("error: ${e.message}")
        }

        println("Удаление пользователей")
        userService.delete(userAboba.id)
        userService.delete(userAlice.id)
        userService.delete(userBob.id)

        println("Удаление фильмов")
        filmService.delete(filmSpiderMan.id)
        filmService.delete(filmStarWars.id)
    }

    override fun run(vararg args: String) {
        println("Запустился сервис")
        performActions()
    }
}
