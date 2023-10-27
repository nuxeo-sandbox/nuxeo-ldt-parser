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
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RunWith(FeaturesRunner.class)
@Features({AutomationFeature.class})
@Deploy({
        "org.nuxeo.ecm.platform.convert",
        "nuxeo.ldt.parser.nuxeo-ldt-parser-core",
        "nuxeo.ldt.parser.nuxeo-ldt-parser-core:test-automation-render-pdf-contrib.xml"
})
public class TestLDT2PDFTemplate {

    @Inject
    protected AutomationService automationService;

    @Inject
    CoreSession session;

    @Ignore
    @Test
    public void testRenderTemplate() throws OperationException {
        Blob blob = new FileBlob(new File(getClass().getResource("/files/statement.json").getPath()),"application/json");
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();
        ctx.setInput(blob);
        Blob pdf = (Blob) automationService.run(ctx, "javascript.test_render_pdf", params);
        Assert.assertNotNull(pdf);
    }

}

