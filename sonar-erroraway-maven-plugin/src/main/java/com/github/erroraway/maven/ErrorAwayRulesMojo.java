/**
 * 
 */
package com.github.erroraway.maven;

import static com.github.erroraway.rules.ErrorAwayRulesMapping.NULLAWAY_REPOSITORY;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.json.JSONObject;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;

import com.github.erroraway.rules.ErrorAwayRulesMapping;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;
/**
 * @author gtoison
 *
 */
@Mojo(name = "rules", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class ErrorAwayRulesMojo extends AbstractMojo {

	private static final String[] DESCRIPTION_FOLDERS = new String[] { 
			null, 
			"android", 
			"argumentselectiondefects", 
			"flogger", 
			"inject", 
			"javadoc", 
			"nullness", 
			"time" };
	
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;
	
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
	File outputDirectory;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Collection<BugCheckerInfo> errorProneBugCheckerInfos = new ArrayList<>();
		errorProneBugCheckerInfos.addAll(BuiltInCheckerSuppliers.ENABLED_WARNINGS);
		errorProneBugCheckerInfos.addAll(BuiltInCheckerSuppliers.ENABLED_ERRORS);
		
		processCheckers(ErrorAwayRulesMapping.ERRORPRONE_REPOSITORY, errorProneBugCheckerInfos);
		
		Map<String, List<Class<? extends BugChecker>>> pluginCheckers = checkerClassesByRepository();

		for (Map.Entry<String, List<Class<? extends BugChecker>>> entry : pluginCheckers.entrySet()) {
			String repository = entry.getKey();
			List<Class<? extends BugChecker>> checkers = entry.getValue();

			List<BugCheckerInfo> checkersInfos = checkers.stream().map(BugCheckerInfo::create).collect(Collectors.toList());

			processCheckers(repository, checkersInfos);
		}
	}

	public static Map<String, List<Class<? extends BugChecker>>> checkerClassesByRepository() {
		Iterator<BugChecker> checkersIterator = ErrorAwayRulesMapping.pluginCheckers();

		Map<String, List<Class<? extends BugChecker>>> pluginCheckers = new HashMap<>();
		while (checkersIterator.hasNext()) {
			BugChecker bugChecker = checkersIterator.next();
			String repository = ErrorAwayRulesMapping.repository(bugChecker.getClass());

			pluginCheckers.computeIfAbsent(repository, r -> new ArrayList<>()).add(bugChecker.getClass());
		}

		return pluginCheckers;
	}

	private void processCheckers(String repositoryName, Collection<BugCheckerInfo> bugCheckerInfos) throws MojoFailureException {
		File repositoryOutputDirectory = new File(outputDirectory, repositoryName);
		repositoryOutputDirectory.mkdirs();
		
		List<String> ruleKeys = new ArrayList<>();
		
		for (BugCheckerInfo bugCheckerInfo : bugCheckerInfos) {
			String ruleKey = asRuleKey(bugCheckerInfo);
			
			ruleKeys.add(ruleKey);
			
			generateRuleMetaData(repositoryOutputDirectory, bugCheckerInfo);
			generateRuleDescription(repositoryOutputDirectory, bugCheckerInfo);
		}
		
		generateRepositoryMetaData(repositoryName, repositoryOutputDirectory, ruleKeys);
	}

	public void generateRuleMetaData(File directory, BugCheckerInfo bugCheckerInfo) throws MojoFailureException {
		String ruleKey = asRuleKey(bugCheckerInfo);
		JSONObject rule = new JSONObject();
		
		rule.put("title", bugCheckerInfo.canonicalName());
		rule.put("defaultSeverity", getSeverity(bugCheckerInfo));
		rule.put("type", RuleType.CODE_SMELL);
		rule.put("status", RuleStatus.READY);
		rule.put("tags", bugCheckerInfo.getTags().stream().map(this::normalizeTag).collect(Collectors.toList()));
		
		File ruleFile = new File(directory, ruleKey + ".json");
		
		try (FileWriter writer = new FileWriter(ruleFile, StandardCharsets.UTF_8)) {
			rule.write(writer);
		} catch (IOException e) {
			throw new MojoFailureException("Error processing " + bugCheckerInfo, e);
		}
	}
	
	private void generateRuleDescription(File directory, BugCheckerInfo bugCheckerInfo) throws MojoFailureException {
		String ruleKey = asRuleKey(bugCheckerInfo);
		
		URL resource = findDescriptionResource(ruleKey);
		String html;
		
		if (resource != null) {
			try (InputStream in =  resource.openStream(); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				html = convertMdToHtml(reader);
			} catch (Exception e) {
				throw handleDescriptionReadException(ruleKey, e);
			}
		} else {
			try (StringReader reader = new StringReader(bugCheckerInfo.message())) {
				html = convertMdToHtml(reader);
				String link = getBugCheckerLink(bugCheckerInfo);

				html += "\n<b>See: </b><a href=\"" + link + "\" target=\"_blank\">" + link + "</a>";
			} catch (Exception e) {
				throw handleDescriptionReadException(ruleKey, e);
			}
		}
		
		File ruleFile = new File(directory, ruleKey + ".html");
		
		try (FileWriter writer = new FileWriter(ruleFile, StandardCharsets.UTF_8)) {
			writer.write(html);
		} catch (IOException e) {
			throw new MojoFailureException("Error processing " + bugCheckerInfo, e);
		}
	}
	
	public void generateRepositoryMetaData(String repositoryName, File repositoryOutputDirectory, List<String> ruleKeys)
			throws MojoFailureException {
		JSONObject repository = new JSONObject();
		repository.put("name", repositoryName);
		repository.put("rules", ruleKeys);
		
		File ruleFile = new File(repositoryOutputDirectory, "repository.json");
		
		try (FileWriter writer = new FileWriter(ruleFile, StandardCharsets.UTF_8)) {
			repository.write(writer);
		} catch (IOException e) {
			throw new MojoFailureException("Error processing repository " + repositoryName, e);
		}
	}

	public MojoFailureException handleDescriptionReadException(String ruleName, Exception e) {
		getLog().warn("Error parsing MD description for" + ruleName, e);
		
		return new MojoFailureException("Error parsing MD description for" + ruleName, e);
	}

	public String convertMdToHtml(Reader reader) throws IOException {
		Parser parser = Parser.builder().build();
		HtmlRenderer renderer = HtmlRenderer.builder().build();

		Node node = parser.parseReader(reader);

		return renderer.render(node);
	}

	private URL findDescriptionResource(String ruleName) {
		for (String folder : DESCRIPTION_FOLDERS) {
			URL url = findDescriptionResource(ruleName, folder);

			if (url != null) {
				return url;
			}
		}

		return null;
	}

	private URL findDescriptionResource(String ruleName, String directory) {
		if (directory == null) {
			return getClass().getResource("/errorprone/bugpattern/" + ruleName + ".md");
		} else {
			return getClass().getResource("/errorprone/bugpattern/" + directory + "/" + ruleName + ".md");
		}
	}
	
	public static String getSeverity(BugCheckerInfo bugCheckerInfo) {
		switch (bugCheckerInfo.defaultSeverity()) {
		case ERROR:
			return Severity.MAJOR;
		case WARNING:
			return Severity.MINOR;
		case SUGGESTION:
			return Severity.INFO;
		default:
			throw new IllegalArgumentException("Unexpected severity: " + bugCheckerInfo.defaultSeverity());
		}
	}
	

	public static String asRuleKey(BugCheckerInfo bugCheckerInfo) {
		return bugCheckerInfo.canonicalName();
	}

	public static String repository(BugCheckerInfo bugCheckerInfo) {
		return ErrorAwayRulesMapping.repository(bugCheckerInfo.checkerClass());
	}

	/**
	 * Rule tags accept only the characters: a-z, 0-9, '+', '-', '#', '.'
	 */
	private String normalizeTag(String tag) {
		return tag.toLowerCase();
	}

	/**
	 * Some plugins do not declare their link on the {@link BugPattern} annotation
	 * @param bugCheckerInfo The {@link BugCheckerInfo}
	 * @return The link for the give {@link BugCheckerInfo}
	 */
	private String getBugCheckerLink(BugCheckerInfo bugCheckerInfo) {
		switch (repository(bugCheckerInfo)) {
		case NULLAWAY_REPOSITORY:
			return "https://github.com/uber/NullAway/wiki/Error-Messages";
		default:
			return bugCheckerInfo.linkUrl();
		}
	}
}
