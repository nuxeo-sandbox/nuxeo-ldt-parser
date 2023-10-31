package nuxeo.ldt.parser.test.service;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import nuxeo.ldt.parser.service.LDTParser;
import nuxeo.ldt.parser.service.LDTParserService;

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
}
