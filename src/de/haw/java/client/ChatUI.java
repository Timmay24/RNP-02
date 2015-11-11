package de.haw.java.client;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ChatUI {
	
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

	public ChatUI(String name) {
		this.name = name;
	}


	public void startUi() {
		this.dialog = new JFrame();
		contentPane = dialog.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		nickButton = new JButton("Nick");
		nickButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				final JDialog nickChangeDialog = new JDialog();
				Container nickChangeContainer = nickChangeDialog.getContentPane();
				nickChangeContainer.setLayout(new BoxLayout(nickChangeContainer, BoxLayout.LINE_AXIS));
				JLabel label = new JLabel("New Nickname?");
				nickChangeContainer.add(label);
				final JTextField inputArea = new JTextField();
				inputArea.setPreferredSize(new Dimension(100, 16));
				nickChangeContainer.add(inputArea);
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						String input = inputArea.getText();
						if (!"".equals(input)) {
							client.writeToServer("/rename " + input);
							setName(input);
							nickChangeDialog.setVisible(false);
							nickChangeDialog.dispose();
						}
					}
				});
				nickChangeContainer.add(okButton);
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						nickChangeDialog.setVisible(false);
						nickChangeDialog.dispose();
					}
				});
				nickChangeContainer.add(cancelButton);
				nickChangeDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				nickChangeDialog.setSize(600, 300);
				nickChangeDialog.setVisible(true);
			}
		});
		buttonPanel.add(nickButton);
		whoButton = new JButton("Who?");
		whoButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				client.writeToServer("/users");
			}
		});
		buttonPanel.add(whoButton);
		logoffButton = new JButton("Logoff");
		logoffButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				client.shutDown();
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		buttonPanel.add(logoffButton);
		clearButton = new JButton("Clear");
		clearButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				chatArea.setText("");
			}
		});
		buttonPanel.add(clearButton);
		contentPane.add(buttonPanel);
		chatArea = new JTextArea();
		JScrollPane scroll = new JScrollPane(chatArea, 
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
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				String input = inputArea.getText();
				if (input.length() > 1 && input.contains("\n")) {
					System.err.println(input.length());;
					inputArea.setText("");
					client.writeToServer(input);
					addChatMessageBySelf(input);
				}
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		contentPane.add(inputArea);
		dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		WindowListener listener = new WindowListener() {
			
			@Override
			public void windowOpened(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void windowIconified(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void windowDeiconified(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void windowDeactivated(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
			
			@Override
			public void windowClosed(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void windowActivated(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
		};
		dialog.addWindowListener(listener);
		dialog.setSize(900, 600);
		dialog.setVisible(true);
	}


	public Component getDialogPane() {
		return contentPane;
	}


	public String getName() {
		return name;
	}


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


	public void addInfoMessage(String message) {
		addChatMessage(message, "INFO");
	}


	public void setClient(TCPClient client) {
		this.client = client;
	}


	public void addChatMessage(String message) {
		chatArea.setText(chatArea.getText() + message + "\r\n");
	}
}
