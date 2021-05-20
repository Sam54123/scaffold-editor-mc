package org.scaffoldeditor.editormc.ui.attribute_types;

import org.scaffoldeditor.editormc.ui.EntityEditor;
import org.scaffoldeditor.scaffold.level.entity.attribute.Attribute;
import org.scaffoldeditor.scaffold.level.entity.attribute.StringAttribute;

import javafx.scene.Node;
import javafx.scene.control.TextField;

public class StringAttributeType implements IRenderAttributeType {

	@Override
	public Node createSetter(String name, Attribute<?> defaultValue) {
		if (!(defaultValue instanceof StringAttribute)) {
			return EntityEditor.DEFAULT_ATTRIBUTE_TYPE.createSetter(name, defaultValue);
		}
		StringAttribute attribute = (StringAttribute) defaultValue;
		TextField field = new TextField();
		field.setText(attribute.getValue());
		
		field.focusedProperty().addListener(e -> {
			if (!field.getText().matches(attribute.getValue())) {
				field.fireEvent(new ChangeAttributeEvent(ChangeAttributeEvent.ATTRIBUTE_CHANGED, name,
						new StringAttribute(field.getText())));
			}
		});

		return field;
	}

}
