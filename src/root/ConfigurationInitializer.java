package root;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationInitializer {

	public static Properties getConfiguration(String configurationFileLocation) {

		// Initialize a new properties object to store configuration
		// This will be returned when `Configuration_Initializer()` is called
		Properties config = new Properties();

		// Set default configuration options to be used if `rem.properties` is not found
		// in the same directory where REM.jar is being executed

		// Note that no font family is specified, one will only be applied if the value
		// of `font_family` is not null

		// Colors adapted from http://www.eclipsecolorthemes.org/?view=theme&id=53551
		config.setProperty("editor_background_color", "#2D2A2E");
		config.setProperty("editor_cursor_color", "#D0D0D0");
		config.setProperty("text_size", "12");
		config.setProperty("text_color", "#FCFCFA");
		config.setProperty("text_header_color", "#78DCE8");
		config.setProperty("text_code_color", "#ffa500");
		config.setProperty("text_blockquote_color", "#A9DC76");
		config.setProperty("text_ordered_list_color", "#FFD866");
		config.setProperty("text_unordered_list_color", "#FFD866");
		config.setProperty("text_link_color", "#78A4E8");
		config.setProperty("text_image_color", "#ff78a3");

		try (InputStream configurationFile = new FileInputStream(configurationFileLocation)) {

			// This will run if a `rem.properties` file is found. This will overwrite the
			// default values defined above.

			config.load(configurationFile);

		} catch (Exception e) {

			// This will run if a `rem.properties` file is not found. In this case, no
			// action is necessary, as the default configuration values set above will pass
			// through and be returned.

			System.out.println("ERROR: Configuration file `" + configurationFileLocation
					+ "` not found. Defaulting to standard fallback configuration.");

		}

		return config;

	}

}
