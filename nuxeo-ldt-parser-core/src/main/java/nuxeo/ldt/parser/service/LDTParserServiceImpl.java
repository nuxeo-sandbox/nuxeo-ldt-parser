package nuxeo.ldt.parser.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

import nuxeo.ldt.parser.service.descriptors.LDTParserDescriptor;

public class LDTParserServiceImpl extends DefaultComponent implements LDTParserService {

    protected static final String EXT_POINT = "ldtParser";

    protected Map<String, LDTParserDescriptor> contributions = new HashMap<String, LDTParserDescriptor>();

    /**
     * Component activated notification.
     * Called when the component is activated. All component dependencies are resolved at that moment.
     * Use this method to initialize the component.
     *
     * @param context the component context.
     */
    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
    }

    /**
     * Component deactivated notification.
     * Called before a component is unregistered.
     * Use this method to do cleanup if any and free any resources held by the component.
     *
     * @param context the component context.
     */
    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    @Override
    public void registerExtension(Extension extension) {
        super.registerExtension(extension);

        if (!EXT_POINT.equals(extension.getExtensionPoint())) {
            // Nothing? log? Throw?
            // (nothing)
        }

        Object[] contribs = extension.getContributions();
        if (contribs != null) {
            for (Object contrib : contribs) {
                LDTParserDescriptor desc = (LDTParserDescriptor) contrib;
                // Sanity check on some items. Just logging the warinng, not throwing an error
                desc.checkDescriptor(false);
                contributions.put(desc.getName(), desc);
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        super.unregisterExtension(extension);
        contributions = null;
    }

    @Override
    public LDTParser newParser(String name) {
        if (contributions == null) {
            throw new NuxeoException("No ldtParser contribution loaded.");
        }
        if (StringUtils.isBlank(name)) {
            name = "default";
        }
        LDTParserDescriptor desc = contributions.get(name);
        if (desc == null) {
            return null;
        }
        return new LDTParser(contributions.get(name));
    }
}
