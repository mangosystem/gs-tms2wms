package org.geoserver.web.data.store.raster;

import org.apache.wicket.markup.html.form.Form;

@SuppressWarnings("serial")
public class TMSRasterEditPanel extends AbstractRasterFileEditPanel {
	public TMSRasterEditPanel(String componentId, Form storeEditForm) {
        super(
                componentId,
                storeEditForm,
                new String[] {".properties"});
    }
}
