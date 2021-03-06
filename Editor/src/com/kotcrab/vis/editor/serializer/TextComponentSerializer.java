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

package com.kotcrab.vis.editor.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.kotcrab.vis.editor.entity.PixelsPerUnitComponent;
import com.kotcrab.vis.editor.module.project.FontCacheModule;
import com.kotcrab.vis.runtime.assets.VisAssetDescriptor;
import com.kotcrab.vis.runtime.component.AssetComponent;
import com.kotcrab.vis.runtime.component.TextComponent;

/**
 * @author Kotcrab
 */
public class TextComponentSerializer extends EntityComponentSerializer<TextComponent> {
	private static final int VERSION_CODE = 1;

	private FontCacheModule fontCache;

	public TextComponentSerializer (Kryo kryo, FontCacheModule fontCache) {
		super(kryo, TextComponent.class);
		this.fontCache = fontCache;
	}

	@Override
	public void write (Kryo kryo, Output output, TextComponent textObject) {
		super.write(kryo, output, textObject);
		parentWrite(kryo, output, textObject);

		output.writeInt(VERSION_CODE);
		kryo.writeClassAndObject(output, getComponent(AssetComponent.class).asset);
		output.writeFloat(getComponent(PixelsPerUnitComponent.class).pixelsPerUnits);
	}

	@Override
	public TextComponent read (Kryo kryo, Input input, Class<TextComponent> type) {
		super.read(kryo, input, type);
		TextComponent component = parentRead(kryo, input, type);

		input.readInt(); //version code

		VisAssetDescriptor asset = (VisAssetDescriptor) kryo.readClassAndObject(input);
		float pixelsPerUnits = input.readFloat();
		component.setFont(fontCache.getGeneric(asset, pixelsPerUnits));

		return component;
	}

	@Override
	public TextComponent copy (Kryo kryo, TextComponent original) {
		super.copy(kryo, original);
		return new TextComponent(original);
	}
}
