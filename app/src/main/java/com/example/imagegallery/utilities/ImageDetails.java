package com.example.imagegallery.utilities;

import androidx.annotation.NonNull;

import java.util.Date;

public class ImageDetails {
    // Name of the file
    private String fileName;
    // Path of the file
    private String path;
    // Date of when the picture was taken
    private Date dateTaken;
    // Size of the file
    private int size;
    // With of the file
    private int width;
    // Height of the file
    private int height;

    public ImageDetails(String fileName, String path, Date dateTaken, int size, int width, int height) {
        this.fileName = fileName;
        this.path = path;
        this.dateTaken = dateTaken;
        this.size = size;
        this.width = width;
        this.height = height;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPath() {
        return path;
    }

    public Date getDateTaken() {
        return dateTaken;
    }

    public int getSize() {
        return size;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override @NonNull
    public String toString() {
        return "ImageDetails{" +
                "fileName='" + fileName + '\'' +
                ", path='" + path + '\'' +
                ", dateTaken=" + dateTaken +
                ", size=" + size +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}

