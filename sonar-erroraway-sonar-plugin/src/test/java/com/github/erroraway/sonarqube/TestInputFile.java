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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

/**
 * @author Guillaume
 *
 */
public class TestInputFile implements InputFile {
	private Path path;
	private Path relativePath;
	private Charset charset;
	private Type type;

	public TestInputFile(Path path, Path relativePath, Charset charset, Type type) {
		this.path = path;
		this.relativePath = relativePath;
		this.charset = charset;
		this.type = type;
	}

	@Override
	public URI uri() {
		return path.toUri();
	}

	@Override
	public String filename() {
		return path.getFileName().toString();
	}

	@Override
	public String key() {
		return null;
	}

	@Override
	public boolean isFile() {
		return Files.isRegularFile(path);
	}

	@Override
	public String relativePath() {
		return relativePath.toString();
	}

	@Override
	public String absolutePath() {
		return path.toAbsolutePath().toString();
	}

	@Override
	public File file() {
		return path.toFile();
	}

	@Override
	public Path path() {
		return path;
	}

	@Override
	public String language() {
		return null;
	}

	@Override
	public Type type() {
		return type;
	}

	@Override
	public InputStream inputStream() throws IOException {
		return Files.newInputStream(path);
	}

	@Override
	public String contents() throws IOException {
		return Files.readString(path);
	}

	@Override
	@SuppressWarnings("deprecation")
	public Status status() {
		return null;
	}

	@Override
	public int lines() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public TextPointer newPointer(int line, int lineOffset) {
		return null;
	}

	@Override
	public TextRange newRange(TextPointer start, TextPointer end) {
		return null;
	}

	@Override
	public TextRange newRange(int startLine, int startLineOffset, int endLine, int endLineOffset) {
		return null;
	}

	@Override
	public TextRange selectLine(int line) {
		return null;
	}

	@Override
	public Charset charset() {
		return charset;
	}

	@Override
	public String toString() {
		return path.toString();
	}
	
	@Override
	public String md5Hash() {
		return null;
	}
}
