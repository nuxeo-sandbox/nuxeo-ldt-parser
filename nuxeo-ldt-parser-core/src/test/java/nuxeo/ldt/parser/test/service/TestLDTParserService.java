package nuxeo.ldt.parser.test.service;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import nuxeo.ldt.parser.service.LDTParser;
import nuxeo.ldt.parser.service.LDTParserService;
import nuxeo.ldt.parser.service.descriptors.LDTParserDescriptor;

import javax.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core")
public class TestLDTParserService {

    @Inject
    protected LDTParserService ldtParserService;

    @Test
    public void testService() {
        assertNotNull(ldtParserService);
    }
    
    @Test
    public void hasDefaultParser() {
        LDTParser parser = ldtParserService.newParser(null);
        assertNotNull(parser);
    }
    
    @Test (expected = NuxeoException.class)
    @Deploy("nuxeo.ldt.parser.nuxeo-ldt-parser-core:ldt-parsers-test-contrib.xml")
    public void shouldFailOnWrongConfiguration() {
        LDTParser parser = ldtParserService.newParser("test-config-should-fail");
        assertNotNull(parser);
        
        LDTParserDescriptor desc = parser.getDescriptor();
        desc.checkDescriptor(true);
    }
}
