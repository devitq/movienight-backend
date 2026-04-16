package com.project.movienight

import com.project.movienight.application.ports.input.CreateFilmCommand
import com.project.movienight.application.ports.input.CreateUserCommand
import com.project.movienight.application.ports.input.EditFilmCommand
import com.project.movienight.application.ports.input.EditUserCommand
import com.project.movienight.application.services.FilmService
import com.project.movienight.application.services.UserService
import com.project.movienight.config.FilmServiceProperties
import com.project.movienight.config.UserServiceProperties
import com.project.movienight.domain.model.Film
import com.project.movienight.domain.model.User
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class MovieNightService(
    private val userService: UserService,
    private val userConfig: UserServiceProperties,
    private val filmService: FilmService,
    private val filmConfig: FilmServiceProperties,
) : CommandLineRunner {
    private data class CreatedUsers(
        var aboba: User,
        val alice: User,
        val bob: User,
    )

    private data class CreatedFilms(
        var spiderMan: Film,
        val starWars: Film,
    )

    fun performActions() {
        printBlockedRules()

        val users = createUsers()
        val films = createFilms()

        editEntities(users, films)
        runNegativeChecks()
        deleteEntities(users, films)
    }

    private fun printBlockedRules() {
        println("Заблокированные имена пользователей: ${userConfig.blockedNames}")
        println("Заблокированные паттерны в описании фильмов: ${filmConfig.blockedPatterns}")
    }

    private fun createUsers(): CreatedUsers {
        println("Создание пользователей...")

        val userAboba =
            userService.create(
                CreateUserCommand(name = "абоба", email = "aboba228@mail.com"),
            )
        println(userAboba)

        val userAlice =
            userService.create(
                CreateUserCommand(name = "алиса", email = "alice@mail.com"),
            )
        println(userAlice)

        val userBob =
            userService.create(
                CreateUserCommand(name = "боб", email = "bob@mail.com"),
            )
        println(userBob)

        return CreatedUsers(
            aboba = userAboba,
            alice = userAlice,
            bob = userBob,
        )
    }

    private fun createFilms(): CreatedFilms {
        println("Создание фильмов...")

        val filmSpiderMan =
            filmService.create(
                CreateFilmCommand(
                    "Spider-Man: Beyond the Spider-Verse",
                    "Spider-Man: Beyond the Spider-Verse is an upcoming American animated superhero " +
                        "film based on Marvel Comics featuring the character Miles Morales / Spider-Man.",
                ),
            )
        println(filmSpiderMan)

        val filmStarWars =
            filmService.create(
                CreateFilmCommand(
                    "Star Wars: Episode III – Revenge of the Sith",
                    "is a 2005 American epic space opera film written and directed by George Lucas. " +
                        "The sequel to Attack of the Clones (2002), it is the sixth film in the Star Wars film series.",
                ),
            )
        println(filmStarWars)

        return CreatedFilms(
            spiderMan = filmSpiderMan,
            starWars = filmStarWars,
        )
    }

    private fun editEntities(
        users: CreatedUsers,
        films: CreatedFilms,
    ) {
        println("Редактирование фильма ${films.spiderMan.title}")
        films.spiderMan =
            filmService.edit(
                id = films.spiderMan.id,
                command =
                    EditFilmCommand(
                        title = "Delayed",
                        description = "-",
                    ),
            )
        println(films.spiderMan)

        println("Редактирование пользователя ${users.aboba.name}")
        users.aboba = userService.edit(users.aboba.id, EditUserCommand("абеме"))
        println(users.aboba)
    }

    private fun runNegativeChecks() {
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
    }

    private fun deleteEntities(
        users: CreatedUsers,
        films: CreatedFilms,
    ) {
        println("Удаление пользователей")
        userService.delete(users.aboba.id)
        userService.delete(users.alice.id)
        userService.delete(users.bob.id)

        println("Удаление фильмов")
        filmService.delete(films.spiderMan.id)
        filmService.delete(films.starWars.id)
    }

    override fun run(vararg args: String) {
        println("Запустился сервис")
        performActions()
    }
}
