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

package com.kotcrab.vis.editor.module.project.assetsmanager;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;

/**
 * Displays single folder in assets manager folders tree
 * @author Kotcrab
 */
public class FolderItem extends Table {
	private FileHandle file;
	private VisLabel name;

	public FolderItem (FileHandle file) {
		this.file = file;
		name = new VisLabel(file.name(), "small");
		name.setEllipsis(true);
		add(new Image(VisUI.getSkin().getDrawable("icon-folder"))).size(20).padTop(3);
		add(name).expand().fill().padRight(6);
	}

	public FileHandle getFile () {
		return file;
	}
}
