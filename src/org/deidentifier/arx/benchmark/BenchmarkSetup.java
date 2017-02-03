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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataGeneralizationScheme;

/**
 * Benchmark of ARX's implementation of the game theoretic approach proposed in: <br>
 * A Game Theoretic Framework for Analyzing Re-Identification Risk. <br>
 * Zhiyu Wan, Yevgeniy Vorobeychik, Weiyi Xia, Ellen Wright Clayton,
 * Murat Kantarcioglu, Ranjit Ganta, Raymond Heatherly, Bradley A. Malin <br>
 * PLOS|ONE. 2015.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkSetup {

    public static enum BenchmarkDataset {
        
        ADULT {
            @Override
            public String toString() {
                return "adult";
            }
        },
        ADULT_NC {
            @Override
            public String toString() {
                return "adult-nc";
            }
        },
        ADULT_TN {
            @Override
            public String toString() {
                return "adult-tn";
            }
        },
        ADULT_TN_SAFE_HARBOR {
            @Override
            public String toString() {
                return "adult-tn-safe-harbor";
            }
        },
        CUP {
            @Override
            public String toString() {
                return "cup";
            }
        },
        FARS {
            @Override
            public String toString() {
                return "fars";
            }
        },
        ATUS {
            @Override
            public String toString() {
                return "atus";
            }
        },
        IHIS {
            @Override
            public String toString() {
                return "ihis";
            }
        },
    }
    /**
     * Some values indicating desired generalization heights
     */
    public static enum BenchmarkGeneralizationDegree {
        NONE {
            @Override
            public String toString() {
                return "none";
            }
        },
        LOW {
            @Override
            public String toString() {
                return "low";
            }
        },
        LOW_MIDDLE {
            @Override
            public String toString() {
                return "low-middle";
            }
        },
        MIDDLE_HIGH {
            @Override
            public String toString() {
                return "middle-high";
            }
        },
        HIGH {
            @Override
            public String toString() {
                return "high";
            }
        }
    }

    /**
     * Returns the dataset for the given name
     * @param name
     * @return
     */
    public static BenchmarkDataset getBenchmarkDataset(String name) {
        for (BenchmarkDataset dataset : BenchmarkDataset.values()) {
            if (dataset.toString().equals(name)) {
                return dataset;
            }
        }
        throw new IllegalArgumentException("Unknown dataset");
    }

    public static DataGeneralizationScheme getScheme(Data data, 
                                                     BenchmarkGeneralizationDegree degree) {
        DataGeneralizationScheme scheme = DataGeneralizationScheme.create(data);
        
        for (String attribute : data.getDefinition().getQuasiIdentifyingAttributes()) {
            scheme.generalize(attribute, getGeneralizationLevel(data.getDefinition().getHierarchy(attribute)[0].length,
                                                                degree));
        }
        return scheme;
    }
    /**
     * Configures and returns the dataset
     * @param dataset
     * @param criteria
     * @return
     * @throws IOException
     */
    
    public static Data getData(BenchmarkDataset dataset) throws IOException {
        Data data = null;
        switch (dataset) {
        case ADULT:
            data = Data.create("data/adult.csv", Charset.defaultCharset(), ';');
            break;
        case ADULT_NC:
            data = Data.create("data/adult_nc.csv", Charset.defaultCharset(), ';');
            break;
        case ADULT_TN:
            data = Data.create("data/adult_tn.csv", Charset.defaultCharset(), ';');
            break;
        case ADULT_TN_SAFE_HARBOR:
            data = Data.create("data/adult_tn.csv", Charset.defaultCharset(), ';');
            break;
        case ATUS:
            data = Data.create("data/atus.csv", Charset.defaultCharset(), ';');
            break;
        case CUP:
            data = Data.create("data/cup.csv", Charset.defaultCharset(), ';');
            break;
        case FARS:
            data = Data.create("data/fars.csv", Charset.defaultCharset(), ';');
            break;
        case IHIS:
            data = Data.create("data/ihis.csv", Charset.defaultCharset(), ';');
            break;
        default:
            throw new RuntimeException("Invalid dataset");
        }
        
        for (String qi : getQuasiIdentifyingAttributes(dataset)) {
            data.getDefinition().setAttributeType(qi, getHierarchy(dataset, qi));
        }
        
        if (dataset == BenchmarkDataset.ADULT_TN_SAFE_HARBOR) {
            for (String qi : getQuasiIdentifyingAttributes(dataset)) {
                int max = data.getDefinition().getMaximumGeneralization(qi);
                data.getDefinition().setMaximumGeneralization(qi, max);
                data.getDefinition().setMinimumGeneralization(qi, max);
            }
        }
        
        return data;
    };
    /**
     * Default parameter
     * @return
     */
    public static int getDefaultAdversaryCost() {
        return 4;
    }
    
    /**
     * Default parameter
     * @return
     */
    public static int getDefaultAdversaryGain() {
        return 300;
    }
    
    /**
     * Default parameter
     * @return
     */
    public static int getDefaultPublisherBenefit() {
        return 1200;
    }

    /**
     * Default parameter
     * @return
     */
    public static int getDefaultPublisherLoss() {
        return 300;
    }

    /**
     * Returns a fitting delta for dataset
     * @param dataset
     * @return
     * @throws IOException
     */
    public static double getDelta(BenchmarkDataset dataset) {
        switch (dataset) {
        case ADULT:
            return 1e-5;
        case ATUS:
        case FARS:
            return 1e-6;
        case IHIS:
            return 1e-7;
        default:
            throw new RuntimeException("Invalid dataset");
        }
    }

    /**
     * Returns all relevant generalization degrees
     * @return
     */
    public static BenchmarkGeneralizationDegree[] getGeneralizationDegrees() {
        return new BenchmarkGeneralizationDegree[] {
                BenchmarkGeneralizationDegree.LOW,
                BenchmarkGeneralizationDegree.LOW_MIDDLE,
                BenchmarkGeneralizationDegree.MIDDLE_HIGH
        };
    }

    /**
     * Returns the according generalization level
     * @param height
     * @param generalization
     * @return
     */
    @SuppressWarnings("incomplete-switch")
    public static int getGeneralizationLevel(int height, BenchmarkGeneralizationDegree generalization) {

        if (generalization == BenchmarkGeneralizationDegree.NONE) {
            return 0;
        }

        switch (height) {
        case 2:
            switch (generalization) {
            case LOW:
                return 0;
            case LOW_MIDDLE:
                return 0;
            case MIDDLE_HIGH:
            case HIGH:
                return 0;
            }
        case 3:
            switch (generalization) {
            case LOW:
                return 1;
            case LOW_MIDDLE:
                return 1;
            case MIDDLE_HIGH:
            case HIGH:
                return 1;
            }
        case 4:
            switch (generalization) {
            case LOW:
                return 1;
            case LOW_MIDDLE:
                return 2;
            case MIDDLE_HIGH:
            case HIGH:
                return 2;
            }
        case 5:
            switch (generalization) {
            case LOW:
                return 1;
            case LOW_MIDDLE:
                return 2;
            case MIDDLE_HIGH:
            case HIGH:
                return 3;
            }
        case 6:
            switch (generalization) {
            case LOW:
                return 1;
            case LOW_MIDDLE:
                return 3;
            case MIDDLE_HIGH:
            case HIGH:
                return 4;
            }
        case 7:
            switch (generalization) {
            case LOW:
                return 1;
            case LOW_MIDDLE:
                return 3;
            case MIDDLE_HIGH:
            case HIGH:
                return 5;
            }
        case 8:
            switch (generalization) {
            case LOW:
                return 1;
            case LOW_MIDDLE:
                return 4;
            case MIDDLE_HIGH:
            case HIGH:
                return 6;
            }
        }
        throw new IllegalArgumentException("Unknown parameter");
    }
    
    /**
     * Returns the generalization hierarchy for the dataset and attribute
     * @param dataset
     * @param attribute
     * @return
     * @throws IOException
     */
    public static Hierarchy getHierarchy(BenchmarkDataset dataset, String attribute) throws IOException {
        switch (dataset) {
        case ADULT:
            return Hierarchy.create("hierarchies/adult_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case ADULT_NC:
            return Hierarchy.create("hierarchies/adult_nc_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case ADULT_TN:
            return Hierarchy.create("hierarchies/adult_tn_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case ADULT_TN_SAFE_HARBOR:
            return Hierarchy.create("hierarchies/adult_tn_safe_harbor_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case ATUS:
            return Hierarchy.create("hierarchies/atus_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case CUP:
            return Hierarchy.create("hierarchies/cup_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case FARS:
            return Hierarchy.create("hierarchies/fars_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        case IHIS:
            return Hierarchy.create("hierarchies/ihis_hierarchy_" + attribute + ".csv", Charset.defaultCharset(), ';');
        default:
            throw new RuntimeException("Invalid dataset");
        }
    }

    public static int getNumberOfRepetitions(BenchmarkDataset dataset) {
        if (dataset == BenchmarkDataset.ADULT_NC ||
            dataset == BenchmarkDataset.ADULT_TN) {
            return 100;
        } else {
            return 10;
        }
    }

    /**
     * Returns the number of records in the given dataset
     * @param dataset
     * @return
     * @throws IOException 
     */
    public static int getNumRecords(BenchmarkDataset dataset) throws IOException {
        return getData(dataset).getHandle().getNumRows();
    }

    /**
     * Parameters to benchmark
     * @return
     */
    public static double[] getParametersAdversaryCost() {
        return new double[]{1d, 1.01d, 1.1d, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 750, 1000, 1250, 1500, 1750, 2000};
    }
    
    /**
     * Parameters to benchmark
     * @return
     */
    public static double[] getParametersAdversaryGain() {
        return new double[]{1d, 1.01d, 1.1d, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 750, 1000, 1250, 1500, 1750, 2000};
    }

    /**
     * Parameters to benchmark
     * @return
     */
    public static double[] getParametersGainLoss() {
        return new double[]{0, 10, 100, 1000, 10000, 100000, 1000000};
    }
    
    /**
     * Parameters to benchmark
     * @return
     */
    public static double[] getParametersPublisherBenefit() {
        return new double[]{250, 500, 750, 1000, 1250, 1500, 1750, 2000};
    }
    
    /**
     * Parameters to benchmark
     * @return
     */
    public static double[] getParametersPublisherLoss() {
        return new double[]{0, 250, 500, 750, 1000, 1250, 1500, 1750, 2000};
    }

    /**
     * Returns the quasi-identifiers for the dataset
     * @param dataset
     * @return
     */
    public static String[] getQuasiIdentifyingAttributes(BenchmarkDataset dataset) {
        switch (dataset) {
        case ADULT:
            return new String[] {   "age",
                                    "education",
                                    "marital-status",
                                    "native-country",
                                    "race",
                                    "salary-class",
                                    "sex",
                                    "workclass",
                                    "occupation" };
        case ADULT_TN:
            return new String[] {   "sex", "age", "zip", "race"};
        case ADULT_TN_SAFE_HARBOR:
            return new String[] {   "sex", "age", "zip", "race"};
        case ADULT_NC:
            return new String[] {   "sex", "age", "zip", "race"};
        case ATUS:
            return new String[] {   "Age",
                                    "Birthplace",
                                    "Citizenship status",
                                    "Labor force status",
                                    "Marital status",
                                    "Race",
                                    "Region",
                                    "Sex",
                                    "Highest level of school completed" };
        case CUP:
            return new String[] {   "AGE",
                                    "GENDER",
                                    "INCOME",
                                    "MINRAMNT",
                                    "NGIFTALL",
                                    "STATE",
                                    "ZIP",
                                    "RAMNTALL" };
        case FARS:
            return new String[] {   "iage",
                                    "ideathday",
                                    "ideathmon",
                                    "ihispanic",
                                    "iinjury",
                                    "irace",
                                    "isex",
                                    "istatenum" };
        case IHIS:
            return new String[] {   "AGE",
                                    "MARSTAT",
                                    "PERNUM",
                                    "QUARTER",
                                    "RACEA",
                                    "REGION",
                                    "SEX",
                                    "YEAR",
                                    "EDUC" };
        default:
            throw new RuntimeException("Invalid dataset");
        }
    }
}
