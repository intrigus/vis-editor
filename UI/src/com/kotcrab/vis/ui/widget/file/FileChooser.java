/*
 * Copyright 2014-2015 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotcrab.vis.ui.widget.file;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.I18NBundle;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileFilter;

import static com.kotcrab.vis.ui.widget.file.FileChooserText.*;

/**
 * Chooser for files, before using {@link FileChooser#setFavoritesPrefsName(String)} should be called.
 * FileChooser is heavy widget and should be reused whenever possible.
 * @author Kotcrab
 * @since 0.1.0
 */
public class FileChooser extends VisWindow {
	private Mode mode;
	private SelectionMode selectionMode = SelectionMode.FILES;
	private boolean multiselectionEnabled = false;
	private FileChooserListener listener;
	private int groupMultiselectKey = Keys.SHIFT_LEFT;
	private int multiselectKey = Keys.CONTROL_LEFT;

	private FileFilter fileFilter = new DefaultFileFilter();
	private FileHandle currentDirectory;

	private FileChooserStyle style;
	private I18NBundle bundle;

	private FileSystemView fileSystemView = FileSystemView.getFileSystemView();

	private Array<FileItem> selectedItems = new Array<FileItem>();
	private ShortcutItem selectedShortcut;

	private Array<FileHandle> history = new Array<FileHandle>();
	private Array<FileHandle> historyForward = new Array<FileHandle>();

	private FavoritesIO favoritesIO;
	private Array<FileHandle> favorites;

	private Array<ShortcutItem> fileRootsCache = new Array<ShortcutItem>();

	private VisTable fileTable;
	private VisScrollPane fileScrollPane;
	private VisTable fileScrollPaneTable;

	private VisTable shortcutsTable;
	private VisScrollPane shortcutsScrollPane;
	private VisTable shortcutsScrollPaneTable;

	private VisImageButton backButton;
	private VisImageButton forwardButton;
	private VisTextField currentPath;
	private VisTextField selectedFileTextField;

	private FilePopupMenu fileMenu;

	public FileChooser (Mode mode) {
		super("");

		this.bundle = VisUI.getFileChooserBundle();
		this.mode = mode;

		getTitleLabel().setText(getText(TITLE_CHOOSE_FILES));

		style = VisUI.getSkin().get(FileChooserStyle.class);

		init();
	}

	public FileChooser (String title, Mode mode) {
		this("default", title, mode);
	}

	public FileChooser (String styleName, String title, Mode mode) {
		super(title);
		this.mode = mode;
		this.bundle = VisUI.getFileChooserBundle();

		style = VisUI.getSkin().get(styleName, FileChooserStyle.class);

		init();
	}

	public FileChooser (I18NBundle bundle, Mode mode) {
		super("");
		this.mode = mode;
		this.bundle = bundle;
		getTitleLabel().setText(getText(TITLE_CHOOSE_FILES));

		style = VisUI.getSkin().get(FileChooserStyle.class);

		init();
	}

	public FileChooser (I18NBundle bundle, String title, Mode mode) {
		super(title);
		this.mode = mode;
		this.bundle = bundle;

		style = VisUI.getSkin().get(FileChooserStyle.class);

		init();
	}

	/**
	 * Sets file name that will be used to store favorites, if not changed default will be used that may be shared with other
	 * programs, should be package name e.g. com.seriouscompay.seriousprogram
	 */
	public static void setFavoritesPrefsName (String name) {
		FavoritesIO.setFavoritesPrefsName(name);
	}

	private void init () {
		setModal(true);
		setResizable(true);
		setMovable(true);
		addCloseButton();
		closeOnEscape();

		favoritesIO = new FavoritesIO();
		favoritesIO.checkIfUsingDefaultName();
		favorites = favoritesIO.loadFavorites();

		createToolbar();
		createCenterContentPanel();
		createFileTextBox();
		createBottomButtons();

		fileMenu = new FilePopupMenu(style.popupMenuStyleName, this, bundle);

		rebuildShortcutsList();

		setDirectory(Gdx.files.absolute(System.getProperty("user.home")), HistoryPolicy.IGNORE);
		setSize(500, 600);
		centerWindow();

		validateSettings();

		addListener(new InputListener() {
			@Override
			public boolean keyDown (InputEvent event, int keycode) {
				if (keycode == Keys.A && Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)) {
					selectAll();
					return true;
				}

				return false;
			}
		});
	}

	private String getText (FileChooserText text) {
		return bundle.get(text.getName());
	}

	private void createToolbar () {
		VisTable toolbarTable = new VisTable(true);
		toolbarTable.defaults().minWidth(30).right();
		add(toolbarTable).fillX().expandX().pad(3).padRight(2);

		backButton = new VisImageButton(style.iconArrowLeft);
		backButton.setGenerateDisabledImage(true);
		backButton.setDisabled(true);
		forwardButton = new VisImageButton(style.iconArrowRight);
		forwardButton.setGenerateDisabledImage(true);
		forwardButton.setDisabled(true);

		currentPath = new VisTextField();

		currentPath.addListener(new InputListener() {
			@Override
			public boolean keyDown (InputEvent event, int keycode) {
				if (keycode == Keys.ENTER) {
					FileHandle file = Gdx.files.absolute(currentPath.getText());
					if (file.exists())
						setDirectory(file, HistoryPolicy.ADD);
					else {
						showDialog(getText(POPUP_DIRECTORY_DOES_NOT_EXIST));
						currentPath.setText(currentDirectory.path());
					}
				}
				return false;
			}
		});

		VisImageButton folderParentButton = new VisImageButton(style.iconFolderParent);
		folderParentButton.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				FileHandle parent = currentDirectory.parent();

				//if current directory is drive root (eg. "C:/") navigating to parent
				//would navigate to "/" which would work but it is bad for UX
				if (FileUtils.isWindows() && currentDirectory.path().endsWith(":/")) return;

				setDirectory(parent, HistoryPolicy.ADD);
			}
		});

		toolbarTable.add(backButton);
		toolbarTable.add(forwardButton);
		toolbarTable.add(currentPath).expand().fill();
		toolbarTable.add(folderParentButton);

		backButton.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				historyBack();
			}
		});

		forwardButton.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				historyForward();
			}
		});
	}

	private void createCenterContentPanel () {
		// fileTable is contained in fileScrollPane contained in fileScrollPaneTable contained in splitPane
		// same for shortcuts
		fileTable = new VisTable();
		fileScrollPane = createScrollPane(fileTable);
		fileScrollPaneTable = new VisTable();
		fileScrollPaneTable.add(fileScrollPane).pad(2).top().expand().fillX();

		shortcutsTable = new VisTable();
		shortcutsScrollPane = createScrollPane(shortcutsTable);
		shortcutsScrollPaneTable = new VisTable();
		shortcutsScrollPaneTable.add(shortcutsScrollPane).pad(2).top().expand().fillX();

		VisSplitPane splitPane = new VisSplitPane(shortcutsScrollPaneTable, fileScrollPaneTable, false);
		splitPane.setSplitAmount(0.3f);
		splitPane.setMinSplitAmount(0.05f);
		splitPane.setMaxSplitAmount(0.8913f);

		row();
		add(splitPane).expand().fill();
		row();
	}

	private void createFileTextBox () {
		VisTable table = new VisTable(true);
		VisLabel nameLabel = new VisLabel(getText(FILE_NAME));
		selectedFileTextField = new VisTextField();

		table.add(nameLabel);
		table.add(selectedFileTextField).expand().fill();

		selectedFileTextField.addListener(new InputListener() {
			@Override
			public boolean keyTyped (InputEvent event, char character) {
				deselectAll(false);
				return false;
			}
		});

		add(table).expandX().fillX().pad(3).padRight(2).padBottom(2f);
		row();

	}

	private void createBottomButtons () {
		VisTextButton cancelButton = new VisTextButton(getText(CANCEL));
		VisTextButton confirmButton = new VisTextButton(mode == Mode.OPEN ? getText(OPEN) : getText(SAVE));

		VisTable buttonTable = new VisTable(true);
		buttonTable.defaults().minWidth(70).right();
		add(buttonTable).padTop(3).padBottom(3).padRight(2).fillX().expandX();

		buttonTable.add(cancelButton).expand().right();
		buttonTable.add(confirmButton);

		cancelButton.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				fadeOut();
				listener.canceled();
			}
		});

		confirmButton.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				selectionFinished();
			}
		});
	}

	private void selectionFinished () {
		if (selectedItems.size == 1) {
			// only files allowed but directory is selected?
			// navigate to that directory!
			if (selectionMode == SelectionMode.FILES) {
				FileHandle selected = selectedItems.get(0).file;
				if (selected.isDirectory()) {
					setDirectory(selected, HistoryPolicy.ADD);
					return;
				}
			}

			// only directories allowed but file is selected?
			// display dialog :(
			if (selectionMode == SelectionMode.DIRECTORIES) {
				FileHandle selected = selectedItems.get(0).file;
				if (selected.isDirectory() == false) {
					showDialog(getText(POPUP_ONLY_DIRECTORIES));
					return;
				}
			}
		}

		if (selectedItems.size > 0 || mode == Mode.SAVE) {
			Array<FileHandle> files = getFileListFromSelected();
			notifyListenerAndCloseDialog(files);
		} else {
			if (selectionMode == SelectionMode.FILES)
				showDialog(getText(POPUP_CHOOSE_FILE));
			else {
				Array<FileHandle> files = new Array<FileHandle>();
				files.add(currentDirectory);
				notifyListenerAndCloseDialog(files);
			}
		}
	}

	@Override
	protected void close () {
		listener.canceled();
		super.close();
	}

	private void notifyListenerAndCloseDialog (Array<FileHandle> files) {
		if (files == null) return;

		listener.selected(files);
		listener.selected(files.get(0));

		fadeOut();
	}

	private VisScrollPane createScrollPane (VisTable table) {
		VisScrollPane scrollPane = new VisScrollPane(table);
		scrollPane.setOverscroll(false, true);
		scrollPane.setFadeScrollBars(false);
		scrollPane.setScrollingDisabled(true, false);
		return scrollPane;
	}

	private Array<FileHandle> getFileListFromSelected () {
		Array<FileHandle> list = new Array<FileHandle>();

		if (mode == Mode.OPEN) {
			for (FileItem f : selectedItems)
				list.add(f.file);

			return list;
		} else if (selectedItems.size > 0) {
			for (FileItem f : selectedItems)
				list.add(f.file);

			showOverwriteQuestion(list);
			return null;
		} else {
			String fileName = selectedFileTextField.getText();
			FileHandle file = currentDirectory.child(fileName);

			if (FileUtils.isValidFileName(fileName) == false) {
				showDialog(getText(POPUP_FILENAME_INVALID));
				return null;
			}

			if (file.exists()) {
				list.add(file);
				showOverwriteQuestion(list);

				return null;
			} else {
				list.add(file);
				return list;
			}
		}

	}

	private void showDialog (String text) {
		VisDialog dialog = new VisDialog(getText(POPUP_TITLE));
		dialog.text(text);
		dialog.button(getText(POPUP_OK));
		dialog.pack();
		dialog.centerWindow();
		getStage().addActor(dialog.fadeIn());
	}

	private void showOverwriteQuestion (Array<FileHandle> filesList) {
		VisDialog dialog = new VisDialog(getText(POPUP_TITLE)) {
			@Override
			@SuppressWarnings("unchecked")
			protected void result (Object object) {
				notifyListenerAndCloseDialog((Array<FileHandle>) object);
			}
		};
		dialog.text(filesList.size == 1 ? getText(POPUP_FILE_EXIST_OVERWRITE) : getText(POPUP_MULTIPLE_FILE_EXIST_OVERWRITE));
		dialog.button(getText(POPUP_NO), null);
		dialog.button(getText(POPUP_YES), filesList);
		dialog.pack();
		dialog.centerWindow();
		getStage().addActor(dialog.fadeIn());
	}

	private void rebuildShortcutsList (boolean rebuildRootCache) {
		shortcutsTable.clear();

		String userHome = System.getProperty("user.home");
		String userName = System.getProperty("user.name");

		// yes, getHomeDirectory returns path to desktop
		File desktop = fileSystemView.getHomeDirectory();
		shortcutsTable.add(new ShortcutItem(desktop, getText(DESKTOP), style.iconFolder)).expand().fill().row();
		shortcutsTable.add(new ShortcutItem(new File(userHome), userName, style.iconFolder)).expand().fill().row();

		shortcutsTable.addSeparator();

		if (rebuildRootCache) rebuildFileRootsCache();

		for (ShortcutItem item : fileRootsCache)
			shortcutsTable.add(item).expandX().fillX().row();

		Array<FileHandle> favourites = favoritesIO.loadFavorites();

		if (favourites.size > 0) {
			shortcutsTable.addSeparator();

			for (FileHandle f : favourites)
				shortcutsTable.add(new ShortcutItem(f.file(), f.name(), style.iconFolder)).expand().fill().row();
		}
	}

	private void rebuildShortcutsList () {
		rebuildShortcutsList(true);
	}

	private void rebuildFileRootsCache () {
		fileRootsCache.clear();

		File[] roots = File.listRoots();

		for (int i = 0; i < roots.length; i++) {
			File root = roots[i];
			ShortcutItem item;

			if (mode == Mode.OPEN ? root.canRead() : root.canWrite()) {
				String displayName = fileSystemView.getSystemDisplayName(root);

				if (displayName != null && displayName.equals("/"))
					item = new ShortcutItem(root, getText(FileChooserText.COMPUTER), style.iconDrive);
				else if (displayName != null && displayName.equals("") == false)
					item = new ShortcutItem(root, displayName, style.iconDrive);
				else
					item = new ShortcutItem(root, root.toString(), style.iconDrive);

				fileRootsCache.add(item);
			}
		}
	}

	private void rebuildFileList () {
		deselectAll();

		fileTable.clear();
		FileHandle[] files = currentDirectory.list(fileFilter);
		currentPath.setText(currentDirectory.path());

		if (files.length == 0) return;

		Array<FileHandle> fileList = FileUtils.sortFiles(files);

		for (FileHandle f : fileList)
			if (f.file() == null || f.file().isHidden() == false)
				fileTable.add(new FileItem(f, null)).expand().fill().row();

		fileScrollPane.setScrollX(0);
		fileScrollPane.setScrollY(0);
	}

	/** Refresh chooser lists content */
	public void refresh () {
		rebuildShortcutsList();
		rebuildFileList();
	}

	/**
	 * Adds favorite to favorite list
	 * @param favourite to be added
	 */
	public void addFavorite (FileHandle favourite) {
		favorites.add(favourite);
		favoritesIO.saveFavorites(favorites);
		rebuildShortcutsList(false);
	}

	/**
	 * Removes favorite from current favorite list
	 * @param favorite to be removed (path to favorite)
	 * @return true if favorite was removed, false otherwise
	 */
	public boolean removeFavorite (FileHandle favorite) {
		boolean removed = favorites.removeValue(favorite, false);
		favoritesIO.saveFavorites(favorites);
		rebuildShortcutsList(false);
		return removed;
	}

	public void setVisble (boolean visible) {
		if (isVisible() == false && visible)
			deselectAll(); // reset selected item when dialog is changed from invisible to visible

		super.setVisible(visible);
	}

	private void deselectAll () {
		deselectAll(true);
	}

	private void deselectAll (boolean updateTextField) {
		for (FileItem item : selectedItems)
			item.deselect(false);

		selectedItems.clear();
		if (updateTextField) setSelectedFileFieldText();
	}

	private void selectAll () {
		Array<Cell> cells = fileTable.getCells();
		for (Cell c : cells) {
			FileItem item = (FileItem) c.getActor();
			item.select(false);
		}

		removeInvalidSelections();
		setSelectedFileFieldText();
	}

	private void setSelectedFileFieldText () {
		if (selectedItems.size == 0)
			selectedFileTextField.setText("");
		else if (selectedItems.size == 1)
			selectedFileTextField.setText(selectedItems.get(0).file.name());
		else {
			StringBuilder b = new StringBuilder();

			for (FileItem item : selectedItems) {
				b.append('"');
				b.append(item.file.name());
				b.append("\" ");
			}

			selectedFileTextField.setText(b.toString());
		}
	}

	private void removeInvalidSelections () {
		if (selectionMode == SelectionMode.FILES) {
			for (FileItem item : selectedItems)
				if (item.file.isDirectory()) item.deselect();
		}

		if (selectionMode == SelectionMode.DIRECTORIES) {
			for (FileItem item : selectedItems)
				if (item.file.isDirectory() == false) item.deselect();
		}
	}

	private void historyClear () {
		history.clear();
		historyForward.clear();
		forwardButton.setDisabled(true);
		backButton.setDisabled(true);
	}

	private void historyAdd () {
		history.add(currentDirectory);
		historyForward.clear();
		backButton.setDisabled(false);
		forwardButton.setDisabled(true);
	}

	private void historyBack () {
		FileHandle dir = history.pop();
		historyForward.add(currentDirectory);
		setDirectory(dir, HistoryPolicy.IGNORE);

		if (history.size == 0)
			backButton.setDisabled(true);

		forwardButton.setDisabled(false);
	}

	private void historyForward () {
		FileHandle dir = historyForward.pop();
		history.add(currentDirectory);
		setDirectory(dir, HistoryPolicy.IGNORE);

		if (historyForward.size == 0)
			forwardButton.setDisabled(true);

		backButton.setDisabled(false);
	}

	public Mode getMode () {
		return mode;
	}

	public void setMode (Mode mode) {
		this.mode = mode;
	}

	public void setDirectory (String directory) {
		setDirectory(Gdx.files.absolute(directory), HistoryPolicy.CLEAR);
	}

	public void setDirectory (File directory) {
		setDirectory(Gdx.files.absolute(directory.getAbsolutePath()), HistoryPolicy.CLEAR);
	}

	public void setDirectory (FileHandle directory) {
		setDirectory(directory, HistoryPolicy.CLEAR);
	}

	public void setDirectory (FileHandle directory, HistoryPolicy historyPolicy) {
		if (directory.equals(currentDirectory)) return;
		if (directory.exists() == false) throw new IllegalStateException("Provided directory does not exist!");
		if (directory.isDirectory() == false)
			throw new IllegalStateException("Provided path is a file, not directory!");

		if (historyPolicy == HistoryPolicy.ADD) historyAdd();

		currentDirectory = directory;
		rebuildFileList();

		if (historyPolicy == HistoryPolicy.CLEAR) historyClear();
	}

	public FileFilter getFileFilter () {
		return fileFilter;
	}

	public void setFileFilter (FileFilter fileFilter) {
		this.fileFilter = fileFilter;
		rebuildFileList();
	}

	public SelectionMode getSelectionMode () {
		return selectionMode;
	}

	/**
	 * Changes selection mode, also updates the title of this file chooser to match current selection mode
	 * (eg. Choose file, Choose directory etc.)
	 */
	public void setSelectionMode (SelectionMode selectionMode) {
		this.selectionMode = selectionMode;

		switch (selectionMode) {
			case FILES:
				getTitleLabel().setText(getText(TITLE_CHOOSE_FILES));
				break;
			case DIRECTORIES:
				getTitleLabel().setText(getText(TITLE_CHOOSE_DIRECTORIES));
				break;
			case FILES_AND_DIRECTORIES:
				getTitleLabel().setText(getText(TITLE_CHOOSE_FILES_AND_DIRECTORIES));
				break;
		}
	}

	public boolean isMultiselectionEnabled () {
		return multiselectionEnabled;
	}

	public void setMultiselectionEnabled (boolean multiselectionEnabled) {
		this.multiselectionEnabled = multiselectionEnabled;
	}

	public void setListener (FileChooserListener listener) {
		this.listener = listener;
		validateSettings();
	}

	public int getMultiselectKey () {
		return multiselectKey;
	}

	/** @param multiselectKey from {@link Keys} */
	public void setMultiselectKey (int multiselectKey) {
		this.multiselectKey = multiselectKey;
	}

	public int getGroupMultiselectKey () {
		return groupMultiselectKey;
	}

	/** @param groupMultiselectKey from {@link Keys} */
	public void setGroupMultiselectKey (int groupMultiselectKey) {
		this.groupMultiselectKey = groupMultiselectKey;
	}

	private void validateSettings () {
		if (listener == null) listener = new FileChooserAdapter();
	}

	@Override
	protected void setStage (Stage stage) {
		super.setStage(stage);
		deselectAll();
	}

	public enum Mode {OPEN, SAVE}

	public enum SelectionMode {FILES, DIRECTORIES, FILES_AND_DIRECTORIES}

	public enum HistoryPolicy {ADD, CLEAR, IGNORE}

	private class DefaultFileFilter implements FileFilter {
		@Override
		public boolean accept (File f) {
			if (f.isHidden()) return false;
			if (mode == Mode.OPEN ? f.canRead() == false : f.canWrite() == false) return false;

			return true;
		}
	}

	private class FileItem extends Table {
		public FileHandle file;
		private VisLabel name;
		private VisLabel size;

		public FileItem (final FileHandle file, Drawable icon) {
			this.file = file;
			setTouchable(Touchable.enabled);
			name = new VisLabel(file.name());
			name.setEllipsis(true);

			if (file.isDirectory())
				size = new VisLabel("");
			else
				size = new VisLabel(FileUtils.readableFileSize(file.length()));

			if (icon == null && file.isDirectory()) icon = style.iconFolder;

			if (icon != null) add(new Image(icon)).padTop(3);
			Cell<VisLabel> labelCell = add(name).padLeft(icon == null ? 22 : 0);

			labelCell.width(new Value() {
				@Override
				public float get (Actor context) {
					int padding = file.isDirectory() ? 35 : 60;
					return fileScrollPaneTable.getWidth() - getUsedWidth() - padding;
				}
			});

			add(size).expandX().right().padRight(6);

			addListener();
		}

		private int getUsedWidth () {
			@SuppressWarnings("rawtypes")
			Array<Cell> cells = getCells();

			int width = 0;
			for (Cell<?> cell : cells) {
				if (cell.getActor() == name) continue;
				width += cell.getActor().getWidth();
			}

			return width;
		}

		private void addListener () {
			addListener(new InputListener() {
				@Override
				public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
					return true;
				}

				@Override
				public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
					if (event.getButton() == Buttons.RIGHT) {
						fileMenu.build(favorites, file);
						fileMenu.showMenu(getStage(), event.getStageX(), event.getStageY());
					}
				}
			});

			addListener(new ClickListener() {
				@Override
				public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
					if (selectedShortcut != null) selectedShortcut.deselect();

					if (multiselectionEnabled == false || (Gdx.input.isKeyPressed(multiselectKey) == false && Gdx.input.isKeyPressed(groupMultiselectKey) == false))
						deselectAll();

					boolean itemSelected = select();

					if (selectedItems.size > 1 && multiselectionEnabled && Gdx.input.isKeyPressed(groupMultiselectKey))
						selectGroup();

					if (selectedItems.size > 1) removeInvalidSelections();

					setSelectedFileFieldText();

					// very fast selecting and deselecting folder would navigate to that folder
					// return false will protect against that (tap count won't be increased)
					if (itemSelected == false) return false;

					return super.touchDown(event, x, y, pointer, button);
				}

				@Override
				public void clicked (InputEvent event, float x, float y) {
					super.clicked(event, x, y);
					if (getTapCount() == 2 && selectedItems.contains(FileItem.this, true)) {
						FileHandle file = FileItem.this.file;
						if (file.isDirectory()) {
							setDirectory(file, HistoryPolicy.ADD);
						} else
							selectionFinished();
					}
				}

				private void selectGroup () {
					Array<Cell> cells = fileTable.getCells();

					int thisSelectionIndex = getItemId(cells, FileItem.this);
					int lastSelectionIndex = getItemId(cells, selectedItems.get(selectedItems.size - 2));

					int start;
					int end;

					if (thisSelectionIndex > lastSelectionIndex) {
						start = lastSelectionIndex;
						end = thisSelectionIndex;
					} else {
						start = thisSelectionIndex;
						end = lastSelectionIndex;
					}

					for (int i = start; i < end; i++) {
						FileItem item = (FileItem) cells.get(i).getActor();
						item.select(false);
					}
				}

				private int getItemId (Array<Cell> cells, FileItem item) {
					for (int i = 0; i < cells.size; i++) {
						if (cells.get(i).getActor() == item) return i;
					}

					throw new IllegalStateException("Item not found in cells");
				}
			});
		}

		/** Selects this items, if item is already in selectedList it will be deselected */
		private boolean select () {
			return select(true);
		}

		private boolean select (boolean deselectIfAlreadySelected) {
			if (deselectIfAlreadySelected && selectedItems.contains(this, true)) {
				deselect();
				return false;
			}

			setBackground(style.highlight);
			if (selectedItems.contains(this, true) == false) selectedItems.add(this);
			return true;
		}

		private void deselect () {
			deselect(true);
		}

		private void deselect (boolean removeFromList) {
			setBackground((Drawable) null);
			if (removeFromList) selectedItems.removeValue(this, true);
		}

	}

	private class ShortcutItem extends Table {
		public File file;
		private VisLabel name;

		/** Used only by shortcuts panel */
		public ShortcutItem (final File file, String customName, Drawable icon) {
			this.file = file;
			name = new VisLabel(customName);
			name.setEllipsis(true);
			add(new Image(icon)).padTop(3);
			Cell<VisLabel> labelCell = add(name).expand().fill().padRight(6);

			labelCell.width(new Value() {
				@Override
				public float get (Actor context) {
					return shortcutsScrollPaneTable.getWidth() - getUsedWidth() - 10;
				}
			});

			addListener();
		}

		private int getUsedWidth () {
			@SuppressWarnings("rawtypes")
			Array<Cell> cells = getCells();

			int width = 0;
			for (Cell<?> cell : cells) {
				if (cell.getActor() == name) continue;
				width += cell.getActor().getWidth();
			}

			return width;
		}

		private void addListener () {
			addListener(new InputListener() {
				@Override
				public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
					return true;
				}

				@Override
				public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
					if (event.getButton() == Buttons.RIGHT) {
						fileMenu.buildForFavorite(favorites, file);
						fileMenu.showMenu(getStage(), event.getStageX(), event.getStageY());
					}
				}
			});

			addListener(new ClickListener() {
				@Override
				public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
					deselectAll();
					setSelectedFileFieldText();
					select();
					return super.touchDown(event, x, y, pointer, button);
				}

				@Override
				public void clicked (InputEvent event, float x, float y) {
					super.clicked(event, x, y);

					if (getTapCount() == 1) {
						File file = ShortcutItem.this.file;
						if (file.exists() == false) {
							showDialog(getText(POPUP_DIRECTORY_DOES_NOT_EXIST));
							return;
						}

						if (file.isDirectory()) {
							setDirectory(Gdx.files.absolute(file.getAbsolutePath()), HistoryPolicy.ADD);
							getStage().setScrollFocus(fileScrollPane);
						}
					}
				}
			});
		}

		private void select () {
			if (selectedShortcut != null) selectedShortcut.deselect();
			selectedShortcut = ShortcutItem.this;
			setBackground(style.highlight);
		}

		private void deselect () {
			setBackground((Drawable) null);
		}

	}

}
