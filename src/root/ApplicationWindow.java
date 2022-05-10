package root;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

public class ApplicationWindow {
	// Initializes UI elements to be used throughout the program
	JFrame frame = new JFrame();
	String windowTitlePrefix = "REM - ";
	JTextPane textPane = new JTextPane();
	JMenuBar menuBar = new JMenuBar();
	JMenu menu = new JMenu("File");
	JMenuItem menuOpen = new JMenuItem("Open");
	JMenuItem menuSave = new JMenuItem("Save");

	// Variables to keep track of file actions
	Boolean fileSaved = true;
	File workingFile = null;

	private void setWorkingFile(File newFile) {
		workingFile = newFile;
		frame.setTitle(windowTitlePrefix + workingFile.getName());
	}

	private void readFileToBuffer(File inputFile) {
		try {
			Scanner inputFileScanner = new Scanner(inputFile);
			StringBuilder fileText = new StringBuilder();
			while (inputFileScanner.hasNextLine()) {
				fileText.append(inputFileScanner.nextLine() + "\n");
			}
			textPane.setText(fileText.toString());
			inputFileScanner.close();
		} catch (FileNotFoundException e) {
			System.out.println("ERROR: Input file not found");
			e.printStackTrace();
		}
	}

	private void readBufferToFile(JTextPane bufferPane, File file) {
		try {
			FileWriter fw = new FileWriter(file);
			fw.write(bufferPane.getText());
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void clearBuffer(JTextPane bufferPane) {
		bufferPane.setText(null);
	}

	private Color parseColor(String index) {
		return Color.decode((String) config.get(index));
	}

	// Set `config` to the properties returned by the configuration loader
	Properties config = ConfigurationInitializer.getConfiguration("rem.properties");

	// This method adapted from https://stackoverflow.com/a/28773736
	private final class SyntaxHighlightingFilter extends DocumentFilter {

		private final StyledDocument styledDocument = (StyledDocument) textPane.getDocument();

		private final StyleContext styleContext = StyleContext.getDefaultStyleContext();

		private final AttributeSet text_color = styleContext.addAttribute(styleContext.getEmptySet(),
				StyleConstants.Foreground, parseColor("text_color"));
		private final AttributeSet text_header_color = styleContext.addAttribute(styleContext.getEmptySet(),
				StyleConstants.Foreground, parseColor("text_header_color"));
		private final AttributeSet text_blockquote_color = styleContext.addAttribute(styleContext.getEmptySet(),
				StyleConstants.Foreground, parseColor("text_blockquote_color"));
		private final AttributeSet text_ordered_list_color = styleContext.addAttribute(styleContext.getEmptySet(),
				StyleConstants.Foreground, parseColor("text_ordered_list_color"));
		private final AttributeSet text_unordered_list_color = styleContext.addAttribute(styleContext.getEmptySet(),
				StyleConstants.Foreground, parseColor("text_unordered_list_color"));
		private final AttributeSet text_code_color = styleContext.addAttribute(styleContext.getEmptySet(),
				StyleConstants.Foreground, parseColor("text_code_color"));
		private final AttributeSet text_link_color = styleContext.addAttribute(styleContext.getEmptySet(),
				StyleConstants.Foreground, parseColor("text_link_color"));
		private final AttributeSet text_image_color = styleContext.addAttribute(styleContext.getEmptySet(),
				StyleConstants.Foreground, parseColor("text_image_color"));

		@Override
		public void insertString(FilterBypass fb, int offset, String text, AttributeSet attributeSet)
				throws BadLocationException {
			super.insertString(fb, offset, text, attributeSet);

			handleTextChanged();
		}

		@Override
		public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
			super.remove(fb, offset, length);

			handleTextChanged();
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attributeSet)
				throws BadLocationException {
			super.replace(fb, offset, length, text, attributeSet);

			handleTextChanged();
		}

		// Runs your updates later, not during the event notification.
		private void handleTextChanged() {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					updateTextStyles();
				}
			});
		}

		// TODO:
		// - Bold text
		// - Italic text
		// - Inline LaTeX
		// - Block LaTeX

		Pattern headerPattern = Pattern.compile("^((\\#){1,6})\\ (.*)", Pattern.MULTILINE);
		Pattern blockquotePattern = Pattern.compile("^>(.*)", Pattern.MULTILINE);
		Pattern unorderedListPattern = Pattern.compile("(^([\\-\\*\\+]{1})(\\s)(.*)(?:$)?)+", Pattern.MULTILINE);
		Pattern orderedListPattern = Pattern.compile("(^(\\d+\\.)(\\s)(.*)(?:$)?)+", Pattern.MULTILINE);
		Pattern codePattern = Pattern.compile("(\\`{1})([^\\`]*)(\\`{1})", Pattern.MULTILINE);
		Pattern linkPattern = Pattern.compile(
				"(\\[((?:\\[[^\\]]*\\]|[^\\[\\]])*)\\]\\([ \\t]*()<?((?:\\([^)]*\\)|[^()\\s])*?)>?[ \\t]*((['\"])(.*?)\\6[ \\t]*)?\\))",
				Pattern.MULTILINE); // links, adapted from regexr.com/3ciio
		Pattern imagePattern = Pattern.compile("\\!\\[[^\\]]*\\]\\(([^\\)]*)\\)", Pattern.MULTILINE); // imaes, adapted
																										// from
																										// regexr.com/3bq1m

		private void updateTextStyles() {
			// Clear existing styles
			styledDocument.setCharacterAttributes(0, textPane.getText().length(), text_color, true);

			Matcher headerMatcher = headerPattern.matcher(textPane.getText());
			while (headerMatcher.find()) {
				styledDocument.setCharacterAttributes(headerMatcher.start(),
						headerMatcher.end() - headerMatcher.start(), text_header_color, false);
			}

			Matcher blockquoteMatcher = blockquotePattern.matcher(textPane.getText());
			while (blockquoteMatcher.find()) {
				styledDocument.setCharacterAttributes(blockquoteMatcher.start(),
						blockquoteMatcher.end() - blockquoteMatcher.start(), text_blockquote_color, false);
			}

			Matcher orderedListMatcher = orderedListPattern.matcher(textPane.getText());
			while (orderedListMatcher.find()) {
				styledDocument.setCharacterAttributes(orderedListMatcher.start(),
						orderedListMatcher.end() - orderedListMatcher.start(), text_ordered_list_color, false);
			}

			Matcher unorderedListMatcher = unorderedListPattern.matcher(textPane.getText());
			while (unorderedListMatcher.find()) {
				styledDocument.setCharacterAttributes(unorderedListMatcher.start(),
						unorderedListMatcher.end() - unorderedListMatcher.start(), text_unordered_list_color, false);
			}

			Matcher codeMatcher = codePattern.matcher(textPane.getText());
			while (codeMatcher.find()) {
				styledDocument.setCharacterAttributes(codeMatcher.start(), codeMatcher.end() - codeMatcher.start(),
						text_code_color, false);
			}

			Matcher linkMatcher = linkPattern.matcher(textPane.getText());
			while (linkMatcher.find()) {
				styledDocument.setCharacterAttributes(linkMatcher.start(), linkMatcher.end() - linkMatcher.start(),
						text_link_color, false);
			}

			Matcher imageMatcher = imagePattern.matcher(textPane.getText());
			while (imageMatcher.find()) {
				styledDocument.setCharacterAttributes(imageMatcher.start(), imageMatcher.end() - imageMatcher.start(),
						text_image_color, false);
			}
		}
	}

	// Create the interface elements and prepare the window for display
	private void initialize() {
		// Initialize frame layout
		frame.setSize(400, 500);
		frame.setTitle(windowTitlePrefix + "Scratchpad");
		frame.getContentPane().setLayout(new BorderLayout(0, 0));

		// Create a panel to hold UI items
		JPanel panel = new JPanel();
		panel.setBorder(null);

		// Add `panel` to the frame
		frame.getContentPane().add(panel, BorderLayout.CENTER);

		// Menu buttons
		menuOpen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (workingFile == null) {
					// Run if the buffer is in scratchpad mode
					if ("".equals(textPane.getText())) {
						// Run if there is no text in the buffer
						JFileChooser fc = new JFileChooser();
						int fcResponse = fc.showOpenDialog(frame);
						if (fcResponse == JFileChooser.APPROVE_OPTION) {
							File file = fc.getSelectedFile();
							setWorkingFile(file);
							readFileToBuffer(file);
						}
					} else {
						// Runs if there is text in the buffer
						int promptChoice = JOptionPane.showConfirmDialog(panel,
								"Scratchpad not saved. Discard and open new file?", "REM", JOptionPane.YES_NO_OPTION);
						// yes = 0, no = 1
						if (promptChoice == 0) {
							// Save scratchpad to file
							JFileChooser fc = new JFileChooser();
							fc.setDialogTitle("Create file to save scratchpad to...");
							int saveFileSelection = fc.showSaveDialog(frame);
							if (saveFileSelection == JFileChooser.APPROVE_OPTION) {
								readBufferToFile(textPane, fc.getSelectedFile());
							}
						} else {
							// do nothing
//							JFileChooser fc = new JFileChooser();
//							fc.setDialogTitle("Select a file to open...");
//							int saveFileSelection = fc.showOpenDialog(frame);
//							if (saveFileSelection == JFileChooser.APPROVE_OPTION) {
//								// User confirms they want to open a new file, so buffer can be destroyed
//								clearBuffer(textPane);
//								setWorkingFile(fc.getSelectedFile());
//								readFileToBuffer(workingFile);
//							}
						}
					}
				} else // Run if a working file exists
				if (fileSaved == true) {
					// If file was saved, the buffer can be safely destroyed and replaced
					clearBuffer(textPane);
					JFileChooser fc = new JFileChooser();
					fc.setDialogTitle("Select a file to open...");
					int saveFileSelection = fc.showOpenDialog(frame);
					if (saveFileSelection == JFileChooser.APPROVE_OPTION) {
						// User confirms they want to open a new file, so buffer can be destroyed
						clearBuffer(textPane);
						setWorkingFile(fc.getSelectedFile());
						readFileToBuffer(workingFile);
					}
				} else {
					// File has not been saved, and a working file exists
					int promptChoice = JOptionPane.showConfirmDialog(panel, "File has not been saved. Save changes?",
							"REM", JOptionPane.YES_NO_OPTION);
					// yes = 0, no = 1
					if (promptChoice == JOptionPane.YES_OPTION) {
						// Save working file
						readBufferToFile(textPane, workingFile);
						frame.setTitle(windowTitlePrefix + workingFile.getName());
						fileSaved = true;
					} else {
						// User does not want to save changes, open new file instead
						JFileChooser fc = new JFileChooser();
						fc.setDialogTitle("Select a file to open...");
						int saveFileSelection = fc.showOpenDialog(frame);
						if (saveFileSelection == JFileChooser.APPROVE_OPTION) {
							clearBuffer(textPane);
							setWorkingFile(fc.getSelectedFile());
							readFileToBuffer(workingFile);
							frame.setTitle(windowTitlePrefix + workingFile.getName());
						}
					}
				}

				// Old code, before file operation rewrite
//				if (!fileSaved) {
//					int promptChoice = JOptionPane.showConfirmDialog(panel,
//							"File not saved. Are you sure you want to open a new file and discard changes?", "REM",
//							JOptionPane.YES_NO_OPTION);
//					// yes = 0, no = 1
//					if (promptChoice == 0) {
//
//						// Execute if a dirty file is being edited, but the user chooses to open a new
//						// file without saving changes
//
//						fileSaved = true;
//
//						JFileChooser fileChooser = new JFileChooser();
//						fileChooser.setCurrentDirectory(new File("."));
//
//						if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
//							File chosenFile = new File(fileChooser.getSelectedFile().getAbsolutePath());
//							updateFile(chosenFile);
//							readFileToBuffer(chosenFile);
//						}
//
//					} else if (promptChoice == 1) {
//						// do nothing
//					}
//				} else {
//					fileSaved = true;
//
//					JFileChooser fileChooser = new JFileChooser();
//					fileChooser.setCurrentDirectory(new File("."));
//
//					if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
//						File chosenFile = new File(fileChooser.getSelectedFile().getAbsolutePath());
//						updateFile(chosenFile);
//						readFileToBuffer(chosenFile);
//					}
//
//				}
			}
		});

		menuSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (workingFile == null) {
					JFileChooser fc = new JFileChooser();
					fc.setDialogTitle("Create file to save scratchpad to...");
					int saveFileSelection = fc.showSaveDialog(frame);
					if (saveFileSelection == JFileChooser.APPROVE_OPTION) {
						setWorkingFile(fc.getSelectedFile());
						readBufferToFile(textPane, workingFile);
						fileSaved = true;
						frame.setTitle(windowTitlePrefix + workingFile.getName());
					}
				} else {
					readBufferToFile(textPane, workingFile);
					fileSaved = true;
					frame.setTitle(windowTitlePrefix + workingFile.getName());
				}
			}
		});

		menu.add(menuOpen);
		menu.add(menuSave);
		menuBar.add(menu);

		frame.setJMenuBar(menuBar);

		// Apply syntax highlighting to textPane
		((AbstractDocument) textPane.getDocument()).setDocumentFilter(new SyntaxHighlightingFilter());
		panel.setLayout(null);

		// Set textPane font to respect user selection
		if (config.get("font_family") == null) {
			textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Integer.valueOf((String) config.get("text_size"))));
		} else {
			textPane.setFont(new Font((String) config.get("font_family"), Font.PLAIN,
					Integer.valueOf((String) config.get("text_size"))));
		}

		// Set various colors to respect user preferences
		textPane.setBackground(parseColor("editor_background_color"));
		textPane.setForeground(null);
		textPane.setCaretColor(parseColor("editor_cursor_color"));

		// Handle dirty file title updating
		textPane.addKeyListener(new KeyListener() {
			// If a key is typed in a file, show a * to indicate it hasn't been saved
			@Override
			public void keyTyped(KeyEvent arg0) {
				fileSaved = false;
				if (workingFile == null) {
					frame.setTitle(windowTitlePrefix + "Scratchpad*");
				} else {
					frame.setTitle(windowTitlePrefix + workingFile.getName() + "*");
				}
			}

			// These two methods aren't necessary, but Java complains if they are removed

			@Override
			public void keyReleased(KeyEvent arg0) {
			}

			@Override
			public void keyPressed(KeyEvent arg0) {
			}

		});

		// This is approximately the height of the menu bar. It gets subtracted from the
		// bounds of the scrollPane to make it appear that the editor takes up all
		// available space.
		int titleHeight = 59;

		// Create a scrollPane to hold the textPane to create scrollbars as needed
		JScrollPane scrollPane = new JScrollPane(textPane);
		scrollPane.setBounds(0, 0, frame.getWidth(), frame.getHeight() - titleHeight);
		scrollPane.setBorder(null);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		panel.add(scrollPane);

		// Resize textPane when window is resized
		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				scrollPane.setBounds(0, 0, frame.getWidth(), frame.getHeight() - titleHeight);
			}
		});

		// Make sure program can't be quit with an unsaved buffer
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent ev) {
				if (fileSaved == true) {
					System.exit(0);
				} else {
					int promptChoice = JOptionPane.showConfirmDialog(panel,
							"File not saved. Are you sure you want to exit without saving changes?", "REM",
							JOptionPane.YES_NO_OPTION);
					// yes = 0, no = 1
					if (promptChoice == 0) {
						System.exit(1);
					} else {
						// do nothing
					}
				}
			}
		});

		frame.setVisible(true);
	}

	// Run when a new instance of Java_Editor_Window() is created
	public ApplicationWindow() {
		initialize();
	}

	// Launch the window
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					ApplicationWindow window = new ApplicationWindow();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
