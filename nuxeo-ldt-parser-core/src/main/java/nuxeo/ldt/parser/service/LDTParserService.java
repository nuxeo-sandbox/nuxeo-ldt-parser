package nuxeo.ldt.parser.service;

public interface LDTParserService {

    /**
     * If name is null or "", uses "default"
     * 
     * @param name
     * @return
     * @since 2021
     */
    LDTParser newParser(String name);
}
