package nuxeo.ldt.parser.service;

public interface LDTParserService {
    
    /**
     * If name is null or "", uses "default"
     * 
     * @param name
     * @return
     * @since TODO
     */
    LDTParser getParser(String name);
    
    /**
     * If name is null or "", uses "default"
     * 
     * @param name
     * @return
     * @since TODO
     */
    LDTParserDescriptor getDescriptor(String name);
}
