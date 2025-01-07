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

package nuxeo.ldt.parser.test.templates;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import jakarta.inject.Inject;
import java.io.File;

@RunWith(FeaturesRunner.class)
@Features({AutomationFeature.class})
@Deploy({
        "org.nuxeo.ecm.platform.convert",
        "nuxeo.ldt.parser.nuxeo-ldt-parser-core",
        // Deploy the template,
        "nuxeo.ldt.parser.nuxeo-ldt-parser-core:render-pdf-with-template/template-contrib.xml",
        // The automation chain,
        "nuxeo.ldt.parser.nuxeo-ldt-parser-core:render-pdf-with-template/with-any2pdf/automation-render-pdf-with-any2pdf.xml"
})
//any2pdf is a a converter provided by the platform out of the box. 
// It requires LibreOffice and its soffic e command line to be available
public class TestLDT2PDFTemplateWithAny2Pdf {

    @Inject
    protected AutomationService automationService;

    @Inject
    protected CoreSession session;
    
    @Inject
    protected ConversionService conversionService;

    @Test
    public void testRenderTemplate() throws Exception {
        
        boolean available = conversionService.isConverterAvailable("any2pdf").isAvailable();
        Assume.assumeTrue("any2pdf not available (soffice not available). Not doing the test", available);
        
        File f = FileUtils.getResourceFileFromContext("files/record.json");
        Blob blob = new FileBlob(f,"application/json");
        
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(blob);
        Blob pdf = (Blob) automationService.run(ctx, "javascript.test_render_pdf_with_any2pdf");
        Assert.assertNotNull(pdf);
        
        //f = new File("HERE YOUR PATH");
        //pdf.transferTo(f);
    }

}

