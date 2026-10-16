package @packageName@.api.controller;

import @packageName@.api.mapper.UserApiMapperImpl;
import @packageName@.application.port.in.UserUseCase;
import @packageName@.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(UserApiMapperImpl.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserUseCase userUseCase;

    @Test
    void listsUsers() throws Exception {
        when(userUseCase.findAll()).thenReturn(List.of(
                new @packageName@.domain.model.User(1L, "jdoe", "jdoe@example.com", "John", "Doe")));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("jdoe"))
                .andExpect(jsonPath("$[0].email").value("jdoe@example.com"));
    }

    @Test
    void returns404WhenTheUserDoesNotExist() throws Exception {
        when(userUseCase.findById(42L)).thenThrow(new UserNotFoundException(42L));

        mockMvc.perform(get("/users/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("Usuario 42 no encontrado"));
    }

    @Test
    void createsAUserWith201() throws Exception {
        when(userUseCase.create(any())).thenReturn(
                new @packageName@.domain.model.User(5L, "new", "new@example.com", null, null));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new\",\"email\":\"new@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("new"));
    }

    @Test
    void rejectsAUserWithoutRequiredFields() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
