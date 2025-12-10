package com.pdfanalyzer.core.frequency.fft;

import com.pdfanalyzer.core.semantic.model.SemanticCell;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FFT 변환 결과를 담는 스펙트럼 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FFTSpectrum {
    private double[] complexSpectrum;
    private int size;
    private List<SemanticCell> originalCells;

    public double[] getRealPart() {
        double[] real = new double[size];
        for (int i = 0; i < size; i++) {
            real[i] = complexSpectrum[i * 2];
        }
        return real;
    }

    public double[] getImaginaryPart() {
        double[] imag = new double[size];
        for (int i = 0; i < size; i++) {
            imag[i] = complexSpectrum[i * 2 + 1];
        }
        return imag;
    }

    public double getMagnitudeAt(int index) {
        if (index < 0 || index >= size) {
            return 0.0;
        }
        double real = complexSpectrum[index * 2];
        double imag = complexSpectrum[index * 2 + 1];
        return Math.sqrt(real * real + imag * imag);
    }

    public double getPhaseAt(int index) {
        if (index < 0 || index >= size) {
            return 0.0;
        }
        double real = complexSpectrum[index * 2];
        double imag = complexSpectrum[index * 2 + 1];
        return Math.atan2(imag, real);
    }
}
