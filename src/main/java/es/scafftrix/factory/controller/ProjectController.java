package es.scafftrix.factory.controller;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.dto.ProjectCreationRequest;
import es.scafftrix.factory.dto.ProjectSearchCriteria;
import es.scafftrix.factory.dto.ProjectUpdateRequest;
import es.scafftrix.factory.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * Controller for creating, listing and inspecting projects.
 *
 * <p>It delegates all business logic to {@link ProjectService}, only translating form
 * submissions and Spring MVC objects. The pinned corporate technology is not read back from
 * the form: the two selectable technical options (Spring Boot version and configuration
 * format) are re-validated against the configured allow-list on creation.
 */
@Controller
@RequestMapping("/projects")
public class ProjectController {

    private static final String MODEL_PROJECT = "project";
    private static final String MODEL_DETAIL = "detail";

    private final ProjectService projectService;
    private final es.scafftrix.factory.mapper.ProjectMapper mapper;

    public ProjectController(ProjectService projectService, es.scafftrix.factory.mapper.ProjectMapper mapper) {
        this.projectService = projectService;
        this.mapper = mapper;
    }

    @GetMapping("/new")
    public String newProject(Model model) {
        ProjectCreationRequest request = new ProjectCreationRequest();
        request.setGroupId(projectService.defaultGroupId());
        request.setVersion(projectService.defaultVersion());
        request.setSpringBootVersion(projectService.defaultSpringBootVersion());
        model.addAttribute("request", request);
        addCreateFormOptions(model);
        return "project/new";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("request") ProjectCreationRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            addCreateFormOptions(model);
            return "project/new";
        }
        var project = projectService.create(request);
        redirectAttributes.addFlashAttribute("created", true);
        return "redirect:/projects/" + project.getId();
    }

    @GetMapping
    public String list(@ModelAttribute("criteria") ProjectSearchCriteria criteria, Model model) {
        criteria = criteria == null ? ProjectSearchCriteria.empty() : criteria.normalized();
        Page<Project> page = projectService.search(criteria);
        model.addAttribute("page", page);
        model.addAttribute("criteria", criteria);
        model.addAttribute("projects", mapper.toSummaries(page.getContent()));
        model.addAttribute("empty", page.isEmpty());
        return "project/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        Project project = addProjectContext(model, id);
        model.addAttribute("stack", project.technologyStack());
        return "project/detail";
    }

    /** Renders the edit form prefilled with the current values of the project. */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        Project project = addProjectContext(model, id);
        model.addAttribute("request", mapper.toUpdateRequest(project));
        return "project/edit";
    }

    /**
     * Applies the editable metadata of a project. On validation failure the form is
     * re-rendered with the submitted values and the project context; on success the user
     * returns to the detail page.
     */
    @PostMapping("/{id}")
    public String update(@PathVariable UUID id,
                         @Valid @ModelAttribute("request") ProjectUpdateRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            addProjectContext(model, id);
            return "project/edit";
        }
        projectService.update(id, request);
        redirectAttributes.addFlashAttribute("updated", true);
        return "redirect:/projects/" + id;
    }

    /**
     * Deletes a project after the browser confirmation. Always redirects to the list, so a
     * repeated submission simply lands on a list where the project is already gone.
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        projectService.delete(id);
        redirectAttributes.addFlashAttribute("deleted", true);
        return "redirect:/projects";
    }

    /** Loads the project and exposes it (plus its detail view) to the given model. */
    private Project addProjectContext(Model model, UUID id) {
        Project project = projectService.getEntity(id);
        model.addAttribute(MODEL_PROJECT, project);
        model.addAttribute(MODEL_DETAIL, mapper.toDetail(project));
        return project;
    }

    /** Everything the create form needs besides the bound request. */
    private void addCreateFormOptions(Model model) {
        model.addAttribute("projectTypes", ProjectType.values());
        model.addAttribute("springBootVersions", projectService.offeredSpringBootVersions());
        model.addAttribute("configFormats", ConfigFormat.values());
        model.addAttribute("defaultSpringBootVersion", projectService.defaultSpringBootVersion());
    }
}