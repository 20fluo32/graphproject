package com.demo.domain;

import com.demo.utils.Letterbox;
import org.opencv.core.Mat;

public class PreprocessedFrame {
    private Mat image;
    private Letterbox letterbox;

    public PreprocessedFrame(Mat image, Letterbox letterbox) {
        this.image = image;
        this.letterbox = letterbox;
    }

    public Mat getImage() {
        return image;
    }

    public Letterbox getLetterbox() {
        return letterbox;
    }
}
