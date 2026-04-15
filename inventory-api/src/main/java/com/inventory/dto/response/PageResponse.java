package com.inventory.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Explicit pagination wrapper so that totalElements, totalPages, page, size
 * are always serialized correctly — regardless of Jackson / Spring Data config.
 */
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public PageResponse() {}

    /** Build from a Spring Data Page. */
    public static <T> PageResponse<T> of(Page<T> p) {
        PageResponse<T> r = new PageResponse<>();
        r.content       = p.getContent();
        r.page          = p.getNumber();
        r.size          = p.getSize();
        r.totalElements = p.getTotalElements();
        r.totalPages    = p.getTotalPages();
        r.first         = p.isFirst();
        r.last          = p.isLast();
        return r;
    }

    public List<T> getContent()          { return content; }
    public void setContent(List<T> c)    { this.content = c; }
    public int getPage()                 { return page; }
    public void setPage(int p)           { this.page = p; }
    public int getSize()                 { return size; }
    public void setSize(int s)           { this.size = s; }
    public long getTotalElements()       { return totalElements; }
    public void setTotalElements(long t) { this.totalElements = t; }
    public int getTotalPages()           { return totalPages; }
    public void setTotalPages(int t)     { this.totalPages = t; }
    public boolean isFirst()             { return first; }
    public void setFirst(boolean f)      { this.first = f; }
    public boolean isLast()              { return last; }
    public void setLast(boolean l)       { this.last = l; }
}
