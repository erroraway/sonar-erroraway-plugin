/*
 * Copyright 2022 The ErrorAway Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.erroraway.sonarqube;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;

import org.sonar.api.batch.fs.InputFile;

/**
 * @author Guillaume
 *
 */
public class InputFileJavaFileObject implements JavaFileObject {
	private InputFile file;

	public InputFileJavaFileObject(InputFile file) {
		this.file = file;
	}

	@Override
	public URI toUri() {
		return file.uri();
	}

	@Override
	public String getName() {
		return file.uri().toString();
	}

	@Override
	public InputStream openInputStream() throws IOException {
		return file.inputStream();
	}

	@Override
	public OutputStream openOutputStream() throws IOException {
		throw new UnsupportedOperationException("Cannot open output stream on input file");
	}

	@Override
	public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
		return new InputStreamReader(openInputStream(), file.charset());
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		return file.contents();
	}

	@Override
	public Writer openWriter() throws IOException {
		throw new UnsupportedOperationException("Cannot open writer on input file");
	}

	@Override
	public long getLastModified() {
		// Not known so we return zero
		return 0L;
	}

	@Override
	public boolean delete() {
		return false;
	}

	@Override
	public Kind getKind() {
		return Kind.SOURCE;
	}

	@Override
	public boolean isNameCompatible(String simpleName, Kind kind) {
		String baseName = simpleName + kind.extension;
		return kind.equals(getKind()) && (baseName.equals(toUri().getPath()) || toUri().getPath().endsWith("/" + baseName));
	}

	@Override
	public NestingKind getNestingKind() {
		// Not known so we return null
		return null;
	}

	@Override
	public Modifier getAccessLevel() {
		// Not known so we return null
		return null;
	}
}
