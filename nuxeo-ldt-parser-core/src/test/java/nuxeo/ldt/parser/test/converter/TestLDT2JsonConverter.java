/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Michael Vachette
 *     Thibaud Arguillere
 */

package nuxeo.ldt.parser.test.converter;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import nuxeo.ldt.parser.test.TestUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

@RunWith(FeaturesRunner.class)
@Features({PlatformFeature.class})
@Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core")
public class TestLDT2JsonConverter {

    @Inject
    protected ConversionService conversionService;


    @Test
    public void converterIsAvailable() {
        Assert.assertTrue(conversionService.isConverterAvailable("ldt2json").isAvailable());
    }

    @Test
    public void testConversionSuccess() throws IOException {

        Blob blob = TestUtils.getSimpleTestFileBlob();
        HashMap<String, Serializable> params = new HashMap<>();
        // Use "default" parser
        //params.put("parserName", null);
        params.put("startOffset", "" + TestUtils.SIMPLELDT_RECORD2_STARTOFFSET);
        params.put("recordSize","" + TestUtils.SIMPLELDT_RECORD2_RECORDSIZE);
        params.put("targetFileName","test.json");
        BlobHolder conversionResult = conversionService.convert("ldt2json", new SimpleBlobHolder(blob),params);
        Blob jsonBlob = conversionResult.getBlob();
        Assert.assertNotNull(jsonBlob);
        Assert.assertEquals("test.json", jsonBlob.getFilename());
        
        TestUtils.checkSimpleTestFileRecord2Values(jsonBlob.getString());
        
    }

    @Ignore
    @Test(expected = ConversionException.class)
    public void testConversionFailure() {
        
        Blob blob = TestUtils.getSimpleTestFileBlob();
        HashMap<String, Serializable> params = new HashMap<>();
        // Dummy values
        params.put("startOffset", "" + TestUtils.SIMPLELDT_RECORD2_STARTOFFSET);
        params.put("recordSize","" + TestUtils.SIMPLELDT_RECORD2_RECORDSIZE);
        params.put("targetFileName","test.json");
        
        conversionService.convert("ldt2json",new SimpleBlobHolder(blob),params);
    }

    @Test(expected = ConversionException.class)
    public void testMissingParams() {

        Blob blob = TestUtils.getSimpleTestFileBlob();
        HashMap<String, Serializable> params = new HashMap<>();
        
        //params.put("startOffset", "" + TestUtils.SIMPLELDT_RECORD2_STARTOFFSET);
        params.put("recordSize","" + TestUtils.SIMPLELDT_RECORD2_RECORDSIZE);
        params.put("targetFileName","test.json");
        
        conversionService.convert("ldt2json", new SimpleBlobHolder(blob),params);
        
    }

}

