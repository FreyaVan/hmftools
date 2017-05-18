package com.hartwig.hmftools.common.purple;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.hartwig.hmftools.common.copynumber.CopyNumberAlteration;
import com.hartwig.hmftools.common.numeric.Doubles;

public class FittedCopyNumberFactory {

    private final int maxPloidy;
    private final double cnvRatioWeightFactor;

    public FittedCopyNumberFactory(int maxPloidy, double cnvRatioWeightFactor) {
        this.maxPloidy = maxPloidy;
        this.cnvRatioWeightFactor = cnvRatioWeightFactor;
    }

    public List<FittedCopyNumber> fittedCopyNumber(double purity, double normFactor, Collection<EnrichedCopyNumber> copyNumbers) {
        return copyNumbers.stream().map(x -> fittedCopyNumber(purity, normFactor, x)).collect(Collectors.toList());
    }

    FittedCopyNumber fittedCopyNumber(double purity, double normFactor, EnrichedCopyNumber copyNumber) {

        double minDeviation = 0;
        double actualBAF = copyNumber.mBAF();
        double actualRatio = copyNumber.tumorRatio();

        ImmutableFittedCopyNumber.Builder builder = ImmutableFittedCopyNumber.builder()
                .from(copyNumber)
                .status(copyNumber.status())
                .genotype(copyNumber.genotype())
                .bafCount(copyNumber.mBAFCount())
                .actualBAF(actualBAF)
                .tumorCNVRatio(actualRatio)
                .normalCNVRatio(copyNumber.normalRatio())
                .broadBAF(0)
                .broadRatioOfRatios(0)
                .segmentBAF(0)
                .segmentRatioOfRatios(0)
                .normalisedTumorRatio(actualRatio / normFactor * 2d)
                .ratioOfRatios(Doubles.replaceNaNWithZero(actualRatio / copyNumber.normalRatio() / normFactor * 2));

        for (int ploidy = 1; ploidy <= maxPloidy; ploidy++) {
            double modelRatio = modelCNVRatio(purity, normFactor, ploidy);
            double cnvDeviation = cnvDeviation(cnvRatioWeightFactor, modelRatio, actualRatio);

            double modelBAF = copyNumber.mBAFCount() == 0 ? 0 : modelBAFToMinimizeDeviation(purity, ploidy, actualBAF);
            double bafDeviation = bafDeviation(modelBAF, actualBAF);

            double deviation = Math.pow(Math.max(ploidy, 1.5) / 2.0, 0.85) * (bafDeviation + cnvDeviation) * actualBAF;

            if (ploidy == 1 || deviation < minDeviation) {
                builder.fittedPloidy(ploidy)
                        .modelBAF(modelBAF)
                        .modelCNVRatio(modelRatio)
                        .bafDeviation(bafDeviation)
                        .cnvDeviation(cnvDeviation)
                        .deviation(deviation);
                minDeviation = deviation;
            }
        }

        return builder.build();
    }

    @VisibleForTesting
    static double modelCNVRatio(double purity, double normFactor, int ploidy) {
        return normFactor + (ploidy - CopyNumberAlteration.NORMAL_HUMAN_COPY_NUMBER) * purity * normFactor / 2d;
    }

    @VisibleForTesting
    static double cnvDeviation(double cnvRatioWeighFactor, double modelCNVRatio, double actualRatio) {
        return cnvRatioWeighFactor * Math.abs(modelCNVRatio - actualRatio);
    }

    private static double bafDeviation(double modelBAF, double actualBAF) {
        return Math.abs(modelBAF - actualBAF);
    }

    @VisibleForTesting
    static double modelBAFToMinimizeDeviation(double purity, int ploidy, double actualBAF) {
        double result = 0;
        double deviation = 0;

        int minBetaAllele = (int) Math.round(ploidy / 2d);
        for (int betaAllele = minBetaAllele; betaAllele < ploidy + 1; betaAllele++) {

            double modelBAF = modelBAF(purity, ploidy, betaAllele);
            double modelDeviation = bafDeviation(modelBAF, actualBAF);

            if (betaAllele == minBetaAllele || modelDeviation < deviation) {
                result = modelBAF;
                deviation = modelDeviation;
            }
        }

        return result;
    }

    @VisibleForTesting
    static double modelBAF(double purity, int ploidy, int alleleCount) {
        if (ploidy / alleleCount == 2) {
            return 0.533;
        }

        return (1 + purity * (alleleCount - 1)) / (2 + purity * (ploidy - 2));
    }
}
