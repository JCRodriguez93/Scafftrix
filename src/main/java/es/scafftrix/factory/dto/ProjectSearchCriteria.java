package es.scafftrix.factory.dto;

import es.scafftrix.factory.domain.ProjectStatus;
import es.scafftrix.factory.domain.ProjectType;

/**
 * Filter state for the history screen.
 *
 * <p>{@code sort} is constrained to a small allow-list by the controller so it can never
 * reach the {@code Pageable} unchecked.
 *
 * <p>{@code page} and {@code size} are boxed on purpose: they are bound straight from the
 * query string, and Spring's constructor binding passes {@code null} for parameters that are
 * absent. Primitive fields would make {@code /projects} fail with a binding error whenever the
 * pagination parameters are not part of the URL. {@link #normalized()} restores the defaults.
 */
public record ProjectSearchCriteria(String search,
                                    ProjectType type,
                                    ProjectStatus status,
                                    String sort,
                                    Integer page,
                                    Integer size) {

    public static final String SORT_NEWEST = "newest";
    public static final String SORT_OLDEST = "oldest";
    public static final String SORT_NAME_ASC = "name";
    public static final String SORT_STATUS = "status";

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static ProjectSearchCriteria empty() {
        return new ProjectSearchCriteria(null, null, null, SORT_NEWEST, DEFAULT_PAGE, DEFAULT_SIZE);
    }

    public ProjectSearchCriteria normalized() {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        String normalizedSort = isAllowedSort(sort) ? sort : SORT_NEWEST;
        int normalizedPage = page == null ? DEFAULT_PAGE : Math.max(0, page);
        int normalizedSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(MAX_SIZE, size);
        return new ProjectSearchCriteria(normalizedSearch, type, status, normalizedSort, normalizedPage, normalizedSize);
    }

    public boolean isSort(String candidate) {
        return sort != null && sort.equals(candidate);
    }

    private boolean isAllowedSort(String candidate) {
        return SORT_NEWEST.equals(candidate)
                || SORT_OLDEST.equals(candidate)
                || SORT_NAME_ASC.equals(candidate)
                || SORT_STATUS.equals(candidate);
    }
}
