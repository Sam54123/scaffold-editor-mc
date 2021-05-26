package org.scaffoldeditor.editormc.ui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.scaffoldeditor.editormc.ui.attribute_types.BooleanAttributeType;
import org.scaffoldeditor.editormc.ui.attribute_types.ChangeAttributeEvent;
import org.scaffoldeditor.editormc.ui.attribute_types.DefaultAttributeType;
import org.scaffoldeditor.editormc.ui.attribute_types.DoubleAttributeType;
import org.scaffoldeditor.editormc.ui.attribute_types.FloatAttributeType;
import org.scaffoldeditor.editormc.ui.attribute_types.IRenderAttributeType;
import org.scaffoldeditor.editormc.ui.attribute_types.IntAttributeType;
import org.scaffoldeditor.editormc.ui.attribute_types.LongAttributeType;
import org.scaffoldeditor.editormc.ui.attribute_types.StringAttributeType;
import org.scaffoldeditor.editormc.ui.attribute_types.VectorAttributeType;
import org.scaffoldeditor.editormc.ui.controllers.FXMLEntityEditorController;
import org.scaffoldeditor.scaffold.level.entity.Entity;
import org.scaffoldeditor.scaffold.level.entity.attribute.Attribute;
import org.scaffoldeditor.scaffold.level.entity.attribute.BooleanAttribute;
import org.scaffoldeditor.scaffold.level.entity.attribute.DoubleAttribute;
import org.scaffoldeditor.scaffold.level.entity.attribute.FloatAttribute;
import org.scaffoldeditor.scaffold.level.entity.attribute.IntAttribute;
import org.scaffoldeditor.scaffold.level.entity.attribute.LongAttribute;
import org.scaffoldeditor.scaffold.level.entity.attribute.StringAttribute;
import org.scaffoldeditor.scaffold.level.entity.attribute.VectorAttribute;
import org.scaffoldeditor.scaffold.operation.ChangeAttributesOperation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class EntityEditor {
	public final Stage parent;
	public final Entity entity;
	public final Map<String, IRenderAttributeType> attributeTypes = new HashMap<>();
	protected Scene scene;
	protected Stage stage;
	protected Map<String, Attribute<?>> cachedAttributes = new HashMap<>();
	protected FXMLEntityEditorController controller;
	
	public static final IRenderAttributeType DEFAULT_ATTRIBUTE_TYPE = new DefaultAttributeType();
	
	protected GridPane attributePane;
	
	public EntityEditor(Stage parent, Entity entity) {		
		this.parent = parent;
		this.entity = entity;
		this.stage = new Stage();
		initDefaultAttributeTypes();
		
		Parent root;
		FXMLLoader loader;
		try {
			loader = new FXMLLoader(getClass().getResource("/assets/scaffold/ui/entity_editor.fxml"));
			root = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}		
		
		scene = new Scene(root, 600, 400);
		stage.setTitle("Edit "+entity.getName());
		stage.setScene(scene);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initOwner(parent);
		stage.setResizable(false);
		
		controller = loader.getController();	
		controller.nameField.setText(entity.getName());	
		controller.entityTypeLabel.setText(entity.registryName);
		attributePane = controller.attributePane;
		loadAttributes();
		
		scene.addEventHandler(ChangeAttributeEvent.ATTRIBUTE_CHANGED, e -> {
			cachedAttributes.put(e.name, e.newValue);
		});
		
		controller.applyButton.setOnAction(e -> {
			apply();
		});
		
		controller.applyAndCloseButton.setOnAction(e -> {
			apply();
			close();
		});
	}
	
	protected void initDefaultAttributeTypes() {
		attributeTypes.put(StringAttribute.REGISTRY_NAME, new StringAttributeType());
		attributeTypes.put(BooleanAttribute.REGISTRY_NAME, new BooleanAttributeType());
		attributeTypes.put(IntAttribute.REGISTRY_NAME, new IntAttributeType());
		attributeTypes.put(LongAttribute.REGISTRY_NAME, new LongAttributeType());
		attributeTypes.put(FloatAttribute.REGISTRY_NAME, new FloatAttributeType());
		attributeTypes.put(DoubleAttribute.REGISTRY_NAME, new DoubleAttributeType());
		attributeTypes.put(VectorAttribute.REGISTRY_NAME, new VectorAttributeType());
	}
	
	protected void loadAttributes() {
		int i = 2;
		for (String name : entity.getAttributes()) {
			Attribute<?> attribute = entity.getAttribute(name);
			
			Node setter;
			if (attributeTypes.containsKey(attribute.registryName)) {
				setter = attributeTypes.get(attribute.registryName).createSetter(name, attribute);
			} else {
				setter = DEFAULT_ATTRIBUTE_TYPE.createSetter(name, attribute);
			}		
			Label label = new Label(name);
			
			attributePane.add(label, 1, i);
			attributePane.add(setter, 2, i);
			
			i++;
		}
	}
	
	public void show() {
		stage.show();
	}
	
	public void close() {
		stage.close();
	}
	
	public void apply() {
		if (cachedAttributes.size() > 0) {
			entity.getLevel().getOperationManager().execute(new ChangeAttributesOperation(entity, cachedAttributes));
		}
	}
}