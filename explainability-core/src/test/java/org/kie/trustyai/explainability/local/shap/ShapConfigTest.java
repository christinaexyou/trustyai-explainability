/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.trustyai.explainability.local.shap;

import org.apache.commons.math3.linear.RealMatrix;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.PerturbationContext;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.utils.MatrixUtilsExtensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.*;

class ShapConfigTest {
    PerturbationContext pc = new PerturbationContext(new Random(), 0);
    List<Feature> fs = Arrays.asList(
            FeatureFactory.newNumericalFeature("f", 1.),
            FeatureFactory.newNumericalFeature("f", 2.));
    PredictionInput pi = new PredictionInput(fs);
    List<PredictionInput> pis = Arrays.asList(pi, pi);
    List<PredictionInput> piEmpty = new ArrayList<>();
    RealMatrix piMatrix = MatrixUtilsExtensions.matrixFromPredictionInput(pis);

    // Test that everything recovers as expected
    @Test
    void testRecovery() {
        ExecutorService executor = ForkJoinPool.commonPool();
        ShapConfig skConfig = ShapConfig.builder()
                .withLink(ShapConfig.LinkType.IDENTITY)
                .withBackground(pis)
                .withPC(pc)
                .withExecutor(executor)
                .withNSamples(100)
                .withConfidence(.99)
                .build();
        assertEquals(ShapConfig.LinkType.IDENTITY, skConfig.getLink());
        assertTrue(skConfig.getNSamples().isPresent());
        assertEquals(100, skConfig.getNSamples().get());
        assertEquals(.99, skConfig.getConfidence());
        assertSame(pc, skConfig.getPC());
        assertSame(executor, skConfig.getExecutor());
        assertSame(pis, skConfig.getBackground());
        assertArrayEquals(piMatrix.getData(), skConfig.getBackgroundMatrix().getData());
    }

    // Test that the default arguments recover as expected
    @Test
    void testNullRecovery() {
        ShapConfig skConfig = ShapConfig.builder()
                .withLink(ShapConfig.LinkType.LOGIT)
                .withBackground(pis)
                .build();
        assertEquals(ShapConfig.LinkType.LOGIT, skConfig.getLink());
        assertFalse(skConfig.getNSamples().isPresent());
        assertSame(pis, skConfig.getBackground());
        assertArrayEquals(piMatrix.getData(), skConfig.getBackgroundMatrix().getData());
        assertSame(ForkJoinPool.commonPool(), skConfig.getExecutor());
        assertFalse(skConfig.getNSamples().isPresent());
    }

    // Test that not setting mandatory arguments throws errors
    @Test
    void testMandatoryErrors() {
        ShapConfig.Builder linkNoBG = ShapConfig.builder().withLink(ShapConfig.LinkType.IDENTITY);
        ShapConfig.Builder bgNoLink = ShapConfig.builder().withBackground(pis);
        assertThrows(IllegalArgumentException.class, linkNoBG::build);
        assertThrows(IllegalArgumentException.class, bgNoLink::build);

    }

    @Test
    void testEmptyBackgroundMandatoryErrors() {
        ShapConfig.Builder emptyBG = ShapConfig.builder()
                .withLink(ShapConfig.LinkType.IDENTITY)
                .withBackground(piEmpty);
        assertThrows(IllegalArgumentException.class, emptyBG::build);

    }
}
