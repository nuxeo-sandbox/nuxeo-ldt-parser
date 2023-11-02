package nuxeo.ldt.parser.service;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Implementors have to provide an {@Code LDTParser}. See the documentation of LDTParser for more details.
 * 
 * @since 2021
 */
public interface LDTParserService {

    /**
     * If name is null or "", uses "default"
     * If no "ldtParser" configuration with name (or "default") is found, returns null
     * 
     * @param name
     * @return
     * @since 2021
     */
    LDTParser newParser(String name);
}
