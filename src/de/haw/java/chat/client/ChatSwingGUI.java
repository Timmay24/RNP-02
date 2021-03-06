package de.haw.java.chat.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatSwingGUI implements ChatUI {
	
	private TCPClient client;
	private String name;
	private JFrame dialog;
	private JButton nickButton;
	private JButton whoButton;
	private AbstractButton logoffButton;
	private JButton clearButton;
	private Container contentPane;
	private JTextArea chatArea;
	private JTextArea inputArea;

	public ChatSwingGUI(String name) {
		this.name = name;
	}


	@Override
	public void startUi() {

		// Erst mal die System-Optik aufzwingen
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		this.dialog = new JFrame();
		contentPane = dialog.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		nickButton = new JButton("Rename");
		nickButton.addActionListener(e -> {
            final JDialog nickChangeDialog = new JDialog(dialog, "Change Nickname", Dialog.ModalityType.DOCUMENT_MODAL);
            final Container nickChangeContainer = nickChangeDialog.getContentPane();
            nickChangeContainer.setLayout(new BoxLayout(nickChangeContainer, BoxLayout.LINE_AXIS));
            final JLabel label = new JLabel("New Nickname:");
            nickChangeContainer.add(label);
            final JTextField inputArea1 = new JTextField();
            inputArea1.setPreferredSize(new Dimension(100, 16));
            nickChangeContainer.add(inputArea1);
            final JButton okButton = new JButton("OK");
            okButton.addActionListener(e1 -> {
                final String input = inputArea1.getText();
                if (!"".equals(input)) {
                    client.writeToServer("/rename " + input);
                    setName(input);
                    nickChangeDialog.setVisible(false);
                    nickChangeDialog.dispose();
                }
            });
            nickChangeContainer.add(okButton);
            final JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e1 -> {
                nickChangeDialog.setVisible(false);
                nickChangeDialog.dispose();
            });
            nickChangeContainer.add(cancelButton);
            nickChangeDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            nickChangeDialog.setSize(600, 300);
            nickChangeDialog.setVisible(true);
        });
		buttonPanel.add(nickButton);
		whoButton = new JButton("Who?");
		whoButton.addActionListener(e -> client.writeToServer("/users"));
		buttonPanel.add(whoButton);
		logoffButton = new JButton("Logoff");
		logoffButton.addActionListener(e -> shutdown());
		buttonPanel.add(logoffButton);
		clearButton = new JButton("Clear");
		clearButton.addActionListener(e -> chatArea.setText(""));
		buttonPanel.add(clearButton);
		contentPane.add(buttonPanel);
		chatArea = new JTextArea();
		final JScrollPane scroll = new JScrollPane(chatArea, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setPreferredSize(new Dimension(300, 600));
		chatArea.setEditable(false);
		chatArea.setLineWrap(true);
		contentPane.add(scroll);
		inputArea = new JTextArea();
		inputArea.setPreferredSize(new Dimension(300, 20));
		inputArea.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				final String input = inputArea.getText();
				if (input.length() > 1 && input.contains("\n")) {
//					System.err.println(input.length());
					inputArea.setText("");
					// auto-format a non-command input as a /msg
					String outMessage = "";
					if (input.startsWith("/")) {
						outMessage = input;
					} else {
						outMessage = "/msg " + input;
					}
					client.writeToServer(outMessage);
					addChatMessageBySelf(input.replaceFirst("/msg ", ""));
				}
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
			}
		});
		contentPane.add(inputArea);
		dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		final WindowListener listener = new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
			}

			@Override
			public void windowIconified(WindowEvent e) {
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
			}

			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}

			@Override
			public void windowClosed(WindowEvent e) {
			}

			@Override
			public void windowActivated(WindowEvent e) {
			}
		};
		dialog.addWindowListener(listener);
		dialog.setSize(900, 600);
		dialog.setVisible(true);
	}

	private void shutdown() {
		client.shutDown();
		dialog.setVisible(false);
		dialog.dispose();
	}


	public String getName() {
		return name;
	}


	@Override
	public void setName(String name) {
		this.name = name;
		dialog.setTitle("Nickname: " + getName());
	}


	public void addChatMessage(String message, String nickname) {
		chatArea.setText(chatArea.getText() + nickname + ": " + message + "\r\n");
	}


	private void addChatMessageBySelf(String message) {
		chatArea.setText(chatArea.getText() + ">" +  getName() + ": " + message);
	}


	@Override
	public void addInfoMessage(String message) {
		addChatMessage(message, "INFO");
	}


	public void addErrorMessage(String message) {
		addChatMessage(message, "ERROR");
	}


	@Override
	public void setClient(TCPClient client) {
		this.client = client;
	}


	@Override
	public void addChatMessage(String message) {
		chatArea.setText(chatArea.getText() + message + "\r\n");
	}
}
