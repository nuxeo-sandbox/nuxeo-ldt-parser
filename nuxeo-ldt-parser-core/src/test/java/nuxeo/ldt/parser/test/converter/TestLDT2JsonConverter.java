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
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;


/*
 * ***** ALL THIS TEST CLASS IS @TODO
 */


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

    @Ignore
    @Test
    public void testConversionSuccess() throws IOException {
        Blob blob = new FileBlob(new File(getClass().getResource("/files/SOME-FILE.LDT").getPath()),"application/ldt");
        HashMap<String, Serializable> params = new HashMap<>();
        params.put("clientId","099900001X100000");
        params.put("taxId", "00005622612810");
        params.put("month","FEVEREIRO");
        params.put("year","2021");
        params.put("targetFileName","test.json");
        BlobHolder conversionResult = conversionService.convert("ldt2json",new SimpleBlobHolder(blob),params);
        Blob jsonBlob = conversionResult.getBlob();
        //System.out.println(jsonBlob.getString());
        Assert.assertNotNull(jsonBlob);
        Assert.assertEquals("test.json",jsonBlob.getFilename());
        Assert.assertTrue(jsonBlob.getLength()>0);
    }

    @Ignore
    @Test(expected = ConversionException.class)
    public void testConversionFailure() {
        Blob blob = new FileBlob(new File(getClass().getResource("/files/SOME-FILE.LDT").getPath()),"application/ldt");
        HashMap<String, Serializable> params = new HashMap<>();
        params.put("clientId","099900001X100000");
        params.put("taxId", "00005622612810");
        params.put("month","FEVEREIRO");
        params.put("year","2022");
        params.put("targetFileName","test.json");
        conversionService.convert("ldt2json",new SimpleBlobHolder(blob),params);
    }

    @Ignore
    @Test(expected = ConversionException.class)
    public void testMissingParams() {
        Blob blob = new FileBlob(new File(getClass().getResource("/files/SOME-FILE.LDT").getPath()),"application/ldt");
        HashMap<String, Serializable> params = new HashMap<>();
        params.put("clientId","099900001X100000");
        params.put("taxId", "00005622612810");
        params.put("targetFileName","test.json");
        conversionService.convert("ldt2json",new SimpleBlobHolder(blob),params);
    }
    /*
     ng nxql = "SELECT * FROM AccountStatement WHERE accountstatement:taxId = '00007915107860'";
        nxql += " AND accountstatement:clientId = '099900008X100000'";
        nxql += " AND accountstatement:month = 'FEVEREIRO'";
        nxql += " AND accountstatement:year = '2021'";
     */
    @Ignore
    @Test
    public void testConversionSuccessWithLineNumber() throws IOException {
        Blob blob = new FileBlob(new File(getClass().getResource("/test.LDT").getPath()),"application/ldt");
        HashMap<String, Serializable> params = new HashMap<>();
        params.put("clientId","099900008X100000");
        params.put("taxId", "00007915107860");
        params.put("month","FEVEREIRO");
        params.put("year","2021");
        // ========================================
        params.put("lineStart","26");
        // ========================================
        params.put("targetFileName","test.json");
        BlobHolder conversionResult = conversionService.convert("ldt2json",new SimpleBlobHolder(blob),params);
        Blob jsonBlob = conversionResult.getBlob();
        //System.out.println(jsonBlob.getString());
        Assert.assertNotNull(jsonBlob);
        Assert.assertEquals("test.json",jsonBlob.getFilename());
        Assert.assertTrue(jsonBlob.getLength()>0);
    }

}

