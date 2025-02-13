/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.externalsearch.pubmed.pmcoa;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.externalsearch.pubmed.traits.PubMedProviderTraits;

@Tag("slow")
class PmcOaClientTest
{
    private PubMedProviderTraits traits;
    private PmcOaClient sut;

    @BeforeEach
    public void setup() throws InterruptedException
    {
        Thread.sleep(1000); // Get around API rate limiting
        sut = new PmcOaClient();
    }

    @Test
    public void thatBiocWorks() throws Exception
    {
        String results = new String(sut.bioc(traits, "PMC8222896"), UTF_8);

        // System.out.println(results);

        assertThat(results).contains("Longitudinal symptoms in asthmatic COVID‐19 patients.");
    }
}
