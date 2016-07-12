package com.box.l10n.mojito.rest.entity;

import java.util.List;

/**
 * This is used to retrieved paginated results.
 *
 * @author jaurambault
 */
public class Page<T> {

    /**
     * indicates if there is a next page
     */
    boolean hasNext;

    /**
     * indicates if there is a previous page
     */
    boolean hasPrevious;

    /**
     * the page size
     */
    int size;

    /**
     * the number of the current page
     */
    int number;

    /**
     * the number of elements currently on this page
     */
    int numberOfElements;

    /**
     * indicates if this page is the first
     */
    boolean first;

    /**
     * indicates if this page is the last
     */
    boolean last;

    /**
     * the total number of pages
     */
    int totalPages;

    /**
     * the total number of elements
     */
    int totalElements;

    /**
     * the page content as {@link List}
     */
    List<T> content;

    public boolean hasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean hasPreview() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getNumberOfElements() {
        return numberOfElements;
    }

    public void setNumberOfElements(int numberOfElements) {
        this.numberOfElements = numberOfElements;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

}
