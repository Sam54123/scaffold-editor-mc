package org.scaffoldeditor.editormc.sub_editors.blocktexture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.FilenameUtils;
import org.scaffoldeditor.scaffold.block_textures.SerializableBlockTexture;
import org.scaffoldeditor.scaffold.core.Project;
import org.scaffoldeditor.scaffold.io.AssetManager;
import org.scaffoldeditor.scaffold.io.AssetType;
import org.scaffoldeditor.scaffold.serialization.BlockTextureWriter;
import org.scaffoldeditor.scaffold.util.event.EventDispatcher;
import org.scaffoldeditor.scaffold.util.event.EventListener;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.control.Alert.AlertType;

public class BlockTextureEditor {
	
	public static class SaveBlockTextureEvent {
		public final boolean isExternal;
		public final String assetPath;
		public final SerializableBlockTexture newTexture;
		
		public SaveBlockTextureEvent(boolean isExternal, String assetPath, SerializableBlockTexture texture) {
			this.isExternal = isExternal;
			this.assetPath = assetPath;
			this.newTexture = texture;
		}
	}
	
	@FXML
	private CheckBox useExternal;
	@FXML
	private TextField filePath;
	@FXML
	private Button browseButton;
	@FXML
	private Button loadButton;
	@FXML
	private Button saveExternallyButton;
	private SerializableBlockTexture texture;
	private EventDispatcher<SaveBlockTextureEvent> dispatcher = new EventDispatcher<>();
	private String assetPath = "";
	private Stage stage;
	
	@FXML
	private void initialize() {
		useExternal.selectedProperty().addListener(listener -> {
			setUseExternalFile(useExternal.isSelected());
		});
		filePath.textProperty().addListener(listener -> {
			loadButton.setDisable(filePath.getText().length() == 0);
		});
		setBlockTexture(SerializableBlockTexture.DEFAULT);
	}
	
	/**
	 * Save the block texture to file if it's stored externally. Call {@link #onSaved}
	 * regardless.
	 */
	@FXML
	public void save() {
		if (isExternal()) {
			AssetManager manager = AssetManager.getInstance();
			if (getAssetPath() == null || getAssetPath().length() == 0) {
				File target = openSaveDialog();
				if (target == null) {
					return;
				}
				String newPath = manager.relativise(target);
				filePath.setText(newPath);
				this.assetPath = newPath;
			}
						
			AssetType<?> loader = manager.getLoader(assetPath);
			if (loader == null || !loader.isAssignableTo(SerializableBlockTexture.class)) {
				error("Block textures are not savable to the file type ."+FilenameUtils.getExtension(assetPath));
				return;
			}
			
			if (!manager.isWritable(getAssetPath())) {
				Alert alert = new Alert(AlertType.WARNING);
				alert.setHeaderText("Loaded block texture is not in a writable directory!");
				alert.setContentText("Would you like to save a new copy in the project folder?");
				alert.getButtonTypes().setAll(ButtonType.NO, ButtonType.YES);
				
				Optional<ButtonType> result = alert.showAndWait();
				if (!(result.isPresent() && result.get().equals(ButtonType.YES))) {
					return;
				}
			}
			
			// Save the file.
			File targetFile = manager.getAbsoluteFile(getAssetPath());
			System.out.println("Saving block texture to "+targetFile);
			try {
				new BlockTextureWriter(new FileOutputStream(targetFile)).write(texture);
				manager.forceCache(assetPath, texture);
			} catch (IOException e) {
				e.printStackTrace();
				error("An error has occured while saving the file. See console for details.");
			} catch (TransformerException e) {
				throw new AssertionError("Unable to serialize .blocktexture.", e);
			}
		}
		
		dispatcher.fire(new SaveBlockTextureEvent(isExternal(), assetPath, texture));
	}
	
	@FXML
	public void cancel() {
		stage.close();
	}
	
	@FXML
	private void load() {
		loadBlockTexture(filePath.getText());
	}
			
	/**
	 * Set the block texture that this editor is editing.
	 * 
	 * @param texture Block texture to set. Will not be updated. Use
	 *                {@link #getBlockTexture()} to get the edited value.
	 */
	public void setBlockTexture(SerializableBlockTexture texture) {
		this.texture = texture.clone();
		setUseExternalFile(false);
	}
	
	public void setEnteredAsset(File target) {
		if (target != null) {
			filePath.setText(AssetManager.getInstance().relativise(target));
		}
	}
	
	/**
	 * Try to a block texture asset.
	 * @param assetPath Local asset path.
	 * @return Success.
	 */
	public boolean loadBlockTexture(String assetPath) {
		if (assetPath == null || assetPath.length() == 0) return false;
		
		AssetManager manager = AssetManager.getInstance();
		if (!manager.getLoader(assetPath).isAssignableTo(SerializableBlockTexture.class)) {
			error("Block textures are not loadable from the file type ."+FilenameUtils.getExtension(assetPath));
			return false;
		};
		try {
			texture = (SerializableBlockTexture) manager.loadAsset(assetPath, true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			error("Unable to find the file specified. See console for details.");
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			error("An unknown error occured. See console for details.");
			return false;
		}
		
		this.assetPath = assetPath;
		filePath.setText(assetPath);
		
		if (!isExternal()) {
			useExternal.setSelected(true);
		}
		
		return true;
	}
	
	/**
	 * Get the block texture that the editor is editing.
	 */
	public SerializableBlockTexture getBlockTexture() {
		return texture;
	}
	
	private void setUseExternalFile(boolean useExternal) {
		filePath.setDisable(!useExternal);
		browseButton.setDisable(!useExternal);
		loadButton.setDisable(!(useExternal && filePath.getText().length() > 0));
		saveExternallyButton.setDisable(useExternal);
		
		if (useExternal) {
			if (filePath.getText().length() != 0) {
				load();
			}
		} else {
			assetPath = "";
		}
		
		if (this.useExternal.isSelected() != useExternal) {
			this.useExternal.setSelected(useExternal);
		}
	}
	
	public boolean isExternal() {
		return useExternal.isSelected();
	}
	
	/**
	 * Get the external asset path the editor is referencing, if any.
	 * @return The asset path to the block texture.
	 */
	public String getAssetPath() {
		return assetPath;
	}
	
	public void onSaved(EventListener<SaveBlockTextureEvent> listener) {
		dispatcher.addListener(listener);
	}
	
	public Stage getStage() {
		return stage;
	}
	
	private void error(String message) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("IO Error");
		alert.setHeaderText("Error saving/loading block texture.");
		alert.setContentText(message);
		alert.showAndWait();
	}
	
	private File openSaveDialog() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Save blocktexture file");
		chooser.getExtensionFilters().add(new ExtensionFilter("BlockTexture Files", "*.texture"));
		chooser.setInitialDirectory(AssetManager.getInstance().getProject().getProjectFolder().toFile());
		File selectedFile = chooser.showSaveDialog(stage);
		if (selectedFile == null) return null;
		
		String filePath = selectedFile.getAbsolutePath();
		if (!filePath.endsWith(".texture")) {
			filePath = filePath+".texture";
		}
		
		return new File(filePath);
	}
	
	/**
	 * Open the block texture editor.
	 * @param parent Parent window.
	 * @return The opened editor.
	 */
	public static BlockTextureEditor open(Window parent) {
		FXMLLoader loader = new FXMLLoader(BlockTextureEditor.class.getResource("/assets/scaffold/ui/blocktexture_editor.fxml"));
		Parent root;
		try {
			root = loader.load();
		} catch (IOException e) {
			throw new AssertionError("Unable to load blocktexture editor UI!", e);
		}
		
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initOwner(parent);
		stage.setTitle("Edit BlockTexture");
		
		Scene scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
		
		BlockTextureEditor controller = loader.getController();
		controller.stage = stage;
		
		return controller;
	}
}
