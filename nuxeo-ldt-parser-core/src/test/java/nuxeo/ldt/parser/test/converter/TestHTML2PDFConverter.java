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
 */
package nuxeo.ldt.parser.test.converter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

@RunWith(FeaturesRunner.class)
@Features({PlatformFeature.class})
@Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core")
public class TestHTML2PDFConverter {

    @Inject
    protected CommandLineExecutorService commandLineExecutorService;

    @Inject
    protected ConversionService conversionService;

    @Test
    public void commandLineIsAvailable() {
        Assert.assertTrue(commandLineExecutorService.getCommandAvailability("wkhtmlToPdf").isAvailable());
    }

    @Test
    public void converterIsAvailable() {
        Assert.assertTrue(conversionService.isConverterAvailable("html2pdf").isAvailable());
    }

    @Test
    public void testConvert() {
        Blob blob = new FileBlob(new File(getClass().getResource("/files/statement.html").getPath()),"text/html");
        HashMap<String, Serializable> params = new HashMap<>();
        Blob pdf = conversionService.convert("html2pdf",new SimpleBlobHolder(blob),params).getBlob();
        Assert.assertNotNull("PDF is null",pdf);
    }

}

