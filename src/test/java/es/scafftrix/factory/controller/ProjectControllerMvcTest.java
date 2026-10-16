package es.scafftrix.factory.controller;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectCoordinates;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.domain.TechnologyStack;
import es.scafftrix.factory.dto.ProjectSearchCriteria;
import es.scafftrix.factory.mapper.ProjectMapper;
import es.scafftrix.factory.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Renders the real Thymeleaf views through the web layer, so the frontend contract
 * (edit/delete controls and the edit form) is checked, not just the model attributes.
 */
@WebMvcTest(controllers = ProjectController.class)
@Import(ProjectMapper.class)
class ProjectControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    private Project sampleProject() {
        return new Project("Customer Management",
                new ProjectCoordinates("customerManagement", "es.scafftrix", "customer-management",
                        "es.scafftrix.customermanagement", "1.0.0-SNAPSHOT"),
                "desc", ProjectType.SPRING_BOOT, new TechnologyStack("H2", "Thymeleaf", "Maven"),
                "4.1.1", ConfigFormat.PROPERTIES);
    }

    @Test
    void listPageRendersEditAndDeleteControls() throws Exception {
        when(projectService.search(any(ProjectSearchCriteria.class)))
                .thenReturn(new PageImpl<>(List.of(sampleProject())));

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(view().name("project/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Editar")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Eliminar")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/delete")));
    }

    @Test
    void editPageRendersThePrefilledForm() throws Exception {
        UUID id = UUID.randomUUID();
        when(projectService.getEntity(id)).thenReturn(sampleProject());

        mockMvc.perform(get("/projects/{id}/edit", id))
                .andExpect(status().isOk())
                .andExpect(view().name("project/edit"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Customer Management")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Guardar cambios")));
    }

    @Test
    void deletePostRedirectsToList() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/projects/{id}/delete", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects"));
    }

    @Test
    void invalidUpdateReRendersTheForm() throws Exception {
        UUID id = UUID.randomUUID();
        when(projectService.getEntity(id)).thenReturn(sampleProject());

        mockMvc.perform(post("/projects/{id}", id)
                        .param("name", "")
                        .param("version", "1.0.0"))
                .andExpect(status().isOk())
                .andExpect(view().name("project/edit"));
    }
}
