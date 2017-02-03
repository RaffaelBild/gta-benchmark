/*
 * Benchmark of ARX's implementation of the game theoretic approach proposed in:
 * A Game Theoretic Framework for Analyzing Re-Identification Risk.
 * Zhiyu Wan, Yevgeniy Vorobeychik, Weiyi Xia, Ellen Wright Clayton,
 * Murat Kantarcioglu, Ranjit Ganta, Raymond Heatherly, Bradley A. Malin
 * PLOS|ONE. 2015.
 * 
 * Copyright 2017 - Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deidentifier.arx.benchmark;

import java.io.File;
import java.io.IOException;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXCostBenefitConfiguration;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataGeneralizationScheme;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkGeneralizationDegree;
import org.deidentifier.arx.criteria.EDDifferentialPrivacy;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.metric.Metric;

import cern.colt.Arrays;
import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * Benchmark of ARX's implementation of the game theoretic approach proposed in: <br>
 * A Game Theoretic Framework for Analyzing Re-Identification Risk. <br>
 * Zhiyu Wan, Yevgeniy Vorobeychik, Weiyi Xia, Ellen Wright Clayton,
 * Murat Kantarcioglu, Ranjit Ganta, Raymond Heatherly, Bradley A. Malin <br>
 * PLOS|ONE. 2015.
 *
 * @author Fabian Prasser
 */
public abstract class BenchmarkExperimentSafepub extends BenchmarkExperiment {


    /** The benchmark instance */
    private static final Benchmark BENCHMARK               = new Benchmark(new String[] { "adversary gain = publisher loss" });
    /** MEASUREMENT PARAMETER */
    private static final int       PAYOUT_SDGS_LOW         = BENCHMARK.addMeasure("SDGS-Low");
    private static final int       PAYOUT_SDGS_LOW_MIDDLE  = BENCHMARK.addMeasure("SDGS-LowMiddle");
    private static final int       PAYOUT_SDGS_MIDDLE_HIGH = BENCHMARK.addMeasure("SDGS-MiddleHigh");

    private static final int       PAYOUT_KANONYMITY_5     = BENCHMARK.addMeasure("KAnonymity(5)");
    private static final int       PAYOUT_KANONYMITY_10    = BENCHMARK.addMeasure("KAnonymity(10)");
    private static final int       PAYOUT_KANONYMITY_15    = BENCHMARK.addMeasure("KAnonymity(15)");

    /**
     * Main
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        BenchmarkDataset dataset = BenchmarkDataset.ADULT;

        // Init
        BENCHMARK.addAnalyzer(PAYOUT_SDGS_LOW, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_SDGS_LOW_MIDDLE, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_SDGS_MIDDLE_HIGH, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_KANONYMITY_5, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_KANONYMITY_10, new ValueBuffer());
        BENCHMARK.addAnalyzer(PAYOUT_KANONYMITY_15, new ValueBuffer());
        
        // Perform
        ARXCostBenefitConfiguration config = ARXCostBenefitConfiguration.create()
                                                                        .setAdversaryCost(BenchmarkSetup.getDefaultAdversaryCost())
                                                                        .setAdversaryGain(BenchmarkSetup.getDefaultAdversaryGain())
                                                                        .setPublisherLoss(BenchmarkSetup.getDefaultPublisherLoss())
                                                                        .setPublisherBenefit(BenchmarkSetup.getDefaultPublisherBenefit());

        double[] parameters = BenchmarkSetup.getParametersGainLoss();
        for (double parameter : parameters) {
            config.setAdversaryGain(parameter);
            config.setPublisherLoss(parameter);
            System.out.println(" - Adversary gain = publisher loss - " + parameter + " - " + Arrays.toString(parameters));
            BENCHMARK.addRun(config.getAdversaryGain());
            analyze(dataset, config);
            BENCHMARK.getResults().write(new File("results/"+dataset.toString()+"-experiment-sdgs.csv"));
        }
    }

    /**
     * Run the benchmark
     * @param dataset
     * @param config
     * @throws IOException
     */
    private static void analyze(BenchmarkDataset dataset, ARXCostBenefitConfiguration configuration) throws IOException {
     
        // Load data
        Data data = BenchmarkSetup.getData(dataset);
        
        // Run benchmarks
        BENCHMARK.addValue(PAYOUT_SDGS_LOW, getSDGSPayout(data, configuration, 1d, BenchmarkSetup.getDelta(dataset), BenchmarkSetup.getScheme(data, BenchmarkGeneralizationDegree.LOW)));
        System.out.println("   * 1/6");
        BENCHMARK.addValue(PAYOUT_SDGS_LOW_MIDDLE, getSDGSPayout(data, configuration, 1d, BenchmarkSetup.getDelta(dataset), BenchmarkSetup.getScheme(data, BenchmarkGeneralizationDegree.LOW_MIDDLE)));
        System.out.println("   * 2/6");
        BENCHMARK.addValue(PAYOUT_SDGS_MIDDLE_HIGH, getSDGSPayout(data, configuration, 1d, BenchmarkSetup.getDelta(dataset), BenchmarkSetup.getScheme(data, BenchmarkGeneralizationDegree.MIDDLE_HIGH)));
        System.out.println("   * 3/6");
        BENCHMARK.addValue(PAYOUT_KANONYMITY_5, getKAnonymityPayout(data, configuration, 5));
        System.out.println("   * 4/6");
        BENCHMARK.addValue(PAYOUT_KANONYMITY_10, getKAnonymityPayout(data, configuration, 10));
        System.out.println("   * 5/6");
        BENCHMARK.addValue(PAYOUT_KANONYMITY_15, getKAnonymityPayout(data, configuration, 15));
        System.out.println("   * 6/6");
    }

    private static double getSDGSPayout(Data data, ARXCostBenefitConfiguration configuration, double epsilon, double delta, DataGeneralizationScheme scheme) throws IOException {

        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(false));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new EDDifferentialPrivacy(epsilon, delta, scheme, true));
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        payout = (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        // Kontrollieren ob data.getHandle().getNumRows() anzahl der records incl. rausgesampelten return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }

    private static double getKAnonymityPayout(Data data, ARXCostBenefitConfiguration configuration, int k) throws IOException {

        double payout = 0d;
        ARXConfiguration config = ARXConfiguration.create();
        config.setCostBenefitConfiguration(configuration);
        config.setQualityModel(Metric.createPublisherPayoutMetric(false));
        config.setMaxOutliers(1d);
        config.addPrivacyModel(new KAnonymity(k)); 
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        payout = (Double)result.getGlobalOptimum().getHighestScore().getMetadata().get(0).getValue();
        data.getHandle().release();
        return payout / (data.getHandle().getNumRows() * configuration.getPublisherBenefit());
    }
}