package com.box.l10n.mojito.rest;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * {@link Pageable} wrapper annotated with {@link View.Pageable} to serialize
 * pagination information when issue views.
 *
 * @author jaurambault
 */
public class PageView<T> implements Page<T> {

    Page<T> page;

    public PageView(Page<T> page) {
        this.page = page;
    }

    @JsonView(View.Pageable.class)
    @Override
    public int getTotalPages() {
        return page.getTotalPages();
    }

    @JsonView(View.Pageable.class)
    @Override
    public long getTotalElements() {
        return page.getTotalElements();
    }

    @Override
    public <U> Page<U> map(Function<? super T, ? extends U> converter) {
        return page.map(converter);
    }

    @JsonView(View.Pageable.class)
    @Override
    public int getNumber() {
        return page.getNumber();
    }

    @JsonView(View.Pageable.class)
    @Override
    public int getSize() {
        return page.getSize();
    }

    @JsonView(View.Pageable.class)
    @Override
    public int getNumberOfElements() {
        return page.getNumberOfElements();
    }

    @JsonView(View.Pageable.class)
    @Override
    public List<T> getContent() {
        return page.getContent();
    }

    @Override
    public boolean hasContent() {
        return page.hasContent();
    }

    @Override
    public Sort getSort() {
        return page.getSort();
    }

    @JsonView(View.Pageable.class)
    @Override
    public boolean isFirst() {
        return page.isFirst();
    }

    @JsonView(View.Pageable.class)
    @Override
    public boolean isLast() {
        return page.isLast();
    }

    @JsonView(View.Pageable.class)
    @Override
    public boolean hasNext() {
        return page.hasNext();
    }

    @JsonView(View.Pageable.class)
    @Override
    public boolean hasPrevious() {
        return page.hasPrevious();
    }

    @Override
    public Pageable nextPageable() {
        return page.nextPageable();
    }

    @Override
    public Pageable previousPageable() {
        return page.previousPageable();
    }

    @Override
    public Iterator<T> iterator() {
        return page.iterator();
    }

    @Override
    public int hashCode() {
        return page.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return page.equals(obj);
    }

    @Override
    public String toString() {
        return page.toString();
    }

}
