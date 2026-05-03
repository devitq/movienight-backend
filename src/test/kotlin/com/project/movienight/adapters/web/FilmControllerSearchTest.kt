package com.project.movienight.adapters.web

import com.project.movienight.application.ports.input.CreateFilmUseCase
import com.project.movienight.application.ports.input.DeleteFilmUseCase
import com.project.movienight.application.ports.input.EditFilmUseCase
import com.project.movienight.application.ports.input.GetAllFilmsUseCase
import com.project.movienight.application.ports.input.GetFilmByIdUseCase
import com.project.movienight.application.ports.input.SearchFilmByTitleUseCase
import com.project.movienight.domain.model.Film
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID

class FilmControllerSearchTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var searchFilmByTitleUseCase: SearchFilmByTitleUseCase

    @BeforeEach
    fun setup() {
        searchFilmByTitleUseCase = mockk()

        val controller =
            FilmController(
                createFilmUseCase = mockk<CreateFilmUseCase>(),
                editFilmUseCase = mockk<EditFilmUseCase>(),
                deleteFilmUseCase = mockk<DeleteFilmUseCase>(),
                getFilmByIdUseCase = mockk<GetFilmByIdUseCase>(),
                getAllFilmsUseCase = mockk<GetAllFilmsUseCase>(),
                searchFilmByTitleUseCase = searchFilmByTitleUseCase,
            )

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `search returns film when title exists`() {
        val title = "Inception"
        val film = Film(id = UUID.randomUUID(), title = title, description = "A dream heist")

        every { searchFilmByTitleUseCase.searchByTitle(title) } returns film

        mockMvc
            .get("/api/films/search") {
                param("title", title)
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value(film.id.toString()) }
                jsonPath("$.title") { value(title) }
                jsonPath("$.description") { value("A dream heist") }
            }

        verify(exactly = 1) { searchFilmByTitleUseCase.searchByTitle(title) }
    }

    @Test
    fun `search returns empty body when title is missing`() {
        val title = "Unknown Title"

        every { searchFilmByTitleUseCase.searchByTitle(title) } returns null

        mockMvc
            .get("/api/films/search") {
                param("title", title)
            }.andExpect {
                status { isOk() }
                content { string("") }
            }

        verify(exactly = 1) { searchFilmByTitleUseCase.searchByTitle(title) }
    }
}
