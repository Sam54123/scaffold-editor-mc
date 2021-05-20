package org.scaffoldeditor.editormc.ui.attribute_types;

import org.scaffoldeditor.scaffold.level.entity.attribute.Attribute;

import javafx.scene.Node;
import javafx.scene.control.TextField;

public class DefaultAttributeType implements IRenderAttributeType {

	@Override
	public Node createSetter(String name, Attribute<?> defaultValue) {
		TextField field = new TextField("Unable to edit attribute type: " + defaultValue.registryName);
		field.setEditable(false);
		return field;
	}
	
}
