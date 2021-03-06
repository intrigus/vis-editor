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

package com.kotcrab.vis.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;

import java.util.Locale;

/**
 * Allows to easily load VisUI skin and change default title alignment, contains static version field
 * @author Kotcrab
 */
public class VisUI {
	/** Current VisUI version, does not include SNAPSHOT even if this version is snapshot */
	public static final String VERSION = "0.9.2";

	private static int defaultTitleAlign = Align.left;

	private static SkinScale scale;
	private static Skin skin;

	private static I18NBundle fileChooserBundle;
	private static I18NBundle dialogUtilsBundle;
	private static I18NBundle tabbedPaneBundle;
	private static I18NBundle colorPickerBundle;

	public enum SkinScale {
		X1 {
			@Override
			public FileHandle getSkinFile () {
				return Gdx.files.classpath("com/kotcrab/vis/ui/skin/x1/uiskin.json");
			}

			@Override
			public String getSizesName () {
				return "default";
			}
		},
		X2 {
			@Override
			public FileHandle getSkinFile () {
				return Gdx.files.classpath("com/kotcrab/vis/ui/skin/x2/uiskin.json");
			}

			@Override
			public String getSizesName () {
				return "x2";
			}
		};

		public abstract FileHandle getSkinFile ();

		public abstract String getSizesName ();
	}

	/** Loads default VisUI skin */
	public static void load () {
		load(SkinScale.X1);
	}

	/** Loads default VisUI skin */
	public static void load (SkinScale scale) {
		VisUI.scale = scale;
		load(scale.getSkinFile());
	}

	/** Loads skin from provided file, skin must be compatible with default VisUI skin */
	public static void load (FileHandle visSkinFile) {
		skin = new Skin(visSkinFile);
	}

	/** Sets provided skin as default for every VisUI widget, skin must be compatible with default VisUI skin */
	public static void load (Skin skin) {
		VisUI.skin = skin;
	}

	/** Unloads skin */
	public static void dispose () {
		if (skin != null) {
			skin.dispose();
			skin = null;
		}
	}

	public static Skin getSkin () {
		if (skin == null) throw new IllegalStateException("VisUI is not loaded!");
		return skin;
	}

	public static Sizes getSizes () {
		if (scale == null)
			return getSkin().get(Sizes.class);
		else
			return getSkin().get(scale.getSizesName(), Sizes.class);
	}

	/** @return int value from {@link Align} */
	public static int getDefaultTitleAlign () {
		return defaultTitleAlign;
	}

	/**
	 * Sets default title align user for VisWindow and VisDialog
	 * @param defaultTitleAlign int value from {@link Align}
	 */
	public static void setDefaultTitleAlign (int defaultTitleAlign) {
		VisUI.defaultTitleAlign = defaultTitleAlign;
	}

	/** Returns I18N bundle used by FileChooser, if current bundle is null, a default bundle is set and returned */
	public static I18NBundle getFileChooserBundle () {
		if (fileChooserBundle == null) {
			FileHandle file = Gdx.files.classpath("com/kotcrab/vis/ui/i18n/FileChooser");
			fileChooserBundle = I18NBundle.createBundle(file, new Locale("en"));
		}

		return fileChooserBundle;
	}

	/**
	 * Changes bundle used by FileChooser, will not affect already created FileChoosers.
	 * If set to null then {@link #getFileChooserBundle()} will return default bundle
	 */
	public static void setFileChooserBundle (I18NBundle fileChooserBundle) {
		VisUI.fileChooserBundle = fileChooserBundle;
	}

	/** Returns I18N bundle used by DialogUtils, if current bundle is null, a default bundle is set and returned */
	public static I18NBundle getDialogUtilsBundle () {
		if (dialogUtilsBundle == null) {
			FileHandle file = Gdx.files.classpath("com/kotcrab/vis/ui/i18n/DialogUtils");
			dialogUtilsBundle = I18NBundle.createBundle(file, new Locale("en"));
		}

		return dialogUtilsBundle;
	}

	/**
	 * Changes bundle used by DialogUtils, will not affect already created dialogs.
	 * If set to null then {@link #getDialogUtilsBundle()} will return default bundle
	 */
	public static void setDialogUtilsBundle (I18NBundle dialogUtilsBundle) {
		VisUI.dialogUtilsBundle = dialogUtilsBundle;
	}

	/** Returns I18N bundle used by TabbedPane, if current bundle is null, a default bundle is set and returned */
	public static I18NBundle getTabbedPaneBundle () {
		if (tabbedPaneBundle == null) {
			FileHandle file = Gdx.files.classpath("com/kotcrab/vis/ui/i18n/TabbedPane");
			tabbedPaneBundle = I18NBundle.createBundle(file, new Locale("en"));
		}

		return tabbedPaneBundle;
	}

	/**
	 * Changes bundle used by TabbedPane, will not affect already created TabbedPane.
	 * If set to null then {@link #getTabbedPaneBundle()} will return default bundle
	 */
	public static void setTabbedPaneBundle (I18NBundle tabbedPaneBundle) {
		VisUI.tabbedPaneBundle = tabbedPaneBundle;
	}

	/** Returns I18N bundle used by ColorPicker, if current bundle is null, a default bundle is set and returned */
	public static I18NBundle getColorPickerBundle () {
		if (colorPickerBundle == null) {
			FileHandle file = Gdx.files.classpath("com/kotcrab/vis/ui/i18n/ColorPicker");
			colorPickerBundle = I18NBundle.createBundle(file, new Locale("en"));
		}

		return colorPickerBundle;
	}

	/**
	 * Changes bundle used by ColorPicker, will not affect already created pickers.
	 * If set to null then {@link #getColorPickerBundle()} will return default bundle
	 */
	public static void setColorPickerBundle (I18NBundle colorPickerBundle) {
		VisUI.colorPickerBundle = colorPickerBundle;
	}
}
