package com.pdfanalyzer.core.frequency.fft;

import com.pdfanalyzer.core.semantic.model.SemanticCell;
import lombok.extern.slf4j.Slf4j;
import org.jtransforms.fft.DoubleFFT_1D;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FFT 기반 주파수 영역 분석 엔진
 * Y[k] = X[k] · P[k]
 * R = IFFT(X · P)
 */
@Slf4j
@Component
public class FFTEngine {

    public FFTSpectrum transform(List<SemanticCell> cells) {
        log.info("FFT 변환 시작: {} 셀", cells.size());

        double[] signal = cellsToSignal(cells);
        DoubleFFT_1D fft = new DoubleFFT_1D(signal.length);
        double[] spectrum = new double[signal.length * 2];

        System.arraycopy(signal, 0, spectrum, 0, signal.length);
        fft.realForwardFull(spectrum);

        log.info("FFT 변환 완료: {} 포인트", signal.length);

        return FFTSpectrum.builder()
                .complexSpectrum(spectrum)
                .size(signal.length)
                .originalCells(cells)
                .build();
    }

    public double[] inverseTransform(FFTSpectrum spectrum) {
        log.debug("IFFT 변환 시작");

        double[] complexData = spectrum.getComplexSpectrum().clone();
        DoubleFFT_1D fft = new DoubleFFT_1D(spectrum.getSize());

        fft.complexInverse(complexData, true);

        double[] result = new double[spectrum.getSize()];
        for (int i = 0; i < spectrum.getSize(); i++) {
            result[i] = complexData[i * 2];
        }

        log.debug("IFFT 변환 완료");
        return result;
    }

    public FFTSpectrum applyFilter(FFTSpectrum spectrum, double[] filter) {
        log.debug("주파수 필터 적용");

        double[] complexSpectrum = spectrum.getComplexSpectrum().clone();
        int size = spectrum.getSize();

        for (int i = 0; i < size; i++) {
            if (i < filter.length) {
                complexSpectrum[i * 2] *= filter[i];
                complexSpectrum[i * 2 + 1] *= filter[i];
            }
        }

        return FFTSpectrum.builder()
                .complexSpectrum(complexSpectrum)
                .size(size)
                .originalCells(spectrum.getOriginalCells())
                .build();
    }

    public double[] analyzeResonance(List<SemanticCell> cells, double[] filter) {
        log.info("공명 분석 시작");

        FFTSpectrum spectrum = transform(cells);
        FFTSpectrum filtered = applyFilter(spectrum, filter);
        double[] resonance = inverseTransform(filtered);

        log.info("공명 분석 완료");
        return resonance;
    }

    public double[] calculatePowerSpectrum(FFTSpectrum spectrum) {
        double[] complex = spectrum.getComplexSpectrum();
        int size = spectrum.getSize();
        double[] power = new double[size];

        for (int i = 0; i < size; i++) {
            double real = complex[i * 2];
            double imag = complex[i * 2 + 1];
            power[i] = Math.sqrt(real * real + imag * imag);
        }

        return power;
    }

    private double[] cellsToSignal(List<SemanticCell> cells) {
        double[] signal = new double[cells.size()];

        for (int i = 0; i < cells.size(); i++) {
            SemanticCell cell = cells.get(i);
            double value = 0.0;

            value += cell.getImportance() * 2.0;
            double normalizedLength = Math.log(cell.getLength() + 1) / 10.0;
            value += normalizedLength;

            if (cell.isHeader()) {
                value += 1.0;
            }

            signal[i] = value;
        }

        return signal;
    }

    public double[] calculateBandEnergy(FFTSpectrum spectrum, int numBands) {
        double[] power = calculatePowerSpectrum(spectrum);
        double[] bandEnergy = new double[numBands];
        int bandSize = power.length / numBands;

        for (int band = 0; band < numBands; band++) {
            double energy = 0.0;
            int start = band * bandSize;
            int end = Math.min(start + bandSize, power.length);

            for (int i = start; i < end; i++) {
                energy += power[i];
            }

            bandEnergy[band] = energy / (end - start);
        }

        return bandEnergy;
    }
}
