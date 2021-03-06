package org.openhab.binding.teleinfo.reader.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Properties;

import org.openhab.binding.teleinfo.reader.context.conf.ConfigurationPluginHandler;
import org.openhab.binding.teleinfo.reader.context.conf.MissingConfigurationException;
import org.openhab.binding.teleinfo.reader.plugin.core.conf.Configuration;
import org.openhab.binding.teleinfo.reader.plugin.core.conf.ConfigurationDefinition;
import org.openhab.binding.teleinfo.reader.plugin.core.conf.InvalidConfigurationException;
import org.openhab.binding.teleinfo.reader.plugin.core.conf.InvalidParameterValueException;
import org.openhab.binding.teleinfo.reader.plugin.core.conf.ParameterDefinition;
import org.openhab.binding.teleinfo.reader.plugin.core.conf.defaultt.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PropertiesConfigurationPluginHandler implements ConfigurationPluginHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesConfigurationPluginHandler.class);

	private File configurationFolder;
	
	public PropertiesConfigurationPluginHandler(final File configurationFolder) {
		this.configurationFolder = configurationFolder;
	}
	
	@Override
	public Configuration getConfiguration(String pluginId, ConfigurationDefinition configurationDefinition) throws InvalidConfigurationException, MissingConfigurationException, IOException {
		LOGGER.debug("getConfiguration(String, ConfigurationDefinition) [start]");
		
		File confPropertiesFile = new File(configurationFolder, pluginId + ".cfg");
		if (! confPropertiesFile.exists()) {
			LOGGER.warn("No configuration file exists ("+confPropertiesFile.getName()+") for '" + pluginId + "' plugin in '" + configurationFolder.getAbsolutePath() + "' folder");
			// initialize automatically the parameters before throw a error
			if (confPropertiesFile.getParentFile() != null && !confPropertiesFile.getParentFile().exists()) {
				confPropertiesFile.getParentFile().mkdirs();
			}
			try {
				initializeEmptyConfigurationFile(confPropertiesFile, configurationDefinition, pluginId);
			} catch (Exception e) {
				LOGGER.warn("An error occurred during auto configuration file initialization", e);
			}
			
			throw new MissingConfigurationException(pluginId);
		}
		
		Properties parametersProperties = new Properties();
		try (InputStream in = new FileInputStream(confPropertiesFile)) {
			parametersProperties.load(in);
		} catch (Exception e) {
			throw new IOException("An error occurred during '" + pluginId + "' service's configuration loading");
		}
		
		Configuration configuration = new DefaultConfiguration(configurationDefinition);
		
		InvalidConfigurationException invalidConfigurationException = new InvalidConfigurationException();
		
		for (ParameterDefinition paramDef : configurationDefinition.getParametersDefinition()) {
			String value = parametersProperties.getProperty(paramDef.getId());
			if (value == null && paramDef.isRequired()) {
				invalidConfigurationException.addInvalidMessage("The required parameter '" + paramDef.getId() + "' is missing");
			} else if (value != null) {
				try {
					configuration.setParameterValue(paramDef.getId(), value);
				} catch (InvalidParameterValueException e) {
					invalidConfigurationException.addInvalidMessage("The parameter '" + paramDef.getId() + "' is invalid ("+e.getMessage()+")");
				}				
			}
		}
		
		if (invalidConfigurationException.hasInvalidMessages()) {
			throw invalidConfigurationException;
		}
		
		LOGGER.debug("getConfiguration(String, ConfigurationDefinition) [end]");
		return configuration;
	}
	
	private void initializeEmptyConfigurationFile(final File confFile, final ConfigurationDefinition configurationDefinition, String pluginId) throws IOException {
		LOGGER.debug("initializeEmptyConfigurationFile(File) [start]");
		
		try (PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(confFile), "ISO-8859-1")))) {
			out.println("Teleinfo-Reader - '" + pluginId + "' plugin configuration");
			out.println("---------------------------------------------------------------------");
			if (configurationDefinition.getDocumentationURL() != null) {
				out.println("More information about this plugin's configuration: " + configurationDefinition.getDocumentationURL());
			}
			out.println();
			
			for (ParameterDefinition paramDef : configurationDefinition.getParametersDefinition()) {
				out.println("# '" + paramDef.getId() + "' parameter - " + (paramDef.getDescription() == null ? "no description" : paramDef.getDescription().replaceAll("\n", "\n#")));
				out.println("# parameter mandatory: " + paramDef.isRequired() + " - parameter type: " + paramDef.getType().getName());
				out.println("# " + paramDef.getId() + " = <YOUR VALUE>");
				out.println();
			}
		} catch (Exception e) {
			throw new IOException("An error occurred during '" + pluginId + "' plugin's configuration initialization");
		}

		LOGGER.debug("initializeEmptyConfigurationFile(File) [end]");
	}
}
