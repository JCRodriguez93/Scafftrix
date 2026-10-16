package es.scafftrix.factory.repository;

import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectStatus;
import es.scafftrix.factory.domain.ProjectType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dynamic filter for the project history screen.
 *
 * <p>All predicates are combined with AND. Every value is bound as a JPA parameter, so
 * no user input is ever concatenated into a query string.
 */
public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    public static Specification<Project> filter(String search,
                                                ProjectType type,
                                                ProjectStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("baseName")), pattern),
                        cb.like(cb.lower(root.get("artifactId")), pattern),
                        cb.like(cb.lower(root.get("groupId")), pattern),
                        cb.like(cb.lower(root.get("packageName")), pattern)));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("projectType"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}