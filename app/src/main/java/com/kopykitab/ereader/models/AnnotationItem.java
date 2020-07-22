package com.kopykitab.ereader.models;

public class AnnotationItem {
    private String chapterName;
    private String pageNumber;
    private String dateAdded;
    private String notes;
    private String annotationText;
    private String coordinates;

    public AnnotationItem(String chapterName, String pageNumber, String dateAdded, String notes, String annotationText) {
        this.chapterName = chapterName;
        this.pageNumber = pageNumber;
        this.dateAdded = dateAdded;
        this.notes = notes;
        this.annotationText = annotationText;
    }

    public String getChapterName() {
        return chapterName;
    }

    public String getPageNumber() {
        return pageNumber;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public String getNotes() {
        return notes;
    }

    public String getAnnotationText() {
        return annotationText;
    }

    public String getCoordinates() {
        return coordinates;
    }
}
