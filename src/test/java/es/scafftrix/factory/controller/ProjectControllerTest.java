package es.scafftrix.factory.controller;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectCoordinates;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.domain.TechnologyStack;
import es.scafftrix.factory.dto.ProjectCreationRequest;
import es.scafftrix.factory.dto.ProjectSearchCriteria;
import es.scafftrix.factory.dto.ProjectUpdateRequest;
import es.scafftrix.factory.mapper.ProjectMapper;
import es.scafftrix.factory.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectControllerTest {

    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectMapper mapper = new ProjectMapper();
    private final ProjectController controller = new ProjectController(projectService, mapper);

    @Test
    void newProjectAddsAttributes() {
        when(projectService.offeredSpringBootVersions()).thenReturn(List.of("4.1.1", "4.0.8"));
        when(projectService.defaultSpringBootVersion()).thenReturn("4.1.1");
        when(projectService.defaultGroupId()).thenReturn("es.scafftrix");
        when(projectService.defaultVersion()).thenReturn("1.0.0-SNAPSHOT");

        Model model = new ConcurrentModel();
        String view = controller.newProject(model);

        assertEquals("project/new", view);
        ProjectCreationRequest request = (ProjectCreationRequest) model.getAttribute("request");
        assertNotNull(request);
        assertEquals("4.1.1", request.getSpringBootVersion(), "La versión por defecto se preselecciona");
        assertEquals("1.0.0-SNAPSHOT", request.getVersion(), "La versión del proyecto llega prefijada");
        assertEquals("es.scafftrix", request.getGroupId(), "El Group ID llega prefijado");
        assertEquals(List.of("4.1.1", "4.0.8"), model.getAttribute("springBootVersions"));
        assertNotNull(model.getAttribute("projectTypes"));
        assertNotNull(model.getAttribute("configFormats"));
        assertEquals("4.1.1", model.getAttribute("defaultSpringBootVersion"));
    }

    @Test
    void createWithValidationErrorsReturnsForm() {
        when(projectService.offeredSpringBootVersions()).thenReturn(List.of("4.1.1"));
        when(projectService.defaultSpringBootVersion()).thenReturn("4.1.1");

        ProjectCreationRequest request = new ProjectCreationRequest();
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "request");
        bindingResult.rejectValue("name", "NotBlank", "must not be blank");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        String view = controller.create(request, bindingResult, redirectAttributes, model);

        assertEquals("project/new", view);
        assertNotNull(model.getAttribute("springBootVersions"), "El formulario necesita las opciones al repintarse");
        assertNotNull(model.getAttribute("configFormats"));
    }

    @Test
    void createSucceedsAndRedirects() {
        ProjectCreationRequest request = new ProjectCreationRequest();
        request.setName("Test");
        request.setBaseName("test");
        request.setGroupId("es.scafftrix");
        request.setArtifactId("test");
        request.setPackageName("es.scafftrix.test");
        request.setProjectType(ProjectType.SPRING_BOOT);

        TechnologyStack stack = new TechnologyStack("H2", "Thymeleaf", "Maven");
        Project project = new Project("Test",
                new ProjectCoordinates("test", "es.scafftrix", "test", "es.scafftrix.test", "1.0.0-SNAPSHOT"),
                null, ProjectType.SPRING_BOOT, stack, "4.1.1", ConfigFormat.PROPERTIES);

        when(projectService.create(request)).thenReturn(project);

        BindingResult bindingResult = new BeanPropertyBindingResult(request, "request");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        String view = controller.create(request, bindingResult, redirectAttributes, model);

        assertTrue(view.startsWith("redirect:/projects/"));
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("created"));
    }

    @Test
    void listHandlesNullCriteria() {
        TechnologyStack stack = new TechnologyStack("H2", "Thymeleaf", "Maven");
        Project project = new Project("Test",
                new ProjectCoordinates("test", "es.scafftrix", "test", "es.scafftrix.test", "1.0.0-SNAPSHOT"),
                null, ProjectType.SPRING_BOOT, stack, "4.1.1", ConfigFormat.PROPERTIES);
        Page<Project> page = new PageImpl<>(List.of(project));
        when(projectService.search(any(ProjectSearchCriteria.class))).thenReturn(page);

        Model model = new ConcurrentModel();
        String view = controller.list(null, model);

        assertEquals("project/list", view);
        assertNotNull(model.getAttribute("page"));
        assertNotNull(model.getAttribute("criteria"));
        assertNotNull(model.getAttribute("projects"));
        assertEquals(false, model.getAttribute("empty"));
    }

    @Test
    void detailAddsAttributes() {
        UUID id = UUID.randomUUID();
        TechnologyStack stack = new TechnologyStack("H2", "Thymeleaf", "Maven");
        Project project = new Project("Test",
                new ProjectCoordinates("test", "es.scafftrix", "test", "es.scafftrix.test", "1.0.0-SNAPSHOT"),
                null, ProjectType.SPRING_BOOT, stack, "4.1.1", ConfigFormat.PROPERTIES);

        when(projectService.getEntity(id)).thenReturn(project);

        Model model = new ConcurrentModel();
        String view = controller.detail(id, model);

        assertEquals("project/detail", view);
        assertEquals(project, model.getAttribute("project"));
        assertNotNull(model.getAttribute("detail"));
        assertEquals(stack, model.getAttribute("stack"));
    }

    @Test
    void editFormPrefillsTheEditableValues() {
        UUID id = UUID.randomUUID();
        Project project = new Project("Customer Management",
                new ProjectCoordinates("customerManagement", "es.scafftrix", "customer-management",
                        "es.scafftrix.customermanagement", "2.3.0"),
                "desc", ProjectType.SPRING_BOOT, new TechnologyStack("H2", "Thymeleaf", "Maven"),
                "4.1.1", ConfigFormat.PROPERTIES);
        when(projectService.getEntity(id)).thenReturn(project);

        Model model = new ConcurrentModel();
        String view = controller.editForm(id, model);

        assertEquals("project/edit", view);
        ProjectUpdateRequest request = (ProjectUpdateRequest) model.getAttribute("request");
        assertNotNull(request);
        assertEquals("Customer Management", request.getName());
        assertEquals("2.3.0", request.getVersion());
        assertEquals("desc", request.getDescription());
        assertNotNull(model.getAttribute("detail"));
    }

    @Test
    void updateWithValidationErrorsReturnsEditForm() {
        UUID id = UUID.randomUUID();
        Project project = new Project("Test",
                new ProjectCoordinates("test", "es.scafftrix", "test", "es.scafftrix.test", "1.0.0-SNAPSHOT"),
                null, ProjectType.SPRING_BOOT,
                new TechnologyStack("H2", "Thymeleaf", "Maven"), "4.1.1", ConfigFormat.PROPERTIES);
        when(projectService.getEntity(id)).thenReturn(project);

        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName("");
        request.setVersion("1.0.0");
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "request");
        bindingResult.rejectValue("name", "NotBlank", "must not be blank");

        String view = controller.update(id, request, bindingResult,
                new RedirectAttributesModelMap(), new ConcurrentModel());

        assertEquals("project/edit", view);
        verify(projectService, never()).update(any(), any());
    }

    @Test
    void updateSucceedsAndRedirectsToDetail() {
        UUID id = UUID.randomUUID();
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName("Billing");
        request.setVersion("2.0.0");
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "request");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.update(id, request, bindingResult, redirectAttributes, new ConcurrentModel());

        assertEquals("redirect:/projects/" + id, view);
        verify(projectService).update(id, request);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("updated"));
    }

    @Test
    void deleteRedirectsToListAndSetsFlash() {
        UUID id = UUID.randomUUID();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.delete(id, redirectAttributes);

        assertEquals("redirect:/projects", view);
        verify(projectService).delete(id);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("deleted"));
    }
}
