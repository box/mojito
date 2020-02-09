package com.box.l10n.mojito.evolve;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Courses {

    @JsonProperty("_count")
    long count;

    @JsonProperty("_totalPages")
    long totalPages;

    @JsonProperty("_currentPage")
    long currentPage;

    @JsonProperty("_courses")
    List<Course> courses;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }

    public long getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(long currentPage) {
        this.currentPage = currentPage;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }
}
