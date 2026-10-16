package @packageName@.api.controller;

import @packageName@.api.mapper.CenterApiMapperImpl;
import @packageName@.application.port.in.CenterUseCase;
import @packageName@.domain.exception.CenterNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CenterController.class)
@Import(CenterApiMapperImpl.class)
class CenterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CenterUseCase centerUseCase;

    @Test
    void listsCenters() throws Exception {
        when(centerUseCase.findAll()).thenReturn(List.of(
                new @packageName@.domain.model.Center(1L, "Centro Madrid", "Madrid")));

        mockMvc.perform(get("/centers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Centro Madrid"))
                .andExpect(jsonPath("$[0].city").value("Madrid"));
    }

    @Test
    void returns404WhenTheCenterDoesNotExist() throws Exception {
        when(centerUseCase.findById(7L)).thenThrow(new CenterNotFoundException(7L));

        mockMvc.perform(get("/centers/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("Centro 7 no encontrado"));
    }
}
