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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.TempFolder;

import com.github.erroraway.ErrorAwayException;

/**
 * @author Guillaume Toison
 *
 */
@ScannerSide
public class ErrorAwayDependencyManager {

	private TempFolder tempFolder;
	private Configuration configuration;

	private RepositorySystem repositorySystem;
	private LocalRepository localRepository;
	private List<RemoteRepository> remoteRepositories;
	private boolean workOffline;

	public ErrorAwayDependencyManager(TempFolder tempFolder, Configuration configuration) {
		this.tempFolder = tempFolder;
		this.configuration = configuration;

		workOffline = configuration.getBoolean(ErrorAwayPlugin.MAVEN_WORK_OFFLINE).orElse(false);

		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
		repositorySystem = locator.getService(RepositorySystem.class);

		Settings settings = settings(configuration);

		localRepository = localRepository(settings);

		remoteRepositories = remoteRepositories();
	}

	public List<File> downloadDependencies(String... coordinates) {
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		session.setOffline(workOffline);
		if (!workOffline) {
			session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
		}
		session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN);
		session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository));

		List<ArtifactRequest> requests = new ArrayList<>();

		for (String c : coordinates) {
			Artifact artifact = new DefaultArtifact(c);
			ArtifactRequest request = new ArtifactRequest(artifact, remoteRepositories, null);

			requests.add(request);
		}

		try {
			List<ArtifactResult> artifactResults = repositorySystem.resolveArtifacts(session, requests);

			return artifactResults.stream()
					.map(ArtifactResult::getArtifact)
					.map(Artifact::getFile)
					.toList();
		} catch (ArtifactResolutionException e) {
			throw new ErrorAwayException("Error resolving " + Arrays.toString(coordinates) + " from " + remoteRepositories, e);
		}
	}

	private Settings settings(Configuration configuration) {
		SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
		SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();

		File userSettingsFile;
		Optional<String> userSettingsFileValue = configuration.get(ErrorAwayPlugin.MAVEN_USER_SETTINGS_FILE);
		if (userSettingsFileValue.isPresent()) {
			userSettingsFile = new File(userSettingsFileValue.get());
		} else {
			File userHome = new File(System.getProperty("user.home")).getAbsoluteFile();
			userSettingsFile = new File(userHome, File.separator + ".m2" + File.separator + "settings.xml");
		}

		request.setUserSettingsFile(userSettingsFile);

		try {
			return builder.build(request).getEffectiveSettings();
		} catch (SettingsBuildingException e) {
			throw new ErrorAwayException("Error build settings from " + userSettingsFile, e);
		}
	}

	private LocalRepository localRepository(Settings settings) {
		File localRepositoryDir;
		Optional<String> localRepositoryValue = configuration.get(ErrorAwayPlugin.MAVEN_LOCAL_REPOSITORY);
		Optional<Boolean> useTemporaryLocalRepositoryValue = configuration.getBoolean(ErrorAwayPlugin.MAVEN_USE_TEMP_LOCAL_REPOSITORY);

		if (localRepositoryValue.isPresent()) {
			localRepositoryDir = new File(localRepositoryValue.get());
		} else if (useTemporaryLocalRepositoryValue.orElse(false).booleanValue()) {
			localRepositoryDir = tempFolder.newDir("repository");
		} else if (StringUtils.isNotEmpty(settings.getLocalRepository())) {
			localRepositoryDir = new File(settings.getLocalRepository());
		} else {
			File userHome = new File(System.getProperty("user.home")).getAbsoluteFile();
			localRepositoryDir = new File(userHome, File.separator + ".m2" + File.separator + "repository");
		}

		return new LocalRepository(localRepositoryDir);
	}

	private List<RemoteRepository> remoteRepositories() {
		String[] configurationRepositories = configuration.getStringArray(ErrorAwayPlugin.MAVEN_REPOSITORIES);
		List<RemoteRepository> repositories = new ArrayList<>();
		int i = 0;

		if (configurationRepositories != null) {
			for (String r : configurationRepositories) {
				RemoteRepository.Builder builder = new RemoteRepository.Builder("repo-" + i++, "default", r);

				repositories.add(builder.build());
			}
		}

		return repositories;
	}
}
