package nuxeo.ldt.parser.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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

    /**
     * Application started notification.
     * Called after the application started.
     * You can do here any initialization that requires a working application
     * (all resolved bundles and components are active at that moment)
     *
     * @param context the component context. Use it to get the current bundle context
     * @throws Exception
     */
    @Override
    public void applicationStarted(ComponentContext context) {
        // do nothing by default. You can remove this method if not used.
    }

    @Override
    public void registerExtension(Extension extension) {
      super.registerExtension(extension);
      
      if(!EXT_POINT.equals(extension.getExtensionPoint())) {
          // Nothing? log? Throw?
          // (nothing)
      }

      Object[] contribs = extension.getContributions();
      if(contribs != null) {
          for (Object contrib : contribs) {
              LDTParserDescriptor desc = (LDTParserDescriptor) contrib;
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
        if(StringUtils.isBlank(name)) {
            name = "default";
        }
        return new LDTParser(contributions.get(name));
    }
}
